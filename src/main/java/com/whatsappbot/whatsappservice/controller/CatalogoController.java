package com.whatsappbot.whatsappservice.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

@RestController
@Slf4j
public class CatalogoController {

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
                        ProductoDTO producto = new ProductoDTO(
                                linea[0].trim(), // title
                                linea[1].trim(), // description
                                linea[2].trim(), // price
                                linea[3].trim()  // image_link
                        );
                        productos.add(producto);
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
}
