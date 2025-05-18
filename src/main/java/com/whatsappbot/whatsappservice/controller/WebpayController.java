package com.whatsappbot.whatsappservice.controller;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.repository.PedidoRepository;

import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RestController
@RequestMapping("/webpay")
@RequiredArgsConstructor
public class WebpayController {

    private final PedidoRepository pedidoRepository;

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
            String buyOrder = json.get("buy_order").asText(); // ej: pedido-8fj2kd2a

            Optional<PedidoEntity> pedidoOpt = pedidoRepository.findByPedidoId(buyOrder);
            if (pedidoOpt.isPresent()) {
                PedidoEntity pedido = pedidoOpt.get();
                pedido.setEstado("pagado");
                pedidoRepository.save(pedido);
                return ResponseEntity.ok("Pago confirmado");
            } else {
                return ResponseEntity.status(404).body("Pedido no encontrado");
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error interno");
        }
    }
}
