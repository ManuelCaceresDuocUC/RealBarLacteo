package com.whatsappbot.whatsappservice.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.service.ComandaService;
import com.whatsappbot.whatsappservice.service.PedidoContextService;
import com.whatsappbot.whatsappservice.service.PedidoService;
import com.whatsappbot.whatsappservice.service.WhatsAppService;

@RestController
@RequestMapping("/webpay")
public class TransbankWebhookController {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ComandaService comandaService;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private PedidoContextService pedidoContext; // ✅ Agregado para limpiar el contexto

    @PostMapping("/webhook")
    public ResponseEntity<String> recibirWebhookDeTransbank(@RequestBody Map<String, Object> payload) {
        try {
            String status = (String) payload.get("status");
            String pedidoId = (String) payload.get("buy_order"); // viene como PED-XXXXX

            if (!"AUTHORIZED".equalsIgnoreCase(status)) {
                return ResponseEntity.badRequest().body("❌ Pago no autorizado");
            }

            PedidoEntity pedido = pedidoService.buscarPedidoPorId(pedidoId);
            if (pedido == null) {
                return ResponseEntity.badRequest().body("❌ Pedido no encontrado: " + pedidoId);
            }

            // Actualizar estado
            pedido.setEstado("pagado");
            pedidoService.guardarPedido(pedido);

            // Generar comanda
            comandaService.generarPDF(pedido);

            // (Opcional) Notificar por WhatsApp
            whatsAppService.enviarMensaje(pedido.getTelefono(),
                "🎉 ¡Gracias por tu pago! Tu pedido " + pedidoId + " está en preparación.");

            // 🧹 Limpiar el contexto del cliente
            String telefono = pedido.getTelefono();
            pedidoContext.pedidoTemporalPorTelefono.remove(telefono);
            pedidoContext.indicacionPreguntadaPorTelefono.remove(telefono);
            pedidoContext.ultimoMensajeProcesadoPorNumero.remove(telefono);

            return ResponseEntity.ok("✅ Pago confirmado, comanda generada y contexto limpiado");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("❌ Error al procesar notificación de Transbank");
        }
    }
}
