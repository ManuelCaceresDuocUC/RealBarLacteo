package com.whatsappbot.whatsappservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.whatsappbot.whatsappservice.model.ProductoStockEntity;

public interface ProductoStockRepository extends JpaRepository<ProductoStockEntity, Long> {

    Optional<ProductoStockEntity> findByNombreIgnoreCase(String nombre);
    Optional<ProductoStockEntity> findByNombreIgnoreCaseAndDisponibleTrue(String nombre);

}
