package com.whatsappbot.whatsappservice.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.whatsappbot.whatsappservice.dto.ProductoCarritoDTO;
import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.repository.PedidoRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    public void guardarPedido(PedidoEntity pedido) {
        pedidoRepository.save(pedido);
    }

    public PedidoEntity buscarPedidoPorId(String pedidoId) {
        return pedidoRepository.findByPedidoId(pedidoId).orElse(null);
    }

    public List<PedidoEntity> obtenerTodos() {
        return pedidoRepository.findAll();
    }
    public List<PedidoEntity> buscarPorEstado(String estado) {
    return pedidoRepository.findByEstado(estado);
    }
    public String crearPedidoConDetalle(String telefono, List<ProductoCarritoDTO> productos, double total) {
    String pedidoId = "pedido-" + UUID.randomUUID().toString().substring(0, 8);

    StringBuilder detalle = new StringBuilder();
    for (ProductoCarritoDTO producto : productos) {
        detalle.append("- ")
               .append(producto.getQuantity())
               .append(" x ")
               .append(producto.getName())
               .append("\n");
    }

    PedidoEntity pedido = new PedidoEntity();
    pedido.setPedidoId(pedidoId);
    pedido.setTelefono(telefono);
    pedido.setDetalle(detalle.toString().trim());
    pedido.setEstado("pendiente");

    pedidoRepository.save(pedido);

    log.info("🛒 Pedido creado con ID: {}", pedidoId);
    return pedidoId;
}
}
