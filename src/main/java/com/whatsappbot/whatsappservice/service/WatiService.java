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

    // ✅ Enviar plantilla de confirmación de pedido
  public void enviarMensajeConTemplate(String telefono, String pedidoId, String linkComanda) throws IOException {
    String url = watiApiUrl + "/api/v1/sendTemplateMessage";

    Map<String, Object> data = new HashMap<>();
    data.put("template_name", "confirmacion_pedido_link");
    data.put("broadcast_name", "confirmacion_pedido_link");
    data.put("phone_number", telefono);

    List<Map<String, String>> parametros = new ArrayList<>();
    parametros.add(Map.of("name", "1", "value", pedidoId));
    parametros.add(Map.of("name", "2", "value", linkComanda));
    data.put("parameters", parametros);

    enviarPostWati(url, data, "plantilla de confirmación con link");
}

    // ✅ Enviar plantilla de ayuda automática
    public void enviarTemplateAyuda(String telefono, String nombre) throws IOException {
    String url = "https://live-mt-server.wati.io/" + tenantId + "/api/v1/sendTemplateMessage?whatsappNumber=" + telefono;

    Map<String, Object> data = new HashMap<>();
    data.put("template_name", "respuesta_ayuda1");
    data.put("broadcast_name", "respuesta_ayuda1");

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
    // ✅ Enviar mensaje de texto libre (requiere sesión iniciada)
    public void enviarMensajeTexto(String telefono, String mensaje) throws IOException {
        String url = watiApiUrl + "/api/v1/sendSessionMessage?whatsappNumber=" + telefono;

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

    // 🔁 Método común para plantillas
    private void enviarPostWati(String url, Map<String, Object> data, String descripcion) throws IOException {
    String json = mapper.writeValueAsString(data);

    System.out.println("📤 Enviando POST a WATI: " + url);
    System.out.println("📦 Payload JSON: " + json);

    RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
    Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();

    try (Response response = client.newCall(request).execute()) {
        String responseBody = response.body() != null ? response.body().string() : "sin cuerpo";
        if (!response.isSuccessful()) {
            System.err.println("❌ Error al enviar " + descripcion + " WATI:");
            System.err.println("Código HTTP: " + response.code());
            System.err.println("Respuesta: " + responseBody);
            throw new IOException("WATI devolvió error al enviar " + descripcion);
        } else {
            System.out.println("✅ " + descripcion + " enviada correctamente");
            System.out.println("📨 Respuesta WATI: " + responseBody);
        }
    }
}

    public void enviarMensajePagoEstatico(String telefono, Double total, String linkPago) throws IOException {
    String url = "https://live-mt-server.wati.io/" + tenantId + "/api/v1/sendTemplateMessage?whatsappNumber=" + telefono;

    Map<String, Object> data = new HashMap<>();
    data.put("template_name", "pago_estatico");
    data.put("broadcast_name", "pago_estatico");

    List<Map<String, String>> parametros = new ArrayList<>();
    // ⚠️ Si total es 0, mostramos un mensaje genérico, de lo contrario mostramos el valor
    String valorTotal = (total <= 0.0) ? "por definir" : String.format("$%.0f", total);
    parametros.add(Map.of("name", "1", "value", valorTotal));
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
            throw new IOException("❌ Error al enviar plantilla de pago estático WATI: Código " + response.code() + " - " + response.body().string());
        } else {
            System.out.println("📨 Plantilla de pago estático enviada correctamente");
        }
    }
}
public void enviarTemplateConfirmacionSimple(String telefono, String nombre) throws IOException {
    String url = watiApiUrl + "/api/v1/sendTemplateMessage";

    Map<String, Object> data = new HashMap<>();
    data.put("template_name", "confirmacion_pedido");
    data.put("broadcast_name", "confirmacion_pedido");
    data.put("phone_number", telefono);

    List<Map<String, String>> parametros = new ArrayList<>();
    parametros.add(Map.of("name", "1", "value", nombre));
    data.put("parameters", parametros);

    enviarPostWati(url, data, "confirmación simple");
}

    
}
