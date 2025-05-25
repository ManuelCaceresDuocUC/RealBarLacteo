package com.whatsappbot.whatsappservice.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.repository.PedidoRepository;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/wati")
    public ResponseEntity<?> recibirMensaje(@RequestBody JsonNode payload) {
        log.info("📥 Payload recibido: {}", payload.toPrettyString());

        try {
            String tipo = payload.path("type").asText("");
            String telefono = payload.path("waId").asText("");
            String nombre = payload.path("senderName").asText("Cliente");

            // 🛒 Pedido desde el catálogo
            if ("order".equalsIgnoreCase(tipo)) {
                JsonNode order = payload.path("order");
                double total = order.path("total").asDouble(0);
                String pedidoId = "pedido-" + UUID.randomUUID().toString().substring(0, 8);
                String detalle = "Pedido desde catálogo";

                // Guardar pedido en la base de datos como "pendiente"
                PedidoEntity pedido = new PedidoEntity();
                pedido.setPedidoId(pedidoId);
                pedido.setTelefono(telefono);
                pedido.setDetalle(detalle);
                pedido.setEstado("pendiente");
                pedidoRepository.save(pedido);

                log.info("🛒 Pedido guardado como pendiente: {} → Total: {}", pedidoId, total);

                // Generar link de pago personalizado (ejemplo con dominio ficticio)
String linkPago = "https://barlacteo-catalogo.s3.us-east-1.amazonaws.com/pagar_modificado.html?pedidoId=" + pedidoId;

                // Enviar plantilla de pago estático con el total y link
                watiService.enviarMensajePagoEstatico(telefono, total, linkPago);
            }

            // 💬 Mensaje de texto: ayuda
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