package com.whatsappbot.whatsappservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.whatsappbot.whatsappservice.model.ProductoStockEntity;
import com.whatsappbot.whatsappservice.repository.ProductoStockRepository;

@Service
public class StockService {
    private final ProductoStockRepository repo;
    public StockService(ProductoStockRepository repo){ this.repo = repo; }

    @Transactional
    public void disminuirPorCompra(String nombreProducto, int cantidad){
        ProductoStockEntity p = repo.findByNombreIgnoreCaseAndDisponibleTrue(nombreProducto)
            .orElseThrow(() -> new IllegalArgumentException("Producto no disponible: " + nombreProducto));

        if (p.getStock() < cantidad)
            throw new IllegalStateException("Stock insuficiente para " + nombreProducto);

        p.setStock(p.getStock() - cantidad);
        // Si llega a 0, puedes marcar no disponible automáticamente:
        if (p.getStock() == 0) p.setDisponible(false);
        // @Version evita colisiones; si hay colisión, Spring lanza OptimisticLockingFailureException.
    }
}
