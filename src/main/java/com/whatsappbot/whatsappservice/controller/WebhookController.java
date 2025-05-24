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
        try {
            log.info("\uD83D\uDCE5 Payload recibido: {}", payload.toString());

            JsonNode messages = payload.at("/data/messages");
            if (!messages.isMissingNode() && messages.isArray()) {
                for (JsonNode msg : messages) {
                    String numero = msg.get("from").asText();
                    String texto = msg.at("/text/body").asText("").toLowerCase();

                    log.info("\u2709\uFE0F Mensaje recibido de {}: {}", numero, texto);

                    if (texto.contains("menu") || texto.contains("ayuda")) {
                        String respuesta = "Hola! Puedes ver nuestro men\u00fa en: https://barlacteo.cl/menu. \nTambi\u00e9n puedes hacer tu pedido desde el cat\u00e1logo de WhatsApp";
                        watiService.enviarMensajeTexto(numero, respuesta);
                        log.info("\uD83D\uDCAC Mensaje de ayuda enviado a {}", numero);
                    }

                    if (texto.contains("pedido confirmado")) {
                        String pedidoId = "pedido-" + UUID.randomUUID().toString().substring(0, 8);
                        String linkPago = transbankService.generarLinkDePago(pedidoId, 1000);

                        watiService.enviarMensajeConTemplate(numero, pedidoId, linkPago);
                        log.info("\u2705 Pedido detectado desde carrito y procesado: {}", pedidoId);
                    }
                                    if (texto.contains("ayuda") || texto.contains("menu")) {
                    String nombreCliente = "Cliente"; // Puedes obtener el nombre desde una base de datos o dejarlo genérico
                    try {
                        watiService.enviarTemplateAyuda(numero, nombreCliente);
                        log.info("✅ Plantilla de ayuda enviada a {}", numero);
                    } catch (Exception e) {
                        log.error("❌ Error al enviar plantilla de ayuda", e);
                    }
                }
                }
            } else {
                log.warn("\u26A0\uFE0F No se encontraron mensajes en el payload");
            }
        } catch (Exception e) {
            log.error("\u274C Error procesando webhook de mensaje", e);
        }
        return ResponseEntity.ok().build();
    }
}
