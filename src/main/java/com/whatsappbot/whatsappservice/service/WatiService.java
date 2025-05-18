package com.whatsappbot.whatsappservice.service;

import java.io.IOException;
import java.util.HashMap;
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

    private final String WATI_URL = "https://app.wati.io/api/v1/sendSessionMessage";
    private final String API_KEY = System.getenv("WATI_API_KEY");

    public void enviarMensaje(String telefono, String mensaje) throws IOException {
        OkHttpClient client = new OkHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        Map<String, String> data = new HashMap<>();
        data.put("phone", telefono); // debe ir sin el "+" (ej: 56966798353)
        data.put("message", mensaje);

        String json = mapper.writeValueAsString(data);

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(WATI_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Error al enviar mensaje WATI: " + response);
        }
    }
}
