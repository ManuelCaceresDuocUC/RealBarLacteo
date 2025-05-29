package com.whatsappbot.whatsappservice.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsappbot.whatsappservice.dto.PagoResponseDTO;
import com.whatsappbot.whatsappservice.dto.ProductoCarritoDTO;
import com.whatsappbot.whatsappservice.service.PdfService;
import com.whatsappbot.whatsappservice.service.PedidoService;
import com.whatsappbot.whatsappservice.service.S3Service;
import com.whatsappbot.whatsappservice.service.TransbankService;
import com.whatsappbot.whatsappservice.service.WatiService;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    private final ObjectMapper mapper = new ObjectMapper();
    private final PedidoService pedidoService;
    private final WatiService watiService;
    private final TransbankService transbankService;
    private final PdfService pdfService;
    private final S3Service s3Service;

    @Value("${wati.api.url}")
    private String watiApiUrl;

    @Value("${wati.api.key}")
    private String apiKey;

    public WebhookController(PedidoService pedidoService, WatiService watiService, TransbankService transbankService,
                             PdfService pdfService, S3Service s3Service) {
        this.pedidoService = pedidoService;
        this.watiService = watiService;
        this.transbankService = transbankService;
        this.pdfService = pdfService;
        this.s3Service = s3Service;
    }

    @PostMapping
public ResponseEntity<?> recibirWebhook(@RequestBody JsonNode payload) {
    try {
        System.out.println("📥 Payload recibido: " + payload.toPrettyString());

        String tipo = payload.path("type").asText();
        if (!"order".equals(tipo)) return ResponseEntity.ok().build();

        String telefono = payload.path("waId").asText();
        String referenceId = payload.path("order").path("referenceId").asText();
        if (referenceId == null || referenceId.isEmpty()) {
            System.err.println("❌ referenceId no presente en el payload");
            return ResponseEntity.ok().build();
        }

        // (OPCIONAL) Intentar obtener atributos de contacto (solo si sirve en el futuro)
        try {
            watiService.obtenerAtributosContacto(telefono);
        } catch (Exception ex) {
            System.err.println("⚠️ No se pudo obtener atributos del contacto WATI, pero se continúa con el flujo.");
        }

        // Consultar WATI para obtener detalle del pedido
        OkHttpClient client = new OkHttpClient();
        String url = watiApiUrl + "/api/v1/order_details/" + referenceId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();
        JsonNode detalle = mapper.readTree(responseBody);

        JsonNode productosJson = detalle.path("orderDetails");
        double total = detalle.path("total").asDouble();
System.out.println("💰 Total del pedido extraído: " + total);

if (total <= 0) {
    throw new RuntimeException("El monto total es inválido: " + total);
}
int monto = (int) Math.round(total);
        List<ProductoCarritoDTO> productos = new ArrayList<>();
        for (JsonNode productoJson : productosJson) {
            ProductoCarritoDTO producto = new ProductoCarritoDTO();
            producto.setName(productoJson.path("name").asText());
            producto.setPrice(productoJson.path("price").asDouble());
            producto.setQuantity(productoJson.path("quantity").asInt());
            productos.add(producto);
        }

        // Guardar pedido y generar comanda
        String pedidoId = pedidoService.crearPedidoConDetalle(telefono, productos, total);
        byte[] pdf = pdfService.generarComandaPDF(pedidoId, productos, total);
        String urlComanda = s3Service.subirComanda(pedidoId, pdf);

        // Enviar confirmación + link de pago
       watiService.enviarMensajeConTemplate(telefono, pedidoId, urlComanda);
PagoResponseDTO pago = transbankService.generarLinkDePago(pedidoId, monto);
watiService.enviarMensajePagoEstatico(telefono, total, pago.getUrl());

        return ResponseEntity.ok().build();

    } catch (Exception e) {
        System.err.println("❌ Error procesando webhook: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(500).body("Error procesando webhook");
    }
}

}
