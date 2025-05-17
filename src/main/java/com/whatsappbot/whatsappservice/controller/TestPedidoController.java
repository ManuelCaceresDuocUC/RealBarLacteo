package com.whatsappbot.whatsappservice.controller;

import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/test")
public class TestPedidoController {

    @Autowired
    private PedidoService pedidoService;

    @PostMapping("/pedido")
    public ResponseEntity<String> generarPedidoPrueba() {
        String pedidoId = "PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String telefono = "56912345678";
        String detalle = "🛒 Pedido:\n1 × Yogur ($1.200)\n2 × Leche ($1.000)\nTotal: $3.200";

        PedidoEntity pedido = new PedidoEntity(pedidoId, telefono, detalle);
        pedidoService.guardarPedido(pedido);

        return ResponseEntity.ok("✅ Pedido de prueba guardado con ID: " + pedidoId);
    }
}
