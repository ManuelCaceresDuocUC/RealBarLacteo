package com.whatsappbot.whatsappservice.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.whatsappbot.whatsappservice.dto.PagoResponseDTO;

import cl.transbank.common.IntegrationType;
import cl.transbank.webpay.common.WebpayOptions;
import cl.transbank.webpay.webpayplus.WebpayPlus;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCommitResponse;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCreateResponse;
import jakarta.annotation.PostConstruct;

@Service
public class TransbankService {

    private static final Logger log = LoggerFactory.getLogger(TransbankService.class);

<<<<<<< HEAD
    private final String API_URL = "https://webpay3gint.transbank.cl/rswebpaytransaction/api/webpay/v1.3/transactions";
    
    private final String COMMERCE_CODE = System.getenv("TRANSBANK_COMMERCE_CODE");
    private final String API_KEY = System.getenv("TRANSBANK_API_KEY");
=======
    private final String commerceCode = System.getenv("TRANSBANK_COMMERCE_CODE");
    private final String apiKey = System.getenv("TRANSBANK_API_KEY");
    private final String returnUrl = System.getenv("TRANSBANK_RETURN_URL");

    private WebpayPlus.Transaction transaction;
>>>>>>> rollback-pago2

    @PostConstruct
    public void init() {
        log.info("🔍 Verificando variables de entorno...");
        if (commerceCode == null || apiKey == null || returnUrl == null) {
            throw new IllegalStateException("❌ Variables de entorno TRANSBANK no configuradas correctamente.");
        }

        WebpayOptions options = new WebpayOptions(commerceCode, apiKey, IntegrationType.TEST);
        transaction = new WebpayPlus.Transaction(options);

        log.info("✅ Variables de entorno cargadas correctamente.");
    }

<<<<<<< HEAD
    public Map<String, String> generarLinkDePago(String buyOrder, int amount) throws IOException {
    OkHttpClient client = new OkHttpClient();

    Map<String, Object> payload = new HashMap<>();
    payload.put("buy_order", buyOrder);
    payload.put("session_id", UUID.randomUUID().toString());
    payload.put("amount", amount);
    payload.put("return_url", "https://realbarlacteo.onrender.com/webpay/confirmacion"); // Tu backend

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(payload);

    RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

    // ✅ USA el endpoint de integración (NO el de producción)
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
=======
    public WebpayPlusTransactionCommitResponse confirmarTransaccion(String token) throws Exception {
        return transaction.commit(token);
    }

    public PagoResponseDTO generarLinkDePago(String buyOrder, int amount) {
        try {
            String sessionId = UUID.randomUUID().toString();
            WebpayPlusTransactionCreateResponse response = transaction.create(buyOrder, sessionId, amount, returnUrl);
            return new PagoResponseDTO(response.getUrl(), response.getToken());
        } catch (Exception e) {
            log.error("❌ Error al crear la transacción Webpay: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el link de pago con Transbank");
>>>>>>> rollback-pago2
        }

        JsonNode node = mapper.readTree(response.body().string());

        return Map.of(
            // ✅ Este es el dominio de redirección correcto para entorno de integración
            "url", "https://webpay3gint.transbank.cl/webpayserver/initTransaction?token=" + node.get("token").asText(),
            "token", node.get("token").asText()
        );
    }
}
<<<<<<< HEAD

}
=======
>>>>>>> rollback-pago2
