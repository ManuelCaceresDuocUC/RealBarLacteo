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
            System.out.println("📬 Payload recibido: " + payload.toPrettyString());

            String tipo = payload.path("type").asText();
            if (!"order".equals(tipo)) return ResponseEntity.ok().build();

            String telefono = payload.path("waId").asText();

            // Recuperar atributos del contacto desde WATI
            JsonNode atributos = watiService.obtenerAtributosContacto(telefono);
            JsonNode lastCart = atributos.path("customParams").path("last_cart_items");
            double total = atributos.path("customParams").path("last_cart_total_value").asDouble();

            if (lastCart == null || lastCart.isEmpty()) {
                System.err.println("⚠️ Pedido recibido sin productos");
                return ResponseEntity.ok().build();
            }

            List<ProductoCarritoDTO> productos = new ArrayList<>();
            for (JsonNode productoJson : lastCart) {
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
            int monto = (int) Math.round(total);
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
