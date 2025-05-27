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
import com.whatsappbot.whatsappservice.service.WatiService;

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
    private final WatiService watiService;

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
            pedido.setEstado("pagado");
            pedidoRepository.save(pedido);

            comandaService.generarPDF(pedido);

            String mensaje = "📥 *NUEVO PEDIDO PAGADO*\n"
                    + "🆔 ID: " + pedido.getPedidoId() + "\n"
                    + "📞 Teléfono: " + pedido.getTelefono() + "\n"
                    + "📦 Detalle: " + pedido.getDetalle();

            watiService.enviarMensajeTexto(pedido.getTelefono(), mensaje);

            return ResponseEntity.ok("✅ ¡Pago confirmado y comanda enviada!");
        } catch (Exception e) {
            log.error("❌ Error al procesar redirección Webpay", e);
            return ResponseEntity.status(500).body("❌ Error interno: " + e.getMessage());
        }
    }
}
