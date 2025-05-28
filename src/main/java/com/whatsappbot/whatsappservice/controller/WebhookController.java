package com.whatsappbot.whatsappservice.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsappbot.whatsappservice.dto.PagoResponseDTO;
import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.repository.PedidoRepository;
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
                String pedidoId = "pedido-" + UUID.randomUUID().toString().substring(0, 8);
                JsonNode orderNode = payload.path("order");

                Map<String, Object> resultado = construirDetalleYTotalDesdeCatalogo(orderNode);
                String detalle = (String) resultado.get("detalle");
                double montoTotal = (double) resultado.get("total");

                // Validación opcional
                if (montoTotal <= 0) {
                    log.warn("❌ Monto inválido para generar transacción: {}", montoTotal);
                    return ResponseEntity.badRequest().body("Monto inválido");
                }

                // Guardar pedido como pendiente
                PedidoEntity pedido = new PedidoEntity();
                pedido.setPedidoId(pedidoId);
                pedido.setTelefono(telefono);
                pedido.setDetalle(detalle);
                pedido.setEstado("pendiente");
                pedidoRepository.save(pedido);

                log.info("🛒 Pedido guardado como pendiente: {}", pedidoId);

                // Generar link de pago
                int monto = (int) montoTotal;
                PagoResponseDTO pago = transbankService.generarLinkDePago(pedidoId, monto);
                String linkPago = pago.getUrl();

                // Enviar plantilla con el link generado
                watiService.enviarMensajePagoEstatico(telefono, montoTotal, linkPago);
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

    private Map<String, Object> construirDetalleYTotalDesdeCatalogo(JsonNode orderNode) {
        StringBuilder detalle = new StringBuilder();
        double total = 0.0;

        if (orderNode != null && orderNode.has("products")) {
            for (JsonNode producto : orderNode.get("products")) {
                String nombre = producto.path("name").asText("Producto desconocido");
                int cantidad = producto.path("quantity").asInt(1);
                double precio = producto.path("price").asDouble(1000); // fallback

                total += cantidad * precio;
                detalle.append("- ").append(cantidad).append(" x ").append(nombre).append("\n");
            }
        } else {
            detalle.append("Sin productos listados.");
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("detalle", detalle.toString().trim());
        resultado.put("total", total);
        return resultado;
    }
}
