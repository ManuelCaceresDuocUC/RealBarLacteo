package com.whatsappbot.whatsappservice.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.whatsappbot.whatsappservice.model.PedidoEntity;

public interface PedidoRepository extends JpaRepository<PedidoEntity, Long> {

    Optional<PedidoEntity> findByPedidoId(String pedidoId);

    List<PedidoEntity> findByEstado(String estado);

    // Ya declarado
    boolean existsByTelefonoAndEstado(String telefono, String estado);

    // Ya declarado
    List<PedidoEntity> findByTelefonoAndEstadoAndFechaCreacionAfter(String telefono, String estado, OffsetDateTime  fechaCreacionMin);

    // ✅ Agregar este para que compile bien tu controlador
    Optional<PedidoEntity> findByTelefonoAndEstado(String telefono, String estado);
    @Query("SELECT p FROM PedidoEntity p WHERE p.estado = 'pagado' ORDER BY p.fechaCreacion DESC")
    Optional<PedidoEntity> findUltimoPedidoPagado();
Optional<PedidoEntity> findTopByTelefonoOrderByFechaCreacionDesc(String telefono);
List<PedidoEntity> findByEstadoAndFechaCreacionBefore(String estado, OffsetDateTime  fechaCreacion);
PedidoEntity findByTokenWs(String tokenWs);

Optional<PedidoEntity> findTopByTelefonoAndEstadoOrderByFechaCreacionDesc(String telefono, String estado);

}
