package com.whatsappbot.whatsappservice.controller;

import java.io.IOException;
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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
    public ResponseEntity<?> crearPedido(@RequestBody PedidoEntity pedido) {
        try {
            // Generar un ID único si no se envió
            if (pedido.getPedidoId() == null || pedido.getPedidoId().isEmpty()) {
                pedido.setPedidoId("pedido-" + UUID.randomUUID().toString().substring(0, 8));
            }

            pedido.setEstado("pendiente");
            PedidoEntity guardado = pedidoRepository.save(pedido);

            int monto = 5000; // Puedes calcularlo en base al detalle más adelante
            String linkPago = transbankService.generarLinkDePago(guardado.getPedidoId(), monto);

            // Enviar mensaje por WhatsApp al cliente
            String mensaje = "Hola! Tu pedido fue registrado.\nPuedes pagarlo aquí 👉 " + linkPago;
            watiService.enviarMensaje(guardado.getTelefono(), mensaje);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Pedido creado y link enviado por WhatsApp",
                    "pedidoId", guardado.getPedidoId(),
                    "linkPago", linkPago
            ));

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "No se pudo generar link o enviar mensaje",
                    "detalle", e.getMessage()
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
            watiService.enviarMensaje("56966798353", aviso); // número del local

            return ResponseEntity.ok("Pago confirmado, comanda generada y aviso enviado.");
        } else {
            return ResponseEntity.status(404).body("Pedido no encontrado");
        }
    } catch (Exception e) {
        return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
    }

    }
}
