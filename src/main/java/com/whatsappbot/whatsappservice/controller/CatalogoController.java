package com.whatsappbot.whatsappservice.controller;

import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
@CrossOrigin(origins = {"https://realbarlacteo-1.onrender.com", "http://localhost:3000"})
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
            CSVParser parser = new CSVParserBuilder().withSeparator(';').build();

            try (CSVReader csvReader = new CSVReaderBuilder(
                    new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))
                    .withCSVParser(parser)
                    .withSkipLines(1) // salta encabezado
                    .build()) {

                String[] linea;
                while ((linea = csvReader.readNext()) != null) {
                    // nombre;descripcion;precio;imagen;categoria (mínimo 5)
                    if (linea.length >= 5) {
                        String nombre = linea[0].trim();

                        // visible solo si está disponible y con stock>0
                        boolean visible = productoStockRepository
                                .findByNombreIgnoreCase(nombre)
                                .map(p -> Boolean.TRUE.equals(p.getDisponible())
                                        && p.getStock() != null
                                        && p.getStock() > 0)
                                .orElse(false);

                        if (visible) {
                            productos.add(new ProductoDTO(
                                    nombre,
                                    linea[1].trim(), // descripción
                                    linea[2].trim(), // precio (string, el front lo normaliza)
                                    linea[3].trim(), // imagen
                                    linea[4].trim()  // categoría
                            ));
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
        private String precio;   // llega como string, el front lo normaliza a número
        private String imagen;
        private String categoria;
    }
}
