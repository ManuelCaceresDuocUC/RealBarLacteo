package com.whatsappbot.whatsappservice.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.repository.PedidoRepository;
import com.whatsappbot.whatsappservice.service.ComandaService;
import com.whatsappbot.whatsappservice.service.TransbankService;

import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCommitResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class NotificacionPagoController {

    private final TransbankService transbankService;
    private final PedidoRepository pedidoRepository;
    private final ComandaService comandaService;

    @GetMapping("/webpay-redireccion")
    public ResponseEntity<String> procesarRedireccionWebpay(@RequestParam("token_ws") String token) {
        try {
            WebpayPlusTransactionCommitResponse response = transbankService.confirmarTransaccion(token);
            String buyOrder = response.getBuyOrder();

            Optional<PedidoEntity> pedidoOpt = pedidoRepository.findByPedidoId(buyOrder);
            if (pedidoOpt.isEmpty()) {
                return ResponseEntity.status(404).body("❌ Pedido no encontrado");
            }

            PedidoEntity pedido = pedidoOpt.get();
            
            // Actualizamos estado si no estaba pagado
            if (!"pagado".equalsIgnoreCase(pedido.getEstado())) {
                pedido.setEstado("pagado");
                pedidoRepository.save(pedido);
            }

            // Generar comanda (S3)
            comandaService.generarPDF(pedido);

            // LOG informativo en lugar de envío a WATI
            log.info("📥 NUEVO PEDIDO PAGADO - ID: {}, Teléfono: {}", pedido.getPedidoId(), pedido.getTelefono());

            return ResponseEntity.ok("✅ ¡Pago confirmado y comanda generada!");
        } catch (Exception e) {
            log.error("❌ Error al procesar redirección Webpay", e);
            return ResponseEntity.status(500).body("❌ Error interno: " + e.getMessage());
        }
    }
}