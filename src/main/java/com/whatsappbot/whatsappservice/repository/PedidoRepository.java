package com.whatsappbot.whatsappservice.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.whatsappbot.whatsappservice.model.PedidoEntity;

public interface PedidoRepository extends JpaRepository<PedidoEntity, Long> {

    /* === Búsquedas clave === */
    Optional<PedidoEntity> findByPedidoId(String pedidoId);
    boolean existsByPedidoId(String pedidoId);
    List<PedidoEntity> findByLocal(String local);
    List<PedidoEntity> findByTelefono(String telefono);
    List<PedidoEntity> findByEstado(String estado);
    Optional<PedidoEntity> findTopByTelefonoOrderByFechaCreacionDesc(String telefono);
    Optional<PedidoEntity> findTopByEstadoOrderByFechaCreacionDesc(String estado); // reemplaza findUltimoPedidoPagado

    /* === Reglas temporales que ya usas === */
    boolean existsByTelefonoAndEstado(String telefono, String estado);
    List<PedidoEntity> findByTelefonoAndEstadoAndFechaCreacionAfter(
        String telefono, String estado, OffsetDateTime fechaCreacionMin
    );
    Optional<PedidoEntity> findByTelefonoAndEstado(String telefono, String estado);
    List<PedidoEntity> findByEstadoAndFechaCreacionBefore(String estado, OffsetDateTime fechaCreacion);
    PedidoEntity findByTokenWs(String tokenWs);
    Optional<PedidoEntity> findTopByTelefonoAndEstadoOrderByFechaCreacionDesc(String telefono, String estado);

    /* === Eliminación === */
    // Por id (ya lo provee JpaRepository: deleteById, existsById)
    // Por pedidoId (útil como fallback desde el frontend)
    long deleteByPedidoId(String pedidoId);
}
