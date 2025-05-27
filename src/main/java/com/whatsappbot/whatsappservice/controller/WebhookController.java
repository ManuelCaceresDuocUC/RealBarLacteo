package com.whatsappbot.whatsappservice.controller;

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
                int montoFijo = 1000; // puedes ajustar el monto o dejarlo como placeholder
                String pedidoId = "pedido-" + UUID.randomUUID().toString().substring(0, 8);
                String detalle = "Pedido desde catálogo";

                // Guardar pedido como pendiente
                PedidoEntity pedido = new PedidoEntity();
                pedido.setPedidoId(pedidoId);
                pedido.setTelefono(telefono);
                pedido.setDetalle(detalle);
                pedido.setEstado("pendiente");
                pedidoRepository.save(pedido);

                log.info("🛒 Pedido guardado como pendiente: {}", pedidoId);

                // Generar link de pago real con Transbank
                PagoResponseDTO pago = transbankService.generarLinkDePago(pedidoId, montoFijo);
                String linkPago = pago.getUrl();

                // Enviar plantilla por WhatsApp con el link generado
                watiService.enviarMensajePagoEstatico(telefono, (double) montoFijo, linkPago);
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
