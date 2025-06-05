package com.whatsappbot.whatsappservice.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
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

    @Column(columnDefinition = "TEXT")
    private String indicaciones;

    @Column(length = 20)
    private String estado;

    @Column(name = "monto")
    private Double monto;

    @Column(name = "link_pago", columnDefinition = "TEXT")
    private String linkPago;

    @Column(name = "fecha_creacion", columnDefinition = "DATETIME")
    private LocalDateTime fechaCreacion;

    // 🟩 Nuevo campo para el local
    @Column(name = "local", length = 20)
    private String local;

    public PedidoEntity() {}

    public PedidoEntity(String pedidoId, String telefono, String detalle, String indicaciones) {
        this.pedidoId = pedidoId;
        this.telefono = telefono;
        this.detalle = detalle;
        this.estado = "pendiente";
        this.indicaciones = indicaciones;
    }

    @PrePersist
    protected void onCreate() {
    this.fechaCreacion = LocalDateTime.now(java.time.ZoneId.of("America/Santiago"));
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

    public String getIndicaciones() {
        return indicaciones;
    }

    public void setIndicaciones(String indicaciones) {
        this.indicaciones = indicaciones;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Double getMonto() {
        return monto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }

    public String getLinkPago() {
        return linkPago;
    }

    public void setLinkPago(String linkPago) {
        this.linkPago = linkPago;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    // 🟩 Getter y Setter para el local
    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }
}
