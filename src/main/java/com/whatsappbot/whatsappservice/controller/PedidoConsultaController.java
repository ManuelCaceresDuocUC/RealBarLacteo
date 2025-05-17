package com.whatsappbot.whatsappservice.controller;

import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedido")
public class PedidoConsultaController {

    @Autowired
    private PedidoService pedidoService;

    // GET /pedido/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPedidoPorId(@PathVariable String id) {
        PedidoEntity pedido = pedidoService.buscarPedidoPorId(id);
        if (pedido == null) {
            return ResponseEntity.status(404).body("❌ Pedido no encontrado: " + id);
        }
        return ResponseEntity.ok(pedido);
    }

    // GET /pedido
    @GetMapping
    public ResponseEntity<List<PedidoEntity>> obtenerTodosLosPedidos() {
        List<PedidoEntity> pedidos = pedidoService.obtenerTodos();
        return ResponseEntity.ok(pedidos);
    }
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<PedidoEntity>> obtenerPorEstado(@PathVariable String estado) {
    List<PedidoEntity> pedidos = pedidoService.buscarPorEstado(estado);
    if (pedidos.isEmpty()) {
        return ResponseEntity.status(404).body(null);
    }
    return ResponseEntity.ok(pedidos);
    }
}
