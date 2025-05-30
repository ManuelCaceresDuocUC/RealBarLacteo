package com.whatsappbot.whatsappservice.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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

            // ✅ Detectar mensaje de plantilla con resumen del carrito
            if ("text".equalsIgnoreCase(tipo) && texto.contains("total estimado:")) {
                log.info("🧾 Detectado mensaje de resumen del carrito");

                // 🕒 Verifica si ya hay pedido pendiente en los últimos 5 minutos
                LocalDateTime haceCincoMin = LocalDateTime.now().minusMinutes(5);
                List<PedidoEntity> pedidos = pedidoRepository.findByTelefonoAndEstadoAndFechaCreacionAfter(
                    telefono, "pendiente", haceCincoMin
                );

                if (!pedidos.isEmpty()) {
                    log.warn("⚠️ Ya existe un pedido reciente y pendiente para este número: {}", telefono);
                    return ResponseEntity.ok().build();
                }

                // 🧾 Extrae detalle del texto
                String detalle = extraerDetalle(texto);
                int monto = extraerMonto(texto);

                if (detalle == null || monto == -1) {
                    log.warn("❌ No se pudo extraer el detalle o monto desde el mensaje");
                    return ResponseEntity.ok().build();
                }

                // 📦 Crear nuevo pedido
                String pedidoId = "pedido-" + UUID.randomUUID().toString().substring(0, 8);
                PedidoEntity pedido = new PedidoEntity();
                pedido.setPedidoId(pedidoId);
                pedido.setTelefono(telefono);
                pedido.setDetalle(detalle);
                pedido.setEstado("pendiente");
                pedido.setMonto((double) monto);
                pedido.setFechaCreacion(LocalDateTime.now(ZoneId.of("America/Santiago")));

                pedidoRepository.save(pedido);
                log.info("📝 Pedido guardado: {}", pedidoId);

                // 💳 Generar link de pago
                PagoResponseDTO pago = transbankService.generarLinkDePago(pedidoId, monto);
                pedido.setLinkPago(pago.getUrl());
                pedidoRepository.save(pedido);

                // 🧾 Generar PDF comanda (guardado en S3)
                String urlComanda = comandaService.generarPDF(pedido);
                log.info("📄 Comanda PDF generada: {}", urlComanda);

                // 🚀 Enviar mensaje con link de pago
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

    // 🔍 Extraer el texto entre "desde el carrito:" y el total
    private String extraerDetalle(String texto) {
        try {
            int inicio = texto.indexOf("desde el carrito:") + "desde el carrito:".length();
            int fin = texto.indexOf("💰 total estimado");
            if (inicio == -1 || fin == -1 || fin <= inicio) return null;

            return texto.substring(inicio, fin).trim();
        } catch (Exception e) {
            return null;
        }
    }

    // 🔍 Extraer monto desde el JSON dentro del texto
    private int extraerMonto(String texto) {
        try {
            Matcher matcher = Pattern.compile("\"total\":(\\d+\\.?\\d*)", Pattern.CASE_INSENSITIVE).matcher(texto);
            if (matcher.find()) {
                return (int) Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {
            log.error("❌ Error extrayendo el monto", e);
        }
        return -1;
    }
}
