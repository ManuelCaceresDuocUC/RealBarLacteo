package com.whatsappbot.whatsappservice.controller;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.whatsappbot.whatsappservice.dto.PagoResponseDTO;
import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.repository.PedidoRepository;
import com.whatsappbot.whatsappservice.service.ComandaService;
import com.whatsappbot.whatsappservice.service.PedidoContextService;
import com.whatsappbot.whatsappservice.service.TransbankService;
import com.whatsappbot.whatsappservice.service.WatiService;

import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCommitResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoControlador {

    private final PedidoRepository pedidoRepository;
    private final TransbankService transbankService;
    private final WatiService watiService;
    private final ComandaService comandaService;
    private final PedidoContextService pedidoContext;

    @PostMapping
    public ResponseEntity<?> crearPedido(@RequestBody Map<String, String> payload) {
        String telefono = payload.get("telefono");
        String detalle = payload.get("detalle");

        if (telefono == null || telefono.isBlank() || detalle == null || detalle.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan datos obligatorios"));
        }

        String pedidoId = "pedido-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("📝 Recibido nuevo pedido: telefono={}, detalle={}", telefono, detalle);

        if (!telefono.startsWith("+")) telefono = "+" + telefono;

        try {
            PedidoEntity pedido = new PedidoEntity();
            pedido.setPedidoId(pedidoId);
            pedido.setTelefono(telefono);
            pedido.setDetalle(detalle);
            pedido.setIndicaciones(payload.get("indicaciones"));
            pedido.setLocal(payload.get("local"));
            pedido.setEstado("pendiente");

            double monto = Integer.parseInt(payload.get("monto"));
            pedido.setMonto(monto);

            PagoResponseDTO pago = transbankService.generarLinkDePago(pedidoId, monto);
            pedido.setLinkPago(pago.getUrl());
            pedido.setTokenWs(pago.getToken());

            pedidoRepository.save(pedido);

            Map<String, Object> respuesta = new java.util.HashMap<>();
            respuesta.put("mensaje", "Pedido creado y link enviado por WhatsApp");
            respuesta.put("pedidoId", pedidoId);
            respuesta.put("linkPago", pago.getUrl());

            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            log.error("❌ Error al crear pedido", e);
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo procesar el pedido"));
        }
    }

    @RequestMapping(value = "/confirmacion", method = {RequestMethod.GET, RequestMethod.POST})
    public String confirmarPago(@RequestParam("token_ws") String token, Model model) {
        try {
            WebpayPlusTransactionCommitResponse response = transbankService.confirmarTransaccion(token);
            String buyOrder = response.getBuyOrder();

            Optional<PedidoEntity> pedidoOpt = pedidoRepository.findByPedidoId(buyOrder);
            if (pedidoOpt.isEmpty()) {
                model.addAttribute("mensaje", "❌ No se encontró el pedido.");
                return "error";
            }

            PedidoEntity pedido = pedidoOpt.get();

            if ("AUTHORIZED".equals(response.getStatus())) {
                pedido.setEstado("pagado");
                pedidoRepository.save(pedido);

                String urlComanda = comandaService.generarPDF(pedido);
                System.out.println("🔗 URL comanda generada: " + urlComanda);
            } else {
                log.warn("⚠️ Transacción NO autorizada para token {}", token);
                model.addAttribute("mensaje", "El pago no fue autorizado.");
                return "error";
            }

            pedido = pedidoRepository.findByPedidoId(buyOrder).orElseThrow();

            String urlComanda = comandaService.generarPDF(pedido);
            System.out.println("🔗 URL comanda generada: " + urlComanda);
            System.out.println("📞 Enviando mensaje de confirmación a: " + pedido.getTelefono());

            if (urlComanda != null) {
                watiService.enviarMensajeConTemplate(pedido.getTelefono(), pedido.getPedidoId(), urlComanda);
            } else {
                log.warn("⚠️ Comanda no pudo ser subida. Se enviará confirmación sin link.");
                watiService.enviarTemplateConfirmacionSimple(pedido.getTelefono(), "Cliente");
            }

            pedidoContext.pedidoTemporalPorTelefono.remove(pedido.getTelefono());
            pedidoContext.indicacionPreguntadaPorTelefono.remove(pedido.getTelefono());
            pedidoContext.ultimoMensajeProcesadoPorNumero.remove(pedido.getTelefono());

            log.info("✅ Pago confirmado para pedido {}", buyOrder);
            return "redirect:" + urlComanda;

        } catch (Exception e) {
            log.error("❌ Error interno al confirmar pago", e);
            model.addAttribute("mensaje", "Ocurrió un error al confirmar el pago.");
            return "error";
        }
    }

    // FIX: antes tenías "/api/ultimo-pedido-id" y quedaba doble prefijo
    @GetMapping("/ultimo-pedido-id")
public ResponseEntity<?> obtenerUltimoPedidoId() {
    return pedidoRepository
        .findTopByEstadoOrderByFechaCreacionDesc("pagado")
        .map(p -> ResponseEntity.ok(Map.of("pedidoId", p.getPedidoId())))
        .orElse(ResponseEntity.notFound().build());
}

    @GetMapping
    public ResponseEntity<?> obtenerPedidosPorLocal(@RequestParam String local) {
        try {
            var pedidos = pedidoRepository.findByLocal(local);
            return ResponseEntity.ok(pedidos);
        } catch (Exception e) {
            log.error("Error al obtener pedidos por local", e);
            return ResponseEntity.status(500).body(Map.of("error", "Error interno"));
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(@PathVariable Long id) {
        return pedidoRepository.findById(id).map(pedido -> {
            String actual = pedido.getEstado();
            switch (actual) {
                case "pagado" -> pedido.setEstado("en preparación");
                case "en preparación" -> pedido.setEstado("listo");
                case "listo" -> pedido.setEstado("entregado");
                default -> pedido.setEstado(actual);
            }
            pedidoRepository.save(pedido);
            return ResponseEntity.ok(pedido);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ultimo-estado")
    public ResponseEntity<?> obtenerUltimoEstadoPedido(@RequestParam String telefono) {
        Optional<PedidoEntity> pedido = pedidoRepository
                .findTopByTelefonoOrderByFechaCreacionDesc(telefono);

        if (pedido.isEmpty()) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(Map.of(
                "estado", pedido.get().getEstado(),
                "fecha", pedido.get().getFechaCreacion(),
                "id", pedido.get().getId(),
                "detalle", pedido.get().getDetalle()
        ));
    }

    @GetMapping("/telefono")
    public ResponseEntity<?> obtenerPedidosPorTelefono(@RequestParam String numero) {
        try {
            return ResponseEntity.ok(pedidoRepository.findByTelefono(numero));
        } catch (Exception e) {
            log.error("❌ Error al obtener historial del cliente", e);
            return ResponseEntity.status(500).body(Map.of("error", "Error al buscar pedidos"));
        }
    }

    @GetMapping("/estado-actual")
    public ResponseEntity<?> obtenerUltimoEstadoPorTelefono(@RequestParam String telefono) {
        return pedidoRepository.findTopByTelefonoOrderByFechaCreacionDesc(telefono)
                .map(pedido -> ResponseEntity.ok(Map.of(
                        "estado", pedido.getEstado(),
                        "pedidoId", pedido.getPedidoId()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/estado-manual")
    public ResponseEntity<?> cambiarEstadoManual(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String nuevoEstado = payload.get("estado");
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debe proporcionar un estado válido"));
        }

        return pedidoRepository.findById(id).map(pedido -> {
            pedido.setEstado(nuevoEstado);
            pedidoRepository.save(pedido);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Estado actualizado manualmente",
                    "nuevoEstado", nuevoEstado
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    // === NUEVO: Hard delete por ID
    @DeleteMapping("/{id}")
public ResponseEntity<Void> eliminarPorId(@PathVariable Long id) {
    if (!pedidoRepository.existsById(id)) return ResponseEntity.notFound().build();
    pedidoRepository.deleteById(id);
    return ResponseEntity.noContent().build();
}

    // === NUEVO: Hard delete por pedidoId (UUID tipo "pedido-xxxx")
    @DeleteMapping("/by-pedido-id/{pedidoId}")
public ResponseEntity<Void> eliminarPorPedidoId(@PathVariable String pedidoId) {
    long n = pedidoRepository.deleteByPedidoId(pedidoId);
    return (n > 0) ? ResponseEntity.noContent().build()
                   : ResponseEntity.notFound().build();
}
}
