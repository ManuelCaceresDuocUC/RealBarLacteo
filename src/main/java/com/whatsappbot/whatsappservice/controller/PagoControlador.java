package com.whatsappbot.whatsappservice.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.whatsappbot.whatsappservice.service.TransbankService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoControlador {

    private final TransbankService transbankService;

    @PostMapping("/iniciar")
    public ResponseEntity<?> iniciarPago(@RequestBody Map<String, Object> body) {
        String pedidoId = (String) body.get("pedidoId");
int monto = ((Number) body.get("monto")).intValue();

        try {
            String url = transbankService.generarLinkDePago(pedidoId, monto);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo iniciar el pago"));
        }
    }
}
