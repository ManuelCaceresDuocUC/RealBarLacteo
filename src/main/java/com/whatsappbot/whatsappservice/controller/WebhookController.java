package com.whatsappbot.whatsappservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsappbot.whatsappservice.service.WatiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WatiService watiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/wati")
    public ResponseEntity<?> recibirMensaje(@RequestBody JsonNode payload) {
        log.info("📥 Payload recibido: {}", payload.toPrettyString());

        try {
            // Extraer campos comunes del payload
            String tipo = payload.path("type").asText("");
            String telefono = payload.path("waId").asText("");
            String nombre = payload.path("senderName").asText("Cliente");

            // 🛒 Detectar pedido desde el carrito de WhatsApp
            if ("order".equalsIgnoreCase(tipo)) {
                JsonNode order = payload.path("order");
                double total = order.path("total").asDouble(0);

                log.info("🛒 Pedido recibido desde el catálogo → Teléfono: {}, Total: {}", telefono, total);

                // Enviar plantilla de pago estático
                watiService.enviarMensajePagoEstatico(telefono, total);
            }

            // 💬 Detectar mensajes de texto como "ayuda"
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
