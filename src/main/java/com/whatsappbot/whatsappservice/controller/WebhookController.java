package com.whatsappbot.whatsappservice.controller;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.whatsappbot.whatsappservice.dto.PagoResponseDTO;
import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.repository.PedidoRepository;
import com.whatsappbot.whatsappservice.service.ComandaService;
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
    private final PedidoRepository pedidoRepository;
    private final TransbankService transbankService;
    private final ComandaService comandaService;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/wati")
    public ResponseEntity<?> recibirMensaje(@RequestBody JsonNode payload) {
        log.info("📥 Payload recibido: {}", payload.toPrettyString());

        try {
            String tipo = payload.path("type").asText("");
            String telefono = payload.path("waId").asText("");
            String nombre = payload.path("senderName").asText("Cliente");
            String texto = payload.path("text").asText("").toLowerCase();

            // ✅ Detecta plantilla que contiene detalle y total del carrito
            if ("text".equalsIgnoreCase(tipo) && texto.contains("total: $")) {
                log.info("🧾 Detectado mensaje de plantilla con resumen de pedido");

                // Revisa si ya hay un pedido pendiente
                PedidoEntity pedidoExistente = pedidoRepository.findFirstByTelefonoAndEstado(telefono, "pendiente");
                if (pedidoExistente != null && pedidoExistente.getLinkPago() != null) {
    log.info("🔁 Ya existe un pedido pendiente con link, reenviando...");
    watiService.enviarMensajePagoEstatico(telefono, pedidoExistente.getMonto(), pedidoExistente.getLinkPago());
    return ResponseEntity.ok().build();
}

                // Extrae monto del texto con expresión regular
                Matcher matcher = Pattern.compile("total: \\$(\\d+)", Pattern.CASE_INSENSITIVE).matcher(texto);
                if (!matcher.find()) {
                    log.warn("❌ No se pudo extraer el total del mensaje");
                    return ResponseEntity.ok().build();
                }

                int monto = Integer.parseInt(matcher.group(1));
                String pedidoId = "PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                // Guarda el pedido
                PedidoEntity pedido = new PedidoEntity();
                pedido.setPedidoId(pedidoId);
                pedido.setTelefono(telefono);
                pedido.setDetalle(payload.path("text").asText());
                pedido.setEstado("pendiente");
                pedido.setMonto((double) monto);
                pedidoRepository.save(pedido);

                // Genera link de pago
                PagoResponseDTO pago = transbankService.generarLinkDePago(pedidoId, monto);
                pedido.setLinkPago(pago.getUrl());
                pedidoRepository.save(pedido);

                // Genera PDF y lo sube
                String urlComanda = comandaService.generarPDF(pedido);
                if (urlComanda != null) {
                    log.info("📤 Comanda generada y subida correctamente: {}", urlComanda);
                }

                // Envía el link de pago por WhatsApp
                watiService.enviarMensajePagoEstatico(telefono, (double) monto, pago.getUrl());
            }

            // Mensaje de texto "ayuda"
            else if ("text".equalsIgnoreCase(tipo) && texto.contains("ayuda")) {
                watiService.enviarTemplateAyuda(telefono, nombre);
            }

        } catch (Exception e) {
            log.error("❌ Error procesando webhook", e);
        }

        return ResponseEntity.ok().build();
    }
}
