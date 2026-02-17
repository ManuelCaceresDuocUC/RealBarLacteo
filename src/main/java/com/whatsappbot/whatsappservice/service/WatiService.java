package com.whatsappbot.whatsappservice.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatiService {

    // Hemos vaciado los métodos para que no intenten conectar con WATI
    // pero mantenemos los nombres para que el proyecto siga compilando.

    public void enviarMensajeTexto(String telefono, String mensaje) {
        log.info("ℹ️ [WATI Desactivado] Se omitió envío de texto a: {}", telefono);
    }

    public void enviarMensajeConTemplate(String telefono, String pedidoId, String linkComanda) throws IOException {
        log.info("ℹ️ [WATI Desactivado] Se omitió envío de plantilla confirmación a: {}", telefono);
    }

    public void enviarTemplateAyuda(String telefono, String nombre) throws IOException {
        log.info("ℹ️ [WATI Desactivado] Se omitió envío de plantilla ayuda a: {}", telefono);
    }

    public void enviarMensajePagoEstatico(String telefono, double total, String linkPago) throws IOException {
        log.info("ℹ️ [WATI Desactivado] Se omitió envío de link de pago a: {}", telefono);
    }

    public void enviarTemplateConfirmacionSimple(String telefono, String nombre) throws IOException {
        log.info("ℹ️ [WATI Desactivado] Se omitió confirmación simple a: {}", telefono);
    }

    public void enviarMensajeBotones(String telefono, String headerText, String bodyText, String footerText, List<String> botones) {
        log.info("ℹ️ [WATI Desactivado] Se omitió envío de botones a: {}", telefono);
    }

    // El método privado que hacía las peticiones HTTP ya no es necesario
    private void enviarPostWati(String url, Map<String, Object> data, String descripcion) throws IOException {
        // No hace nada
    }
}