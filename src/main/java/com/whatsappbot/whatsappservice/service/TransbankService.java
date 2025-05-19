package com.whatsappbot.whatsappservice.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class TransbankService {

    private final String API_URL = "https://webpay3g.transbank.cl/rswebpaytransaction/api/webpay/v1.3/transactions";
    private final String COMMERCE_CODE = System.getenv("TRANSBANK_COMMERCE_CODE");
    private final String API_KEY = System.getenv("TRANSBANK_API_KEY");

    public String generarLinkDePago(String buyOrder, int amount) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Map<String, Object> payload = new HashMap<>();
        payload.put("buy_order", buyOrder);
        payload.put("session_id", UUID.randomUUID().toString());
        payload.put("amount", amount);
        payload.put("return_url", "https://realbarlacteo-1.onrender.com/api/pedidos/confirmacion");

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
            if (!response.isSuccessful()) throw new IOException("Error al crear transacción");
            JsonNode node = mapper.readTree(response.body().string());
            return "https://webpay3g.transbank.cl/webpayserver/initTransaction?token=" + node.get("token").asText();
        }
    }
}