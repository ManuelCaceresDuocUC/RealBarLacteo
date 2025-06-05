// WebhookController actualizado y ordenado con comentarios
package com.whatsappbot.whatsappservice.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsappbot.whatsappservice.dto.PagoResponseDTO;
import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.repository.PedidoRepository;
import com.whatsappbot.whatsappservice.repository.ProductoStockRepository;
import com.whatsappbot.whatsappservice.service.ComandaService;
import com.whatsappbot.whatsappservice.service.TransbankService;
import com.whatsappbot.whatsappservice.service.WatiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WatiService watiService;
    private final PedidoRepository pedidoRepository;
    private final TransbankService transbankService;
    private final ComandaService comandaService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ProductoStockRepository productoStockRepository;

    // Control de concurrencia y almacenamiento temporal por teléfono
    private static final ConcurrentHashMap<String, Boolean> procesamientoEnCurso = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> ultimoMensajeProcesadoPorNumero = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, PedidoEntity> pedidoTemporalPorTelefono = new ConcurrentHashMap<>();

    @PostMapping("/wati")
    public ResponseEntity<?> recibirMensaje(@RequestBody JsonNode payload) {
        log.info("\uD83D\uDCE5 Payload recibido: {}", payload.toPrettyString());

        try {
            String tipo = payload.path("type").asText("");
            String texto = payload.path("text").asText().toLowerCase();
            String telefono = payload.path("waId").asText("");
            String nombre = payload.path("senderName").asText("Cliente");
            String messageId = payload.path("whatsappMessageId").asText("");

            if (telefono.isEmpty() || messageId.isEmpty()) return ResponseEntity.ok().build();
            if (messageId.equals(ultimoMensajeProcesadoPorNumero.get(telefono))) return ResponseEntity.ok().build();
            if (procesamientoEnCurso.putIfAbsent(telefono, true) != null) return ResponseEntity.ok().build();

            try {
                // 1. AYUDA: Si el usuario escribe 'ayuda'
                if ("text".equalsIgnoreCase(tipo) && texto.contains("ayuda")) {
                    watiService.enviarTemplateAyuda(telefono, nombre);
                    return ResponseEntity.ok().build();
                }

                // 2. LOCAL desde botón interactivo con "interactiveButtonReply"
                if ("interactive".equalsIgnoreCase(tipo) && payload.has("interactiveButtonReply")) {
                    String seleccion = payload.path("interactiveButtonReply").path("title").asText("").toLowerCase();

                    String local = null;
                    if (seleccion.contains("hyatt")) local = "HYATT";
                    else if (seleccion.contains("charles")) local = "CHARLES";

                    if (local != null) {
                        PedidoEntity pedido = pedidoTemporalPorTelefono.get(telefono);
                        if (pedido != null && pedido.getLocal() == null) {
                            pedido.setLocal(local);
                            log.info("✅ Local {} asignado al pedido temporal de {}", local, telefono);
                        }
                    }
                    ultimoMensajeProcesadoPorNumero.put(telefono, messageId);
                    return ResponseEntity.ok().build();
                }

                // 3. INDICACION personalizada
                if ("text".equalsIgnoreCase(tipo) && texto.startsWith("indicacion:")) {
                    PedidoEntity pedido = pedidoTemporalPorTelefono.get(telefono);
                    if (pedido != null && pedido.getIndicaciones() == null) {
                        pedido.setIndicaciones(texto.substring(11).trim());
                        log.info("✍️ Indicacion guardada para {}", telefono);
                    }
                    ultimoMensajeProcesadoPorNumero.put(telefono, messageId);
                    return ResponseEntity.ok().build();
                }

                // 4. MENSAJE RESUMEN: Detecta el mensaje con resumen desde el carrito
                if ("text".equalsIgnoreCase(tipo) && texto.contains("desde el carrito") && texto.contains("total estimado")) {
                    // Prevenir duplicados
                    List<PedidoEntity> recientes = pedidoRepository.findByTelefonoAndEstadoAndFechaCreacionAfter(
                        telefono, "pendiente", LocalDateTime.now().minusMinutes(5));
                    if (!recientes.isEmpty()) return ResponseEntity.ok().build();

                    // Extraer detalle y monto
                    String detalle = extraerDetalleFlexible(texto);
                    int monto = extraerMontoFlexible(texto);
                    if (detalle == null || monto <= 0) return ResponseEntity.ok().build();

                    // Verificar stock
                    List<String> productos = extraerProductosDesdeDetalle(detalle);
                    for (String producto : productos) {
                        var stock = productoStockRepository.findByNombreIgnoreCase(producto);
                        if (stock.isPresent() && !stock.get().getDisponible()) {
                            watiService.enviarMensajeTexto(telefono, "❌ El producto '" + producto + "' no está disponible. Edita tu pedido.");
                            return ResponseEntity.ok().build();
                        }
                    }

                    // Recuperar el local y la indicación desde el pedido temporal
                    PedidoEntity temp = pedidoTemporalPorTelefono.remove(telefono);
                    if (temp == null || temp.getLocal() == null) {
                        watiService.enviarMensajeTexto(telefono, "⚠️ Debes seleccionar el local antes de confirmar el pedido.");
                        return ResponseEntity.ok().build();
                    }

                    // Crear y guardar el pedido
                    String pedidoId = "pedido-" + UUID.randomUUID().toString().substring(0, 8);
                    temp.setPedidoId(pedidoId);
                    temp.setDetalle(detalle);
                    temp.setMonto((double) monto);
                    temp.setEstado("pendiente");
                    temp.setFechaCreacion(LocalDateTime.now(ZoneId.of("America/Santiago")));
                    pedidoRepository.save(temp);

                    // Enviar link de pago
                    PagoResponseDTO pago = transbankService.generarLinkDePago(pedidoId, monto);
                    temp.setLinkPago(pago.getUrl());
                    pedidoRepository.save(temp);

                    watiService.enviarMensajePagoEstatico(telefono, (double) monto, pago.getUrl());
                    log.info("✅ Pedido {} registrado y link enviado a {}", pedidoId, telefono);

                    ultimoMensajeProcesadoPorNumero.put(telefono, messageId);
                    return ResponseEntity.ok().build();
                }

                // 5. TRIGGER inicial
                if ("order".equalsIgnoreCase(tipo) && texto.contains("#trigger_view_cart")) {
                    pedidoTemporalPorTelefono.put(telefono, new PedidoEntity());
                    log.info("🚀 Trigger recibido. Se inició el pedido temporal para {}", telefono);
                    ultimoMensajeProcesadoPorNumero.put(telefono, messageId);
                    return ResponseEntity.ok().build();
                }

            } finally {
                procesamientoEnCurso.remove(telefono);
            }

        } catch (Exception e) {
            log.error("❌ Error procesando webhook", e);
        }

        return ResponseEntity.ok().build();
    }

    // 🔧 Utilidades para extraer datos del texto
    private String extraerDetalleFlexible(String texto) {
        try {
            StringBuilder sb = new StringBuilder();
            for (String linea : texto.split("\n")) {
                if (linea.contains("×")) sb.append(linea.trim()).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return null;
        }
    }

    private int extraerMontoFlexible(String texto) {
        try {
            int inicio = texto.indexOf("{\"Total\":");
            int fin = texto.indexOf("}", inicio);
            if (inicio != -1 && fin != -1) {
                String jsonStr = texto.substring(inicio, fin + 1);
                JsonNode node = new ObjectMapper().readTree(jsonStr);
                return (int) node.path("Total").asDouble(0);
            }
        } catch (Exception e) {
            log.error("❌ Error extrayendo el monto", e);
        }
        return -1;
    }

    private List<String> extraerProductosDesdeDetalle(String detalle) {
        List<String> productos = new ArrayList<>();
        for (String linea : detalle.split("\n")) {
            if (linea.contains("×")) {
                String[] partes = linea.split("×");
                if (partes.length == 2) productos.add(partes[1].trim().toLowerCase());
            }
        }
        return productos;
    }
} 
