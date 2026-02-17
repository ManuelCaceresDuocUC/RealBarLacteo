package com.whatsappbot.whatsappservice.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.whatsappbot.whatsappservice.dto.PagoResponseDTO;
import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.repository.PedidoRepository;
import com.whatsappbot.whatsappservice.service.ComandaService;
import com.whatsappbot.whatsappservice.service.PedidoContextService;
import com.whatsappbot.whatsappservice.service.StockService;
import com.whatsappbot.whatsappservice.service.TransbankService;

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
    private final ComandaService comandaService;
    private final PedidoContextService pedidoContext;
    private final StockService stockService;

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

            return ResponseEntity.ok(Map.of(
                "mensaje", "Pedido creado",
                "pedidoId", pedidoId,
                "linkPago", pago.getUrl()
            ));
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

            if (!"AUTHORIZED".equals(response.getStatus())) {
                log.warn("⚠️ Transacción NO autorizada para token {}", token);
                model.addAttribute("mensaje", "El pago no fue autorizado.");
                return "error";
            }

            if (!"pagado".equalsIgnoreCase(pedido.getEstado())) {
                var items = parseItems(pedido.getDetalle());
                for (var e : items.entrySet()) {
                    stockService.disminuirPorCompra(e.getKey(), e.getValue());
                }
                pedido.setEstado("pagado");
                pedidoRepository.save(pedido);
            }

            String urlComanda = comandaService.generarPDF(pedido);
            log.info("🔗 URL comanda generada: {}", urlComanda);

            if (urlComanda == null) {
                log.warn("⚠️ Comanda no pudo ser generada/subida.");
            }

            pedidoContext.pedidoTemporalPorTelefono.remove(pedido.getTelefono());
            pedidoContext.indicacionPreguntadaPorTelefono.remove(pedido.getTelefono());
            pedidoContext.ultimoMensajeProcesadoPorNumero.remove(pedido.getTelefono());

            log.info("✅ Pago confirmado para pedido {}", buyOrder);
            return "redirect:" + (urlComanda != null ? urlComanda : "/");

        } catch (org.springframework.dao.OptimisticLockingFailureException e) {
            log.error("⚠️ Colisión de stock. Otro pedido tomó el stock primero.", e);
            model.addAttribute("mensaje", "Stock agotado durante el proceso.");
            return "error";
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.error("❌ Stock insuficiente o producto inválido.", e);
            model.addAttribute("mensaje", e.getMessage());
            return "error";
        } catch (Exception e) {
            log.error("❌ Error interno al confirmar pago", e);
            model.addAttribute("mensaje", "Ocurrió un error al confirmar el pago.");
            return "error";
        }
    }

    private Map<String, Integer> parseItems(String detalle) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (detalle == null || detalle.isBlank()) return map;

        String[] lines;
        if (detalle.contains(",") && !detalle.contains("\n")) {
            lines = detalle.split(",");
        } else {
            lines = detalle.split("\\r?\\n");
        }

        Pattern[] patterns = new Pattern[] {
            Pattern.compile("^(\\d+)\\s*[xX]\\s*(.+)$"),
            Pattern.compile("^(.+?)\\s*[xX]\\s*(\\d+)$"),
            Pattern.compile("^(.+?)\\s*\\((\\d+)\\)$"),
            Pattern.compile("^-?\\s*(.+?)\\s*[:=]\\s*(\\d+)$")
        };

        for (String raw : lines) {
            String s = raw.trim();
            if (s.isEmpty()) continue;
            s = s.replaceAll("\\s*\\(\\$[\\d.]+\\)", "").trim();

            boolean matched = false;
            for (Pattern p : patterns) {
                Matcher m = p.matcher(s);
                if (m.find()) {
                    String name = (p.pattern().startsWith("^(")) ? m.group(2).trim() : m.group(1).trim();
                    int qty = Integer.parseInt((p.pattern().startsWith("^(")) ? m.group(1) : m.group(2));
                    map.merge(name, qty, Integer::sum);
                    matched = true; 
                    break;
                }
            }
            if (!matched) map.merge(s, 1, Integer::sum);
        }
        return map;
    }

    @GetMapping("/ultimo-pedido-id")
    public ResponseEntity<?> obtenerUltimoPedidoId() {
        return pedidoRepository
            .findTopByEstadoOrderByFechaCreacionDesc("pagado")
            .map(p -> ResponseEntity.ok(Map.of("pedidoId", p.getPedidoId())))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<?> obtenerPedidosPorLocal(@RequestParam String local) {
        try { return ResponseEntity.ok(pedidoRepository.findByLocal(local)); }
        catch (Exception e) {
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
        Optional<PedidoEntity> pedido = pedidoRepository.findTopByTelefonoOrderByFechaCreacionDesc(telefono);
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
        try { return ResponseEntity.ok(pedidoRepository.findByTelefono(numero)); }
        catch (Exception e) {
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
            return ResponseEntity.ok(Map.of("mensaje", "Estado actualizado manualmente", "nuevoEstado", nuevoEstado));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPorId(@PathVariable Long id) {
        if (!pedidoRepository.existsById(id)) return ResponseEntity.notFound().build();
        pedidoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/by-pedido-id/{pedidoId}")
    public ResponseEntity<Void> eliminarPorPedidoId(@PathVariable String pedidoId) {
        long n = pedidoRepository.deleteByPedidoId(pedidoId);
        return (n > 0) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}