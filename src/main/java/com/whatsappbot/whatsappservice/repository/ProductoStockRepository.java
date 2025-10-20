package com.whatsappbot.whatsappservice.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import com.whatsappbot.whatsappservice.model.ProductoStockEntity;

public interface ProductoStockRepository extends JpaRepository<ProductoStockEntity, Long> {

    Optional<ProductoStockEntity> findByNombreIgnoreCase(String nombre);
    Optional<ProductoStockEntity> findByNombreIgnoreCaseAndDisponibleTrue(String nombre);

    @Modifying
    @Transactional
    @Query("update ProductoStockEntity p set p.disponible = :disp where p.id = :id")
    int setDisponible(@Param("id") Long id, @Param("disp") boolean disponible);

    @Modifying
    @Transactional
    @Query("update ProductoStockEntity p set p.stock = :stock where p.id = :id")
    int setStock(@Param("id") Long id, @Param("stock") int stock);
}
