package com.whatsappbot.whatsappservice.model;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;

@Entity
@Table(name = "pedidos")
public class PedidoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)               // opcional: @Column(unique = true)
    private String pedidoId;

    private String telefono;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    @Column(columnDefinition = "TEXT")
    private String indicaciones;

    @Column(length = 20)
    private String estado;

    @Column(name = "monto", nullable = false)
    private double monto;

    @Column(name = "link_pago", columnDefinition = "TEXT")
    private String linkPago;

    // MySQL/MariaDB: usa TIMESTAMP (sin TZ). Quita columnDefinition para portabilidad.
    // Si quieres fijarlo: @Column(name="fecha_creacion", columnDefinition="TIMESTAMP")
    @Column(name = "fecha_creacion", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") // ej: 2025-10-19T14:22:00-03:00
    private OffsetDateTime fechaCreacion;

    @Column(name = "local", length = 20)
    private String local;

    @Column(name = "token_ws")
    private String tokenWs;

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
        if (this.fechaCreacion == null) {
            this.fechaCreacion = OffsetDateTime.now(ZoneId.of("America/Santiago"));
        }
    }

    /* ================= Getters/Setters ================ */

    public Long getId() { return id; }

    public String getPedidoId() { return pedidoId; }
    public void setPedidoId(String pedidoId) { this.pedidoId = pedidoId; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public String getIndicaciones() { return indicaciones; }
    public void setIndicaciones(String indicaciones) { this.indicaciones = indicaciones; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; } // usa primitivo para evitar NPE

    public String getLinkPago() { return linkPago; }
    public void setLinkPago(String linkPago) { this.linkPago = linkPago; }

    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(OffsetDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }

    public String getTokenWs() { return tokenWs; }
    public void setTokenWs(String tokenWs) { this.tokenWs = tokenWs; }
}
