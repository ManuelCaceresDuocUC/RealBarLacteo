package com.whatsappbot.whatsappservice.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.whatsappbot.whatsappservice.model.PedidoEntity;

public interface PedidoRepository extends JpaRepository<PedidoEntity, Long> {
    Optional<PedidoEntity> findByPedidoId(String pedidoId);
    List<PedidoEntity> findByEstado(String estado);

}
