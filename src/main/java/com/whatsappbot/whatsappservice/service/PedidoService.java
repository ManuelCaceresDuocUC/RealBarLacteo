package com.whatsappbot.whatsappservice.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.repository.PedidoRepository;

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
}
