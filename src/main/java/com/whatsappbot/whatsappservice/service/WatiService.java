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

    // 🔐 Variables de configuración cargadas desde application.properties o variables de entorno
    @Value("${wati.api.url}")
    private String watiApiUrl;

    @Value("${wati.api.key}")
    private String apiKey;

    @Value("${wati.tenantId}")
    private String tenantId;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // ✅ 1. Enviar mensaje de texto simple (sesión activa)
    public void enviarMensajeTexto(String telefono, String mensaje) {
        try {
            telefono = telefono.replace("+", ""); // Limpieza del número

            // 🟢 Construcción de URL para enviar sesión
            String url = watiApiUrl + "/" + tenantId + "/api/v1/sendSessionMessage/" + telefono
                    + "?messageText=" + java.net.URLEncoder.encode(mensaje, java.nio.charset.StandardCharsets.UTF_8);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create("", null)) // WATI requiere un body vacío
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "sin contenido";
                if (!response.isSuccessful()) {
                    System.err.println("❌ Error al enviar mensaje por WATI");
                    System.err.println("Código: " + response.code());
                    System.err.println("Respuesta: " + responseBody);
                } else {
                    System.out.println("✅ Mensaje enviado correctamente por WATI: " + responseBody);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Excepción al enviar mensaje WATI:");
            e.printStackTrace();
        }
    }

    // ✅ 2. Enviar plantilla de confirmación con link de comanda
    public void enviarMensajeConTemplate(String telefono, String pedidoId, String linkComanda) throws IOException {
        telefono = telefono.replace("+", "");

        String url = watiApiUrl + "/" + tenantId + "/api/v1/sendTemplateMessage?whatsappNumber=" + telefono;

        Map<String, Object> data = new HashMap<>();
        data.put("template_name", "confirmacion_pedido_link");
        data.put("broadcast_name", "confirmacion_pedido_link");

        List<Map<String, String>> parametros = new ArrayList<>();
        parametros.add(Map.of("name", "1", "value", pedidoId));
        parametros.add(Map.of("name", "2", "value", linkComanda));
        data.put("parameters", parametros);

        enviarPostWati(url, data, "plantilla de confirmación con link");
    }

    // ✅ 3. Enviar plantilla de ayuda automática con nombre del cliente
    public void enviarTemplateAyuda(String telefono, String nombre) throws IOException {
        telefono = telefono.replace("+", "");

        String url = watiApiUrl + "/" + tenantId + "/api/v1/sendTemplateMessage?whatsappNumber=" + telefono;

        Map<String, Object> data = new HashMap<>();
        data.put("template_name", "respuesta_ayuda1");
        data.put("broadcast_name", "respuesta_ayuda1");

        List<Map<String, String>> parametros = new ArrayList<>();
        parametros.add(Map.of("name", "1", "value", nombre));
        data.put("parameters", parametros);

        enviarPostWati(url, data, "plantilla de ayuda");
    }

    // ✅ 4. Enviar plantilla de pago estático con monto y link
    public void enviarMensajePagoEstatico(String telefono, Double total, String linkPago) throws IOException {
        telefono = telefono.replace("+", "");

        String url = watiApiUrl + "/" + tenantId + "/api/v1/sendTemplateMessage?whatsappNumber=" + telefono;

        Map<String, Object> data = new HashMap<>();
        data.put("template_name", "pago_estatico");
        data.put("broadcast_name", "pago_estatico");

        List<Map<String, String>> parametros = new ArrayList<>();
        String valorTotal = (total <= 0.0) ? "por definir" : String.format("$%.0f", total);
        parametros.add(Map.of("name", "1", "value", valorTotal));
        parametros.add(Map.of("name", "2", "value", linkPago));
        data.put("parameters", parametros);

        enviarPostWati(url, data, "plantilla de pago estático");
    }

    // ✅ 5. Enviar plantilla de confirmación simple con nombre
    public void enviarTemplateConfirmacionSimple(String telefono, String nombre) throws IOException {
        telefono = telefono.replace("+", "");

        String url = watiApiUrl + "/" + tenantId + "/api/v1/sendTemplateMessage?whatsappNumber=" + telefono;

        Map<String, Object> data = new HashMap<>();
        data.put("template_name", "confirmacion_pedido");
        data.put("broadcast_name", "confirmacion_pedido");

        List<Map<String, String>> parametros = new ArrayList<>();
        parametros.add(Map.of("name", "1", "value", nombre));
        data.put("parameters", parametros);

        enviarPostWati(url, data, "confirmación simple");
    }

    // 🔁 Método reutilizable para enviar POST a WATI con plantillas
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
}
