package com.whatsappbot.whatsappservice.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.whatsappbot.whatsappservice.service.TransbankService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoControlador {

    private final TransbankService transbankService;
    @GetMapping("/test")
public ResponseEntity<?> test() {
    return ResponseEntity.ok("✅ El backend responde correctamente.");
}
    @PostMapping("/iniciar")
    public ResponseEntity<?> iniciarPago(@RequestBody Map<String, Object> body) {
        String pedidoId = (String) body.get("pedidoId");
        int monto = ((Number) body.get("monto")).intValue();

        log.info("🧾 Recibida solicitud de pago → Pedido ID: {}, Monto: {}", pedidoId, monto);

        try {
            Map<String, String> datosPago = transbankService.generarLinkDePago(pedidoId, monto);
            log.info("✅ Link de pago generado correctamente: {}", datosPago.get("url"));
            return ResponseEntity.ok(datosPago);
        } catch (Exception e) {
            log.error("❌ Error al iniciar el pago", e);  // Este log es esencial para ver el detalle en Render
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo iniciar el pago"));
        }
    }
}
