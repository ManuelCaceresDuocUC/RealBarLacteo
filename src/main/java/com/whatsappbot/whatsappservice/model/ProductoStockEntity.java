package com.whatsappbot.whatsappservice.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "producto_stock")
public class ProductoStockEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private Boolean disponible;
    private LocalDateTime actualizado;

    @Column(nullable = false)
    private Integer stock = 0;

    @Version
    private Long version; // evita sobreventa con bloqueo optimista

    // Getters/Setters
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Boolean getDisponible() { return disponible; }
    public void setDisponible(Boolean disponible) { this.disponible = disponible; }
    public LocalDateTime getActualizado() { return actualizado; }
    public void setActualizado(LocalDateTime actualizado) { this.actualizado = actualizado; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Long getVersion() { return version; }
}
