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


    // ✅ Método para crear pedido y enviar link de pago por WhatsApp
    @PostMapping
    public ResponseEntity<?> crearPedido(@RequestBody Map<String, String> payload) {
        String telefono = payload.get("telefono");
        String detalle = payload.get("detalle");
        String pedidoId = "pedido-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("📝 Recibido nuevo pedido: telefono={}, detalle={}", telefono, detalle);

        try {
            PedidoEntity pedido = new PedidoEntity(pedidoId, telefono, detalle);
            pedidoRepository.save(pedido);
            log.info("✅ Pedido guardado con ID: {}", pedidoId);

            // 1. Crear transacción en Transbank
            String link = "";
            try {
                link = transbankService.generarLinkDePago(pedidoId, 1000); // puedes cambiar el monto
                log.info("✅ Link de pago generado: {}", link);
            } catch (Exception e) {
                log.error("❌ Error al generar link de Transbank", e);
                return ResponseEntity.status(500).body(Map.of(
                    "detalle", "Error al crear transacción",
                    "error", "Fallo al generar el link de pago"
                ));
            }

            // 2. Enviar mensaje por WhatsApp
            try {
                String mensaje = "🍨 Pedido recibido: " + detalle +
                                 "\n👉 Paga aquí: " + link;
                watiService.enviarMensaje(telefono, mensaje);
                log.info("✅ Mensaje enviado por WhatsApp a {}", telefono);
            } catch (Exception e) {
                log.error("❌ Error al enviar mensaje por WhatsApp", e);
                return ResponseEntity.status(500).body(Map.of(
                    "detalle", "Error al enviar mensaje",
                    "error", "Fallo en WhatsApp"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "mensaje", "Pedido creado y link enviado por WhatsApp",
                "pedidoId", pedidoId,
                "linkPago", link
            ));

        } catch (Exception e) {
            log.error("❌ Error inesperado al procesar pedido", e);
            return ResponseEntity.status(500).body(Map.of(
                "detalle", "Error interno",
                "error", "Fallo inesperado"
            ));
        }
    }


    // ✅ Webhook para confirmar el pago desde Transbank
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
        String buyOrder = json.get("buy_order").asText(); // ej: pedido-abc123

        Optional<PedidoEntity> pedidoOpt = pedidoRepository.findByPedidoId(buyOrder);
        if (pedidoOpt.isPresent()) {
            PedidoEntity pedido = pedidoOpt.get();
            pedido.setEstado("pagado");
            pedidoRepository.save(pedido);

            // ✅ Generar comanda en PDF localmente
            comandaService.generarPDF(pedido);

            // ✅ Enviar aviso por WhatsApp al local
            String aviso = "📥 *NUEVO PEDIDO PAGADO*\n"
                         + "🆔 ID: " + pedido.getPedidoId() + "\n"
                         + "📞 Teléfono: " + pedido.getTelefono() + "\n"
                         + "📦 Detalle: " + pedido.getDetalle();
            watiService.enviarMensaje("56952358357", aviso); // nuevo número del local

            return ResponseEntity.ok("Pago confirmado, comanda generada y aviso enviado.");
        } else {
            return ResponseEntity.status(404).body("Pedido no encontrado");
        }
    } catch (Exception e) {
        return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
    }

    }
}
