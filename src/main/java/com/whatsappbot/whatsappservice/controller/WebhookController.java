package com.whatsappbot.whatsappservice.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
import com.whatsappbot.whatsappservice.service.ComandaService;
import com.whatsappbot.whatsappservice.service.TransbankService;
import com.whatsappbot.whatsappservice.service.WatiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WatiService watiService;
    private final PedidoRepository pedidoRepository;
    private final TransbankService transbankService;
    private final ComandaService comandaService;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final ConcurrentHashMap<String, Boolean> procesamientoEnCurso = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> ultimoMensajeProcesadoPorNumero = new ConcurrentHashMap<>();

    @PostMapping("/wati")
    public ResponseEntity<?> recibirMensaje(@RequestBody JsonNode payload) {
        log.info("📥 Payload recibido: {}", payload.toPrettyString());

        try {
            String tipo = payload.path("type").asText("");
            String telefono = payload.path("waId").asText("");
            String nombre = payload.path("senderName").asText("Cliente");
            String texto = payload.path("text").asText().toLowerCase();
            String messageId = payload.path("whatsappMessageId").asText("");

            if (telefono.isEmpty()) {
                log.warn("❌ Número de teléfono no encontrado en el payload");
                return ResponseEntity.ok().build();
            }

            // Evitar reprocesar el mismo mensaje
            if (messageId.isEmpty() || messageId.equals(ultimoMensajeProcesadoPorNumero.get(telefono))) {
                log.warn("⏳ Mensaje duplicado detectado para {}. Ignorando.", telefono);
                return ResponseEntity.ok().build();
            }

            if (procesamientoEnCurso.putIfAbsent(telefono, true) != null) {
                log.warn("⏳ Ya hay un proceso en curso para {}. Ignorando trigger duplicado.", telefono);
                return ResponseEntity.ok().build();
            }

            try {
                if ("text".equalsIgnoreCase(tipo) && texto.contains("ayuda")) {
                    watiService.enviarTemplateAyuda(telefono, nombre);
                    return ResponseEntity.ok().build();
                }

                if ("order".equalsIgnoreCase(tipo) && texto.contains("#trigger_view_cart")) {
                    log.info("🔍 Trigger de carrito detectado para {}", telefono);

                    String url = "https://live-mt-server.wati.io/442590/api/v1/getMessages/" + telefono;
                    var headers = new org.springframework.http.HttpHeaders();
                    headers.set("Authorization", "Bearer " + System.getenv("WATI_API_KEY"));
                    var entity = new org.springframework.http.HttpEntity<>(headers);

                    String mensajeResumen = null;
                    int intentos = 6;

                    for (int intento = 1; intento <= intentos; intento++) {
                        try {
                            var response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, JsonNode.class);
                            JsonNode mensajes = response.getBody().path("messages").path("items");

                            if (mensajes == null || !mensajes.isArray()) {
                                log.warn("❌ No se pudo obtener historial de mensajes o no hay mensajes");
                                return ResponseEntity.ok().build();
                            }

                            long triggerTimestamp = payload.path("timestamp").asLong(0);
                            List<JsonNode> mensajesOrdenados = new ArrayList<>();
                            mensajes.forEach(mensajesOrdenados::add);
                            mensajesOrdenados.sort(Comparator.comparingLong(this::obtenerTimestamp));

                            for (JsonNode msg : mensajesOrdenados) {
                                long msgTimestamp = obtenerTimestamp(msg);
                                if (msgTimestamp <= triggerTimestamp) continue;

                                String finalText = msg.path("finalText").asText("");
                                String text = msg.path("text").asText("");
                                String contenido = !finalText.isBlank() ? finalText : text;
                                log.info("📩 Revisando mensaje con timestamp {} -> contenido: {}", msgTimestamp, contenido);

                                String contenidoNormalizado = contenido.toLowerCase();
                                if (contenidoNormalizado.contains("desde el carrito") && contenidoNormalizado.contains("total estimado")) {
                                    log.info("✅ Mensaje resumen encontrado: {}", contenido);
                                    mensajeResumen = contenido;
                                    break;
                                }
                            }

                            if (mensajeResumen != null) {
                                break;
                            } else {
                                log.info("⏳ Intento {}: mensaje de resumen no encontrado, esperando 2 segundos...", intento);
                                TimeUnit.SECONDS.sleep(2);
                            }

                        } catch (Exception e) {
                            log.error("❌ Error durante intento {} al obtener mensajes", intento, e);
                        }
                    }

                    if (mensajeResumen == null) {
                        log.warn("⚠️ No se encontró mensaje de resumen posterior al trigger después de varios intentos");
                        return ResponseEntity.ok().build();
                    }

                    List<PedidoEntity> recientes = pedidoRepository.findByTelefonoAndEstadoAndFechaCreacionAfter(
                            telefono, "pendiente", LocalDateTime.now().minusMinutes(5));
                    if (!recientes.isEmpty()) {
                        log.warn("⏳ Ya existe un pedido reciente para {}", telefono);
                        return ResponseEntity.ok().build();
                    }

                    String detalle = extraerDetalleFlexible(mensajeResumen);
                    int monto = extraerMontoFlexible(mensajeResumen);

                    if (detalle == null || monto <= 0) {
                        log.warn("❌ No se pudo extraer el detalle o monto válido del mensaje. Monto: {}", monto);
                        return ResponseEntity.ok().build();
                    }

                    String pedidoId = "pedido-" + UUID.randomUUID().toString().substring(0, 8);
                    PedidoEntity pedido = new PedidoEntity();
                    pedido.setPedidoId(pedidoId);
                    pedido.setTelefono(telefono);
                    pedido.setDetalle(detalle);
                    pedido.setEstado("pendiente");
                    pedido.setMonto((double) monto);
                    pedido.setFechaCreacion(LocalDateTime.now(ZoneId.of("America/Santiago")));

                    pedidoRepository.save(pedido);
                    log.info("📝 Pedido guardado: {}", pedidoId);

                    PagoResponseDTO pago = transbankService.generarLinkDePago(pedidoId, monto);
                    pedido.setLinkPago(pago.getUrl());
                    pedidoRepository.save(pedido);

                    String urlComanda = comandaService.generarPDF(pedido);
                    log.info("📄 Comanda PDF generada: {}", urlComanda);

                    watiService.enviarMensajePagoEstatico(telefono, (double) monto, pago.getUrl());

                    // ✅ Guardar el último mensaje procesado
                    ultimoMensajeProcesadoPorNumero.put(telefono, messageId);
                }
            } finally {
                procesamientoEnCurso.remove(telefono);
            }

        } catch (Exception e) {
            log.error("❌ Error procesando webhook", e);
        }

        return ResponseEntity.ok().build();
    }

    private String extraerDetalleFlexible(String texto) {
        try {
            String[] lineas = texto.split("\n");
            StringBuilder sb = new StringBuilder();
            for (String linea : lineas) {
                if (linea.contains("×")) {
                    sb.append(linea.trim()).append("\n");
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("❌ Error extrayendo detalle", e);
            return null;
        }
    }

    private int extraerMontoFlexible(String texto) {
        try {
            int inicio = texto.indexOf("{\"Total\":");
            int fin = texto.indexOf("}", inicio);
            if (inicio != -1 && fin != -1) {
                String jsonStr = texto.substring(inicio, fin + 1);
                log.info("🧾 JSON de monto extraído: {}", jsonStr);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(jsonStr);
                int monto = (int) node.path("Total").asDouble(0);
                log.info("💵 Monto extraído: {}", monto);
                return monto;
            }
        } catch (Exception e) {
            log.error("❌ Error extrayendo el monto", e);
        }
        return -1;
    }

    private long obtenerTimestamp(JsonNode msg) {
        if (msg.has("timestamp")) {
            return msg.path("timestamp").asLong(0);
        }
        if (msg.has("created")) {
            String created = msg.path("created").asText("");
            try {
                return java.time.Instant.parse(created).getEpochSecond();
            } catch (Exception e) {
                log.warn("❌ No se pudo parsear timestamp desde 'created': {}", created);
            }
        }
        return 0;
    }
}
