package com.whatsappbot.whatsappservice.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.service.ComandaService;
import com.whatsappbot.whatsappservice.service.PedidoContextService;
import com.whatsappbot.whatsappservice.service.PedidoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/webpay")
@RequiredArgsConstructor // Usamos constructor para ser consistentes con los otros controllers
public class TransbankWebhookController {

    private final PedidoService pedidoService;
    private final ComandaService comandaService;
    private final PedidoContextService pedidoContext;

    @PostMapping("/webhook")
    public ResponseEntity<String> recibirWebhookDeTransbank(@RequestBody Map<String, Object> payload) {
        try {
            String status = (String) payload.get("status");
            String token = (String) payload.get("token");

            log.info("🔔 Webhook recibido de Transbank - Token: {}, Status: {}", token, status);

            if (!"AUTHORIZED".equalsIgnoreCase(status)) {
                return ResponseEntity.badRequest().body("❌ Pago no autorizado");
            }

            PedidoEntity pedido = pedidoService.buscarPorToken(token);
            if (pedido == null) {
                log.error("❌ Webhook: Pedido no encontrado con token: {}", token);
                return ResponseEntity.badRequest().body("❌ Pedido no encontrado");
            }

            // ✅ Actualizar estado si no estaba pagado (evita duplicados)
            if (!"pagado".equalsIgnoreCase(pedido.getEstado())) {
                pedido.setEstado("pagado");
                pedidoService.guardarPedido(pedido);
                log.info("✅ Pedido {} marcado como pagado via Webhook", pedido.getPedidoId());
            }

            // 📄 Generar comanda
            comandaService.generarPDF(pedido);

            // 🧹 Limpiar contexto del cliente
            String telefono = pedido.getTelefono();
            pedidoContext.pedidoTemporalPorTelefono.remove(telefono);
            pedidoContext.indicacionPreguntadaPorTelefono.remove(telefono);
            pedidoContext.ultimoMensajeProcesadoPorNumero.remove(telefono);

            return ResponseEntity.ok("✅ Pago procesado exitosamente");
            
        } catch (Exception e) {
            log.error("❌ Error en webhook de Transbank", e);
            return ResponseEntity.internalServerError().body("❌ Error al procesar notificación");
        }
    }
}