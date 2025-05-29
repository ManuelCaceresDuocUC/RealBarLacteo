package com.whatsappbot.whatsappservice.controller;

import java.util.Arrays;
import java.util.List;

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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WatiService watiService;
    private final PedidoService pedidoService;
    private final TransbankService transbankService;
    private final PdfService pdfService;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/wati")
    public ResponseEntity<?> recibirMensaje(@RequestBody JsonNode payload) {
        log.info("📥 Payload recibido: {}", payload.toPrettyString());

        try {
            String tipo = payload.path("type").asText("");
            String telefono = payload.path("waId").asText("");
            String nombre = payload.path("senderName").asText("Cliente");

            if ("order".equalsIgnoreCase(tipo)) {
                // 🔄 Obtener carrito real desde WATI
                JsonNode atributos = watiService.obtenerAtributosContacto(telefono);
                JsonNode lastCartItems = atributos.path("contact").path("customParams").path("last_cart_items");
                double total = atributos.path("contact").path("customParams").path("last_cart_total_value").asDouble(0.0);

                if (!lastCartItems.isArray() || lastCartItems.size() == 0) {
                    log.warn("⚠️ Carrito vacío para el número {}", telefono);
                    return ResponseEntity.ok().build();
                }

                List<ProductoCarritoDTO> productos = Arrays.asList(
                    objectMapper.treeToValue(lastCartItems, ProductoCarritoDTO[].class)
                );

                // 🛒 Crear pedido y comanda
                String pedidoId = pedidoService.crearPedidoConDetalle(telefono, productos, total);
                byte[] pdf = pdfService.generarComandaPDF(pedidoId, productos, total);
                String urlComanda = s3Service.subirComanda(pedidoId, pdf);

                // 💳 Generar link de pago
                int monto = (int) Math.round(total);
                PagoResponseDTO pago = transbankService.generarLinkDePago(pedidoId, monto);
                String linkPago = pago.getUrl();

                // 📤 Enviar mensajes
                watiService.enviarMensajeConTemplate(telefono, pedidoId, urlComanda);
                watiService.enviarMensajePagoEstatico(telefono, total, linkPago);
            }

            else if ("text".equalsIgnoreCase(tipo)) {
                String texto = payload.path("text").asText("").toLowerCase();
                log.info("📥 Mensaje de texto: {} desde {}", texto, telefono);

                if (texto.contains("ayuda")) {
                    watiService.enviarTemplateAyuda(telefono, nombre);
                }
            }

        } catch (Exception e) {
            log.error("❌ Error procesando webhook", e);
        }

        return ResponseEntity.ok().build();
    }
}
