package com.whatsappbot.whatsappservice.controller;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsappbot.whatsappservice.dto.PagoResponseDTO;
import com.whatsappbot.whatsappservice.dto.ProductoCarritoDTO;
import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.repository.PedidoRepository;
import com.whatsappbot.whatsappservice.service.PdfService;
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
    private final PedidoRepository pedidoRepository;
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

            // 🛒 Pedido confirmado desde carrito
            if ("order".equalsIgnoreCase(tipo)) {
                JsonNode atributos = watiService.obtenerAtributosContacto(telefono);
                JsonNode itemsNode = atributos.path("contact").path("customParams").path("last_cart_items");
                double total = atributos.path("contact").path("customParams").path("last_cart_total_value").asDouble(0.0);

                if (!itemsNode.isArray() || itemsNode.size() == 0) {
                    log.warn("⚠️ Pedido recibido sin productos");
                    return ResponseEntity.ok().build();
                }

                List<ProductoCarritoDTO> productos = Arrays.asList(
                    objectMapper.treeToValue(itemsNode, ProductoCarritoDTO[].class)
                );

                StringBuilder detalle = new StringBuilder();
                for (ProductoCarritoDTO p : productos) {
                    detalle.append(String.format("- %dx %s\n", p.getQuantity(), p.getName()));
                }

                String pedidoId = "pedido-" + UUID.randomUUID().toString().substring(0, 8);

                PedidoEntity pedido = new PedidoEntity();
                pedido.setPedidoId(pedidoId);
                pedido.setTelefono(telefono);
                pedido.setDetalle(detalle.toString().trim());
                pedido.setEstado("pendiente");
                pedidoRepository.save(pedido);

                log.info("🛒 Pedido guardado como pendiente: {}", pedidoId);

                // Link de pago
                int monto = (int) Math.round(total);
                PagoResponseDTO pago = transbankService.generarLinkDePago(pedidoId, monto);
                String linkPago = pago.getUrl();

                // Comanda
                byte[] pdf = pdfService.generarComandaPDF(pedidoId, productos, total);
                String urlComanda = s3Service.subirComanda(pedidoId, pdf);

                // Enviar mensajes
                watiService.enviarMensajeConTemplate(telefono, pedidoId, urlComanda);
                watiService.enviarMensajePagoEstatico(telefono, total, linkPago);
            }

            // 💬 Mensaje de texto tipo "ayuda"
            else if ("text".equalsIgnoreCase(tipo)) {
                String texto = payload.path("text").asText("").toLowerCase();
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
