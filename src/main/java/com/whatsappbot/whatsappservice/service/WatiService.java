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

@Value("${wati.tenantId}")
private String tenantId;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // ✅ Enviar plantilla con parámetros
    public void enviarMensajeConTemplate(String telefono, String pedidoId, String linkPago) throws IOException {
        String url = watiApiUrl + "/" + tenantId + "/api/v1/sendTemplateMessage?whatsappNumber=" + telefono;

        Map<String, Object> data = new HashMap<>();
        data.put("template_name", "confirmacion_pedido");
        data.put("broadcast_name", "confirmacion_pedido");

        // Solo enviamos el parámetro que corresponde a {{1}} de la plantilla
        List<Map<String, String>> parametros = new ArrayList<>();
        parametros.add(Map.of("name", "1", "value", "Manuel")); // puedes cambiarlo a pedidoId si lo prefieres
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
            } else {
                System.out.println("📨 Plantilla enviada exitosamente");
            }
        }
    }

    // ✅ Enviar mensaje de texto libre (solo si el cliente escribió primero)
    public void enviarMensajeTexto(String telefono, String mensaje) throws IOException {
        String url = watiApiUrl + "/" + tenantId + "/api/v1/sendSessionMessage?whatsappNumber=" + telefono;

        Map<String, String> data = new HashMap<>();
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
   public void enviarTemplateAyuda(String telefono, String nombre) throws IOException {
    String url = watiApiUrl + "/" + tenantId + "/api/v1/sendTemplateMessage?whatsappNumber=" + telefono;

    Map<String, Object> data = new HashMap<>();
    data.put("template_name", "confirmacion_pedido");  // Usa la plantilla real aprobada
    data.put("broadcast_name", "confirmacion_pedido");

    List<Map<String, String>> parametros = new ArrayList<>();
    parametros.add(Map.of("name", "1", "value", nombre));
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
            throw new IOException("❌ Error al enviar plantilla de ayuda WATI: Código " + response.code() + " - " + response.body().string());
        } else {
            System.out.println("📨 Plantilla de ayuda enviada correctamente");
        }
    }
}
}
