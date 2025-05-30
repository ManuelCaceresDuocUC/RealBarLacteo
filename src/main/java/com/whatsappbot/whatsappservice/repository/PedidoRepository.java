package com.whatsappbot.whatsappservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.whatsappbot.whatsappservice.model.PedidoEntity;

public interface PedidoRepository extends JpaRepository<PedidoEntity, Long> {
    Optional<PedidoEntity> findByPedidoId(String pedidoId);
    List<PedidoEntity> findByEstado(String estado);
    boolean existsByTelefonoAndEstado(String telefono, String estado);
    PedidoEntity findFirstByTelefonoAndEstado(String telefono, String estado);

}
