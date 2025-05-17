package com.whatsappbot.whatsappservice.model;

public class Pedido {
    private String id;
    private String telefono;
    private String detalle;

    public Pedido(String id, String telefono, String detalle) {
        this.id = id;
        this.telefono = telefono;
        this.detalle = detalle;
    }

    public String getId() {
        return id;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getDetalle() {
        return detalle;
    }
}
