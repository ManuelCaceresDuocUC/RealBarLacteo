package com.whatsappbot.whatsappservice.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.whatsappbot.whatsappservice.model.ProductoStockEntity;
import com.whatsappbot.whatsappservice.repository.ProductoStockRepository;
import com.whatsappbot.whatsappservice.service.StockService;

@RestController
@RequestMapping("/api/stock")
public class StockController {
    private final ProductoStockRepository repo;
    private final StockService stockService;
    public StockController(ProductoStockRepository r, StockService s){ this.repo = r; this.stockService = s; }

    @GetMapping
    public List<ProductoStockEntity> listar(){ return repo.findAll(); }

    @PatchMapping("/{id}/disponible")
    public ResponseEntity<?> setDisponible(@PathVariable Long id, @RequestParam boolean value){
        int rows = repo.setDisponible(id, value);
        return rows==1 ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<?> setStock(@PathVariable Long id, @RequestParam int value){
        int rows = repo.setStock(id, Math.max(0, value));
        return rows==1 ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/comprar")
    public ResponseEntity<?> comprar(@RequestParam String nombre, @RequestParam int qty){
        stockService.disminuirPorCompra(nombre, qty);
        return ResponseEntity.ok().build();
    }
}
