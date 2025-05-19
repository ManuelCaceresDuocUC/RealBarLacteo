package com.whatsappbot.whatsappservice.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class WatiService {

    private final String WATI_URL = "https://live-server.wati.io/api/v1/sendTemplateMessage";
    private final String API_KEY = System.getenv("WATI_API_KEY");

    public void enviarMensaje(String telefono, String mensajeConLink) throws IOException {
        OkHttpClient client = new OkHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> payload = new HashMap<>();
        payload.put("template_name", "pedido_confirmado"); // usa el nombre real
        payload.put("broadcast_name", "pedido_bar_lacteo");
        payload.put("phone_number", telefono);

        List<Map<String, String>> parameters = new ArrayList<>();
        Map<String, String> param = new HashMap<>();
        param.put("name", "1");
        param.put("value", mensajeConLink);
        parameters.add(param);
        payload.put("parameters", parameters);

        String json = mapper.writeValueAsString(payload);

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(WATI_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("❌ Error al enviar mensaje WATI: Código " + response.code() + " - " + response.body().string());
        }
    }
}