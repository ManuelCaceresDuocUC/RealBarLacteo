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
import com.whatsappbot.whatsappservice.service.PedidoService;
import com.whatsappbot.whatsappservice.service.WhatsAppService;

@RestController
@RequestMapping("/webpay")
public class NotificacionPagoController {

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ComandaService comandaService;

    @PostMapping("/confirmacion")
public ResponseEntity<String> recibirPago(@RequestBody Map<String, Object> payload) {
    try {
        String pedidoId = (String) payload.get("pedidoId");
        System.out.println("👉 ID recibido: " + pedidoId);

        PedidoEntity pedido = pedidoService.buscarPedidoPorId(pedidoId);
        if (pedido == null) {
            System.out.println("❌ Pedido no encontrado");
            return ResponseEntity.badRequest().body("❌ Pedido no encontrado: " + pedidoId);
        }

        whatsAppService.enviarMensaje(pedido.getTelefono(),
            "🎉 Gracias por tu pago. Tu pedido " + pedidoId + " está siendo preparado. 🧾");

        comandaService.generarPDF(pedido);

        pedido.setEstado("pagado");
        pedidoService.guardarPedido(pedido);

        return ResponseEntity.ok("✅ Pago confirmado y comanda PDF generada");

    } catch (Exception e) {
        System.out.println("❌ Excepción en el controlador:");
        e.printStackTrace();
        return ResponseEntity.badRequest().body("❌ Error al procesar la notificación");
    }
}}