package com.whatsappbot.whatsappservice.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WhatsAppService {

    @Value("${wati.api.key}")
    private String apiKey;

    private final String watiUrl = "https://app.wati.io/api/v1/sendSessionMessage";
    @Value("${wati.simulacion:false}")
    private boolean modoSimulacion;
    public void enviarMensaje(String numero, String mensaje) {
    if (modoSimulacion) {
        System.out.println("🟡 [SIMULADO] Mensaje a " + numero + ": " + mensaje);
        return;
    }

    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + apiKey);
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, String> body = new HashMap<>();
    body.put("phone", numero);
    body.put("messageText", mensaje);

    HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

    ResponseEntity<String> response = restTemplate.postForEntity(watiUrl, request, String.class);
    System.out.println("✅ Respuesta de WATI: " + response.getBody());
}

}
