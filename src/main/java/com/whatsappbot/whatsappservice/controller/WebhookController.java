package com.whatsappbot.whatsappservice.controller;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.whatsappbot.whatsappservice.bot.BotCommandProcessor;
import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.service.PedidoService;
import com.whatsappbot.whatsappservice.service.WhatsAppService;

@RestController
@RequestMapping("/wati")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private WhatsAppService whatsAppService;
    @Autowired
    private PedidoService pedidoService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> recibirMensaje(@RequestBody Map<String, Object> payload) {
        try {
            Object dataObj = payload.get("data");

            if (dataObj instanceof Map<?, ?> rawMap) {
                Object mensajeObj = rawMap.get("message");
                Object numeroObj = rawMap.get("waId");

                if (mensajeObj instanceof String mensaje && numeroObj instanceof String numero) {

                    // Detectar si el mensaje es un pedido desde el carrito nativo
                    if (mensaje.contains("pedido:") && mensaje.contains("×") && mensaje.contains("total")) {
                        String pedidoId = "PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                        PedidoEntity pedido = new PedidoEntity(pedidoId, numero, mensaje);
                        pedidoService.guardarPedido(pedido);                                whatsAppService.enviarMensaje(numero, "✅ Recibimos tu pedido 🧾\nPuedes pagar aquí 👉 https://barlacteo.cl/pagar?pedido=" + pedidoId);
                        return ResponseEntity.ok().build();
                    }

                    // Si no es pedido, procesar normalmente
                    String respuesta = BotCommandProcessor.procesar(mensaje);
                    whatsAppService.enviarMensaje(numero, respuesta);
                }
            }
        } catch (Exception e) {
            logger.error("Error al procesar el mensaje", e);
        }
        return ResponseEntity.ok().build();
    }
    public PedidoEntity getPedidoPorId(String id) {
    return pedidoService.buscarPedidoPorId(id);
}
}
