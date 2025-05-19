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

    private final String API_URL = "https://webpay3g.transbank.cl/rswebpaytransaction/api/webpay/v1.3/transactions";
    private final String COMMERCE_CODE = System.getenv("TRANSBANK_COMMERCE_CODE");
    private final String API_KEY = System.getenv("TRANSBANK_API_KEY");

    @PostConstruct
    public void verificarVariablesEntorno() {
        log.info("🔍 Verificando variables de entorno...");
        log.info("TRANSBANK_COMMERCE_CODE: {}", COMMERCE_CODE);
        log.info("TRANSBANK_API_KEY: {}", (API_KEY != null ? "[CARGADA]" : "❌ NO CARGADA"));
    }

    public String generarLinkDePago(String buyOrder, int amount) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Map<String, Object> payload = new HashMap<>();
        payload.put("buy_order", buyOrder);
        payload.put("session_id", UUID.randomUUID().toString());
        payload.put("amount", amount);
        payload.put("return_url", "https://realbarlacteo.onrender.com/webpay/confirmacion");

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(payload);

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Tbk-Api-Key-Id", COMMERCE_CODE)
                .addHeader("Tbk-Api-Key-Secret", API_KEY)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "sin detalle";
                throw new IOException("Error al crear transacción. Código: " + response.code() + " - " + errorBody);
            }
            JsonNode node = mapper.readTree(response.body().string());
            return "https://webpay3g.transbank.cl/webpayserver/initTransaction?token=" + node.get("token").asText();
        }
    }
}