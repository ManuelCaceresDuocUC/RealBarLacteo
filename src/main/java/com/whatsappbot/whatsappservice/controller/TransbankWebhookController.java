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
    private PedidoContextService pedidoContext;

    @PostMapping("/webhook")
    public ResponseEntity<String> recibirWebhookDeTransbank(@RequestBody Map<String, Object> payload) {
        try {
            String status = (String) payload.get("status");
            String token = (String) payload.get("token");

            if (!"AUTHORIZED".equalsIgnoreCase(status)) {
                return ResponseEntity.badRequest().body("❌ Pago no autorizado");
            }

            PedidoEntity pedido = pedidoService.buscarPorToken(token);
            if (pedido == null) {
                return ResponseEntity.badRequest().body("❌ Pedido no encontrado con token: " + token);
            }

            // ✅ Actualizar estado
            pedido.setEstado("pagado");
            pedidoService.guardarPedido(pedido);

            // 📄 Generar comanda
            comandaService.generarPDF(pedido);

            // 📲 Notificar por WhatsApp
            whatsAppService.enviarMensaje(pedido.getTelefono(),
                "🎉 ¡Gracias por tu pago! Tu pedido " + pedido.getPedidoId() + " está en preparación.");

            // 🧹 Limpiar contexto del cliente
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
