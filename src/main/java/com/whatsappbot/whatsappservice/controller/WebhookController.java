package com.whatsappbot.whatsappservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsappbot.whatsappservice.service.TransbankService;
import com.whatsappbot.whatsappservice.service.WatiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final TransbankService transbankService;
    private final WatiService watiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/wati")
    public ResponseEntity<?> recibirMensaje(@RequestBody JsonNode payload) {
        log.info("📥 Payload recibido: {}", payload.toPrettyString());

        try {
            JsonNode messages = payload.at("/data/messages");

            // 🔄 Formato antiguo (WATI clásico)
            if (!messages.isMissingNode() && messages.isArray()) {
                for (JsonNode msg : messages) {
                    String numero = msg.get("from").asText();
                    String texto = msg.at("/text/body").asText("").toLowerCase();

                    log.info("✉️ Mensaje recibido de {}: {}", numero, texto);

                    if (texto.contains("ayuda") || texto.contains("menu")) {
                        watiService.enviarTemplateAyuda(numero, "Cliente");
                    }
                }
            }

            // 🆕 Formato moderno (Live Chat o Broadcast)
            else if (payload.has("eventType") && "message".equals(payload.get("eventType").asText())) {
                String numero = payload.path("waId").asText("");
                String texto = payload.path("text").asText("").toLowerCase();
                String nombre = payload.path("senderName").asText("Cliente");

                log.info("✉️ Mensaje recibido de {}: {}", numero, texto);

                if (texto.contains("ayuda") || texto.contains("menu")) {
                    watiService.enviarTemplateAyuda(numero, nombre);
                }
            }

            else {
                log.warn("⚠️ No se encontraron mensajes válidos en el payload");
            }

        } catch (Exception e) {
            log.error("❌ Error procesando webhook de mensaje", e);
        }

        return ResponseEntity.ok().build();
    }
}