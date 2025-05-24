package com.whatsappbot.whatsappservice.controller;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.repository.PedidoRepository;
import com.whatsappbot.whatsappservice.service.ComandaService;
import com.whatsappbot.whatsappservice.service.TransbankService;
import com.whatsappbot.whatsappservice.service.WatiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoControlador {

    private final PedidoRepository pedidoRepository;
    private final TransbankService transbankService;
    private final WatiService watiService;
    private final ComandaService comandaService;

    @PostMapping
    public ResponseEntity<?> crearPedido(@RequestBody Map<String, String> payload) {
        String telefono = payload.get("telefono");
        String detalle = payload.get("detalle");
        String pedidoId = "pedido-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("📝 Recibido nuevo pedido: telefono={}, detalle={}", telefono, detalle);

        try {
            PedidoEntity pedido = new PedidoEntity(pedidoId, telefono, detalle);
            pedidoRepository.save(pedido);

            String link = transbankService.generarLinkDePago(pedidoId, 1000); // Monto fijo por ahora
            watiService.enviarMensajeConTemplate(telefono, pedidoId, link);

            return ResponseEntity.ok(Map.of(
                "mensaje", "Pedido creado y link enviado por WhatsApp",
                "pedidoId", pedidoId,
                "linkPago", link
            ));
        } catch (Exception e) {
            log.error("❌ Error al crear pedido", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "No se pudo procesar el pedido"
            ));
        }
    }

    @PostMapping("/confirmacion")
    public ResponseEntity<String> confirmarPago(@RequestParam("token_ws") String token) {
        OkHttpClient client = new OkHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        Request request = new Request.Builder()
                .url("https://webpay3g.transbank.cl/rswebpaytransaction/api/webpay/v1.3/transactions/" + token)
                .addHeader("Tbk-Api-Key-Id", System.getenv("TRANSBANK_COMMERCE_CODE"))
                .addHeader("Tbk-Api-Key-Secret", System.getenv("TRANSBANK_API_KEY"))
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return ResponseEntity.status(400).body("Error al confirmar pago");

            JsonNode json = mapper.readTree(response.body().string());
            String buyOrder = json.get("buy_order").asText();

            Optional<PedidoEntity> pedidoOpt = pedidoRepository.findByPedidoId(buyOrder);
            if (pedidoOpt.isPresent()) {
                PedidoEntity pedido = pedidoOpt.get();
                pedido.setEstado("pagado");
                pedidoRepository.save(pedido);

                comandaService.generarPDF(pedido);

                String mensaje = "📥 *NUEVO PEDIDO PAGADO*\n"
                        + "🆔 ID: " + pedido.getPedidoId() + "\n"
                        + "📞 Teléfono: " + pedido.getTelefono() + "\n"
                        + "📦 Detalle: " + pedido.getDetalle();

                watiService.enviarMensajeTexto(pedido.getTelefono(), mensaje);

                return ResponseEntity.ok("Pago confirmado, comanda generada y aviso enviado.");
            } else {
                return ResponseEntity.status(404).body("Pedido no encontrado");
            }
        } catch (Exception e) {
            log.error("❌ Error interno al confirmar pago", e);
            return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
        }
    }
}
