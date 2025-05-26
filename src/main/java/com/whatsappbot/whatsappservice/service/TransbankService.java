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

    private final String commerceCode = System.getenv("TRANSBANK_COMMERCE_CODE");
    private final String apiKey = System.getenv("TRANSBANK_API_KEY");
    private final String returnUrl = System.getenv("TRANSBANK_RETURN_URL");

    private WebpayPlus.Transaction transaction;

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
        }
    }
}
