package com.whatsappbot.whatsappservice.controller;

import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.whatsappbot.whatsappservice.model.ProductoStockEntity;
import com.whatsappbot.whatsappservice.repository.PedidoRepository;
import com.whatsappbot.whatsappservice.repository.ProductoStockRepository;
import com.whatsappbot.whatsappservice.service.TransbankService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor

public class CatalogoController {
private final PedidoRepository pedidoRepository;
private final TransbankService transbankService; 
private final ProductoStockRepository productoStockRepository;

   private static final String CSV_URL = "https://barlacteo-catalogo.s3.us-east-1.amazonaws.com/catalogo_fronted.csv";
@GetMapping("/api/catalogo")
public ResponseEntity<List<ProductoDTO>> obtenerCatalogo() {
    List<ProductoDTO> productos = new ArrayList<>();

    try {
        URL url = new URL(CSV_URL);
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(';') // Usa punto y coma como separador
                .build();

        try (CSVReader csvReader = new CSVReaderBuilder(
                new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))
                .withCSVParser(parser)
                .withSkipLines(1)
                .build()) {

            String[] linea;
            while ((linea = csvReader.readNext()) != null) {
                if (linea.length >= 4) {
                    String nombre = linea[0].trim();

                    // Verifica si está en la base de datos y disponible
                    boolean disponible = productoStockRepository
                        .findByNombreIgnoreCase(nombre)
    .map(ProductoStockEntity::getDisponible) // <- usar getDisponible en lugar de isDisponible
                        .orElse(false);

                    if (disponible) {
                        ProductoDTO producto = new ProductoDTO(
                            nombre,
                            linea[1].trim(), // descripción
                            linea[2].trim(), // precio
                            linea[3].trim()  // imagen
                        );
                        productos.add(producto);
                    }
                }
            }
        }

        return ResponseEntity.ok(productos);
    } catch (Exception e) {
        log.error("❌ Error al leer el catálogo desde S3", e);
        return ResponseEntity.internalServerError().build();
    }
}


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class ProductoDTO {
        private String nombre;
        private String descripcion;
        private String precio;
        private String imagen;
    }
    @Data
    public class PedidoRequestDTO {
    private String detalle;
    private double monto;
}
}
