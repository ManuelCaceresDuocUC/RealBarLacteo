package com.whatsappbot.whatsappservice.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType; // ✅ Este es el de Spring
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
@RequiredArgsConstructor
public class WatiService {

    @Value("${wati.api.url}")
    private String watiApiUrl;

    @Value("${wati.api.key}")
    private String apiKey;

    @Value("${wati.tenantId}")
    private String tenantId;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    public void enviarMensajeTexto(String telefono, String mensaje) {
        try {
            telefono = telefono.replace("+", "");
            String url = watiApiUrl + "/" + tenantId + "/api/v1/sendSessionMessage/" + telefono
                    + "?messageText=" + java.net.URLEncoder.encode(mensaje, java.nio.charset.StandardCharsets.UTF_8);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create("", null))
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

    public void enviarMensajeConTemplate(String telefono, String pedidoId, String linkComanda) throws IOException {
        telefono = telefono.replace("+", "");
        String url = watiApiUrl + "/" + tenantId + "/api/v1/sendTemplateMessage?whatsappNumber=" + telefono;

        Map<String, Object> data = new HashMap<>();
        data.put("template_name", "confirmacion_pedido_link_2");
        data.put("broadcast_name", "confirmacion_pedido_link_2");

        List<Map<String, String>> parametros = new ArrayList<>();
        parametros.add(Map.of("name", "1", "value", pedidoId));
        parametros.add(Map.of("name", "2", "value", linkComanda));
        data.put("parameters", parametros);

        enviarPostWati(url, data, "plantilla de confirmación con link");
    }

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

    public void enviarMensajePagoEstatico(String telefono, int total, String linkPago) throws IOException {
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

    private void enviarPostWati(String url, Map<String, Object> data, String descripcion) throws IOException {
        String json = mapper.writeValueAsString(data);

        System.out.println("📤 Enviando POST a WATI: " + url);
        System.out.println("📦 Payload JSON: " + json);

        RequestBody body = RequestBody.create(json, okhttp3.MediaType.parse("application/json"));
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

    public void enviarMensajeBotones(String telefono, String headerText, String bodyText, String footerText, List<String> botones) {
        try {
            String url = watiApiUrl + "/" + tenantId + "/api/v1/sendInteractiveButtonsMessage?whatsappNumber=" + telefono;

            ObjectNode payload = new ObjectNode(new com.fasterxml.jackson.databind.node.JsonNodeFactory(false));
            ObjectNode header = payload.putObject("header");
            header.put("type", "Text");
            header.put("text", headerText);

            payload.put("body", bodyText);
            payload.put("footer", footerText);

            ArrayNode buttonsArray = payload.putArray("buttons");
            for (String b : botones) {
                ObjectNode button = buttonsArray.addObject();
                button.put("type", "reply");
                button.put("text", b);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON); // <-- Este es de Spring
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);
            restTemplate.postForEntity(url, entity, String.class);

            System.out.println("✅ Botones enviados correctamente por WATI");

        } catch (Exception e) {
            System.err.println("❌ Error enviando botones WATI:");
            e.printStackTrace();
        }
    }
}
