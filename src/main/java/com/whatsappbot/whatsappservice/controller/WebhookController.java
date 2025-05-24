package com.whatsappbot.whatsappservice.controller;

import java.util.UUID;

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
        log.debug("📥 Payload recibido: {}", payload.toString());
        try {
            JsonNode messages = payload.at("/data/messages");
            if (!messages.isMissingNode() && messages.isArray()) {
                for (JsonNode msg : messages) {
                    String numero = msg.get("from").asText();
                    String texto = msg.at("/text/body").asText("").toLowerCase();

                    if (numero == null || numero.isEmpty()) {
                        log.warn("⚠️ Número de WhatsApp no detectado");
                        continue;
                    }

                    if (texto.contains("menu") || texto.contains("ayuda")) {
                        String respuesta = "Hola! Puedes ver nuestro menú en: https://barlacteo.cl/menu. \nTambién puedes hacer tu pedido desde el catálogo de WhatsApp.";
                        watiService.enviarMensajeTexto(numero, respuesta);
                        log.info("📨 Mensaje de ayuda enviado a {}", numero);
                    }

                    if (texto.contains("pedido confirmado")) {
                        String pedidoId = "pedido-" + UUID.randomUUID().toString().substring(0, 8);
                        String linkPago = transbankService.generarLinkDePago(pedidoId, 1000);

                        watiService.enviarMensajeConTemplate(numero, pedidoId, linkPago);
                        log.info("✅ Pedido detectado desde carrito y procesado: {}", pedidoId);
                    }
                }
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ Error procesando webhook de mensaje", e);
            return ResponseEntity.status(500).body("Error procesando el webhook");
        }
    }
}
