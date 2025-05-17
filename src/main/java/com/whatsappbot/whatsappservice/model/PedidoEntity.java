package com.whatsappbot.whatsappservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
@Entity
@Table(name = "pedidos")
public class PedidoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pedidoId;
    private String telefono;
    
    @Column(columnDefinition = "TEXT")
    private String detalle;
    @Column(length = 20)
    private String estado;

    public PedidoEntity() {}

public PedidoEntity(String pedidoId, String telefono, String detalle) {
    this.pedidoId = pedidoId;
    this.telefono = telefono;
    this.detalle = detalle;
    this.estado = "pendiente"; // Valor por defecto al crear
}

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(String pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
}
