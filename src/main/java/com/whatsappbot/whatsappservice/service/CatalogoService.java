package com.whatsappbot.whatsappservice.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.whatsappbot.whatsappservice.model.ProductoCatalogo;

@Service
public class CatalogoService {

    public List<ProductoCatalogo> obtenerCatalogo() {
        List<ProductoCatalogo> productos = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream("/catalogo_fronted.csv"),
                        StandardCharsets.UTF_8))) {

            String linea;
            boolean esPrimera = true;
            while ((linea = reader.readLine()) != null) {
                if (esPrimera) {
                    esPrimera = false; // saltar encabezado
                    continue;
                }
                String[] columnas = linea.split(",");
                if (columnas.length >= 4) {
                    String nombre = columnas[0].trim();
                    String descripcion = columnas[1].trim();
                    int precio = Integer.parseInt(columnas[2].trim());
                    String imagen = columnas[3].trim();
                    productos.add(new ProductoCatalogo(nombre, descripcion, precio, imagen));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return productos;
    }
}
