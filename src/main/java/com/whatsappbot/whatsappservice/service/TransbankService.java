package com.whatsappbot.whatsappservice.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
@Service
public class TransbankService {

    private static final Logger log = LoggerFactory.getLogger(TransbankService.class);

    private final String API_URL = "https://webpay3gint.transbank.cl/rswebpaytransaction/api/webpay/v1.3/transactions";
    private final String COMMERCE_CODE = System.getenv("TRANSBANK_COMMERCE_CODE");
    private final String API_KEY = System.getenv("TRANSBANK_API_KEY");

    @PostConstruct
    public void verificarVariablesEntorno() {
        log.info("🔍 Verificando variables de entorno...");
        log.info("TRANSBANK_COMMERCE_CODE: {}", COMMERCE_CODE);
        log.info("TRANSBANK_API_KEY: {}", (API_KEY != null ? "[CARGADA]" : "❌ NO CARGADA"));
    }

    public Map<String, String> generarLinkDePago(String buyOrder, int amount) throws IOException {
    OkHttpClient client = new OkHttpClient();

    Map<String, Object> payload = new HashMap<>();
    payload.put("buy_order", buyOrder);
    payload.put("session_id", UUID.randomUUID().toString());
    payload.put("amount", amount);
    payload.put("return_url", "https://realbarlacteo.onrender.com/webpay/confirmacion"); // Tu backend

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(payload);

    RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

    // ✅ USA el endpoint de integración (NO el de producción)
    Request request = new Request.Builder()
            .url("https://webpay3gint.transbank.cl/rswebpaytransaction/api/webpay/v1.3/transactions")
            .addHeader("Tbk-Api-Key-Id", "597055555532") // Código de comercio de integración
            .addHeader("Tbk-Api-Key-Secret", "579B532A7440BB0C9079DED094D31EA1615BACEB56610332264630D42D0A36B1C") // API Key integración
            .post(body)
            .build();

    try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful()) {
            String errorBody = response.body() != null ? response.body().string() : "sin detalle";
            throw new IOException("Error al crear transacción. Código: " + response.code() + " - " + errorBody);
        }

        JsonNode node = mapper.readTree(response.body().string());

        return Map.of(
            // ✅ Este es el dominio de redirección correcto para entorno de integración
            "url", "https://webpay3gint.transbank.cl/webpayserver/initTransaction?token=" + node.get("token").asText(),
            "token", node.get("token").asText()
        );
    }
}
}