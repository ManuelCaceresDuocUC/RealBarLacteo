package com.whatsappbot.whatsappservice.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class WatiService {

    @Value("${wati.api.url}")
    private String watiApiUrl;

    @Value("${wati.api.key}")
    private String apiKey;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // ✅ 1. Enviar plantilla con parámetros
    public void enviarMensajeConTemplate(String telefono, String pedidoId, String linkPago) throws IOException {
        String url = watiApiUrl + "/api/v1/sendTemplateMessage";

        Map<String, Object> data = new HashMap<>();
        data.put("template_name", "pedido_confirmado");
        data.put("broadcast_name", "confirmacion_pedido");
        data.put("phone_number", telefono);

        List<Map<String, String>> parametros = new ArrayList<>();
        parametros.add(Map.of("name", "1", "value", pedidoId));
        parametros.add(Map.of("name", "2", "value", linkPago));
        data.put("parameters", parametros);

        String json = mapper.writeValueAsString(data);

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("❌ Error al enviar mensaje WATI: Código " + response.code() + " - " + response.body().string());
            }
        }
    }

    // ✅ 2. Enviar mensaje de texto libre (solo si el usuario escribió primero)
    public void enviarMensajeTexto(String telefono, String mensaje) throws IOException {
        String url = watiApiUrl + "/api/v1/sendSessionMessage";

        Map<String, String> data = new HashMap<>();
        data.put("phone_number", telefono);
        data.put("message", mensaje);

        String json = mapper.writeValueAsString(data);

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("❌ Error al enviar mensaje de texto WATI: Código " + response.code() + " - " + response.body().string());
            }
        }
    }
}
