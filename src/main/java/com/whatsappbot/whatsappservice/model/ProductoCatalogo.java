package com.whatsappbot.whatsappservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoCatalogo {
    private String nombre;
    private String descripcion;
    private int precio;
    private String imagen;
}
