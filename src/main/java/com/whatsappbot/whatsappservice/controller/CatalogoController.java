package com.whatsappbot.whatsappservice.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import com.whatsappbot.whatsappservice.repository.PedidoRepository;
import com.whatsappbot.whatsappservice.service.TransbankService;

import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.whatsappbot.whatsappservice.dto.PagoResponseDTO;
import com.whatsappbot.whatsappservice.model.PedidoEntity;

@RestController
@Slf4j
@RequiredArgsConstructor

public class CatalogoController {
private final PedidoRepository pedidoRepository;
private final TransbankService transbankService; 
   private static final String CSV_URL = "https://barlacteo-catalogo.s3.us-east-1.amazonaws.com/catalogo_fronted.csv";
@PostMapping("/autoservicio/pedido")
public ResponseEntity<?> crearPedidoAutoservicio(@RequestBody PedidoRequestDTO request) {
    log.info("📥 Pedido recibido desde autoservicio: {}", request);

    try {
        // Crear pedido
        PedidoEntity pedido = new PedidoEntity();
        pedido.setTelefono("autoservicio");
        pedido.setDetalle(request.getDetalle());
        pedido.setEstado("pendiente");
        pedido.setMonto(request.getMonto());
        pedido.setPedidoId("pedido-" + UUID.randomUUID());
        pedido.setFechaCreacion(LocalDateTime.now());

        pedidoRepository.save(pedido);

        // Generar link de pago
        PagoResponseDTO pago = transbankService.generarLinkDePago(pedido.getPedidoId(), request.getMonto());
        pedido.setLinkPago(pago.getUrl());
        pedido.setTokenWs(pago.getToken());

        pedidoRepository.save(pedido);
        return ResponseEntity.ok(pago.getUrl());
    } catch (Exception e) {
        log.error("❌ Error en autoservicio: {}", e.getMessage());
        return ResponseEntity.internalServerError().body("Error al generar el pedido");
    }
}
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
    @Data
    public class PedidoRequestDTO {
    private String detalle;
    private int monto;
}
}
