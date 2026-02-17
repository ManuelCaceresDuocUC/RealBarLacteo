package com.whatsappbot.whatsappservice.service;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WhatsAppService {

    /**
     * Hemos simplificado el servicio al máximo.
     * Ya no necesita leer llaves de API ni conectar con servidores externos.
     */
    public void enviarMensaje(String numero, String mensaje) {
        // Simplemente logueamos la intención para que sepas qué está pasando en Render
        log.info("🟡 [WhatsApp Desactivado] Destinatario: {}, Mensaje: {}", numero, mensaje);
    }
}