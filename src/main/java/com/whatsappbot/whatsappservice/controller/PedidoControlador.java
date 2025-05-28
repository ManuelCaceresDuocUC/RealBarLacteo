package com.whatsappbot.whatsappservice.controller;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.whatsappbot.whatsappservice.dto.PagoResponseDTO;
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
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoControlador {

    private final PedidoRepository pedidoRepository;
    private final TransbankService transbankService;
    private final WatiService watiService;
    private final ComandaService comandaService;

    @PostMapping
    public ResponseEntity<?> crearPedido(@RequestBody Map<String, String> payload) {
        String telefono = payload.get("telefono");
        String detalle = payload.get("detalle");

        if (telefono == null || telefono.isBlank() || detalle == null || detalle.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan datos obligatorios"));
        }

        String pedidoId = "pedido-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("📝 Recibido nuevo pedido: telefono={}, detalle={}", telefono, detalle);
        if (!telefono.startsWith("+")) {
            telefono = "+" + telefono;
                }
        try {
            PedidoEntity pedido = new PedidoEntity(pedidoId, telefono, detalle);
            pedidoRepository.save(pedido);

            PagoResponseDTO pago = transbankService.generarLinkDePago(pedidoId, 1000);
            String link = pago.getUrl();

String urlComanda = comandaService.generarPDF(pedido);
watiService.enviarMensajeConTemplate(pedido.getTelefono(), pedido.getPedidoId(), urlComanda);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Pedido creado y link enviado por WhatsApp",
                "pedidoId", pedidoId,
                "linkPago", link
            ));
        } catch (Exception e) {
            log.error("❌ Error al crear pedido", e);
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo procesar el pedido"));
        }
    }
@RequestMapping(value = "/confirmacion", method = {RequestMethod.GET, RequestMethod.POST})
public ResponseEntity<String> confirmarPago(@RequestParam("token_ws") String token) {
    try {
        WebpayPlusTransactionCommitResponse response = transbankService.confirmarTransaccion(token);
        String buyOrder = response.getBuyOrder();

        Optional<PedidoEntity> pedidoOpt = pedidoRepository.findByPedidoId(buyOrder);
        if (pedidoOpt.isPresent()) {
            PedidoEntity pedido = pedidoOpt.get();
            pedido.setEstado("pagado");
            pedidoRepository.save(pedido);

            // ✅ Generar PDF de la comanda
            
String urlComanda = comandaService.generarPDF(pedido);
System.out.println("🔗 URL comanda generada: " + urlComanda);
System.out.println("📞 Enviando mensaje de confirmación a: " + pedido.getTelefono());

            // ✅ Enviar plantilla simple de confirmación por WhatsApp
if (urlComanda != null) {
    watiService.enviarMensajeConTemplate(pedido.getTelefono(), pedido.getPedidoId(), urlComanda);
} else {
    log.warn("⚠️ Comanda no pudo ser subida. Se enviará confirmación sin link.");
    watiService.enviarTemplateConfirmacionSimple(pedido.getTelefono(), "Cliente");
}
            log.info("✅ Pago confirmado para pedido {}", buyOrder);
            return ResponseEntity.ok("✅ Pago confirmado, comanda generada y mensaje enviado.");
        } else {
            return ResponseEntity.status(404).body("❌ Pedido no encontrado");
        }
    } catch (Exception e) {
        log.error("❌ Error interno al confirmar pago", e);
        return ResponseEntity.status(500).body("❌ Error interno: " + e.getMessage());
    }
}


/*@GetMapping("/webpay-redireccion")
public ResponseEntity<String> redirigirAWebpay(@RequestParam("token_ws") String token) {
    String html = """
        <html>
        <head><title>Redireccionando a WebPay...</title></head>
        <body onload="document.forms[0].submit()">
            <form method="POST" action="https://webpay3gint.transbank.cl/webpayserver/initTransaction">
                <input type="hidden" name="token_ws" value="%s" />
                <noscript>
                    <p>Tu navegador no soporta redirección automática. Haz clic en el botón.</p>
                    <button type="submit">Ir a WebPay</button>
                </noscript>
            </form>
        </body>
        </html>
        """.formatted(token);

    return ResponseEntity.ok()
            .header("Content-Type", "text/html")
            .body(html);
}*/
}
