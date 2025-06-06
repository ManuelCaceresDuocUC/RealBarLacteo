package com.whatsappbot.whatsappservice.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
    private final TransbankService transbankService;
    private final PedidoRepository pedidoRepository;
    private final ProductoStockRepository productoStockRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private final ConcurrentHashMap<String, PedidoEntity> pedidoTemporalPorTelefono = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> ultimoMensajeProcesadoPorNumero = new ConcurrentHashMap<>();

    @Value("${wati.api.key}")
    private String watiApiKey;

    @PostMapping("/wati")
    public ResponseEntity<?> recibirMensaje(@RequestBody JsonNode payload) {
        String telefono = payload.path("waId").asText();
        String texto = payload.path("text").asText("");
        String tipo = payload.path("type").asText();
        String messageId = payload.path("whatsappMessageId").asText();

        if (telefono.isBlank() || messageId.isBlank()) return ResponseEntity.ok().build();
        if (messageId.equals(ultimoMensajeProcesadoPorNumero.get(telefono))) return ResponseEntity.ok().build();

        if ("order".equalsIgnoreCase(tipo) && texto.equalsIgnoreCase("#trigger_view_cart")) {
            log.info("🟢 Trigger recibido para validar stock de {}", telefono);

            String url = "https://live-mt-server.wati.io/442590/api/v1/getMessages/" + telefono;
            var headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + watiApiKey);
            var entity = new HttpEntity<>(headers);

            JsonNode mensajes;
            List<JsonNode> mensajesOrdenados = new ArrayList<>();
            String mensajeResumen = null;

            int reintentos = 0;
            long triggerTimestamp = payload.path("timestamp").asLong(0);

            while (mensajeResumen == null && reintentos < 20) {
                try {
                    TimeUnit.SECONDS.sleep(2);
                    var response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
                    mensajes = response.getBody().path("messages").path("items");

                    if (mensajes == null || !mensajes.isArray()) {
                        reintentos++;
                        continue;
                    }

                    mensajesOrdenados.clear();
                    mensajes.forEach(mensajesOrdenados::add);
                    mensajesOrdenados.sort(Comparator.comparingLong(this::obtenerTimestamp));

                    for (JsonNode msg : mensajesOrdenados) {
                        long msgTimestamp = obtenerTimestamp(msg);
                        if (msgTimestamp <= triggerTimestamp) continue;

                        String finalText = msg.path("finalText").asText("");
                        String textMsg = msg.path("text").asText("");
                        String contenido = !finalText.isBlank() ? finalText : textMsg;
                        String contenidoLower = contenido.toLowerCase();

                        if (contenidoLower.contains("desde el carrito") && contenidoLower.contains("total estimado")) {
                            mensajeResumen = contenido;
                            break;
                        }
                    }

                    if (mensajeResumen == null) {
                        reintentos++;
                    }

                } catch (InterruptedException e) {
                    log.error("❌ Error al esperar resumen del carrito", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("❌ Error general en lectura de mensajes", e);
                    reintentos++;
                }
            }

            if (mensajeResumen == null) {
                watiService.enviarMensajeTexto(telefono, "❌ No se encontró el resumen del carrito. Intenta de nuevo.");
                return ResponseEntity.ok().build();
            }

            List<String> productos = extraerProductosDesdeDetalle(extraerDetalleFlexible(mensajeResumen));
            for (String producto : productos) {
                var stock = productoStockRepository.findByNombreIgnoreCase(producto);
                if (stock.isPresent() && !stock.get().getDisponible()) {
                    String advertencia = "❌ El producto '" + producto + "' no está disponible. Por favor edita tu pedido.";
                    watiService.enviarMensajeTexto(telefono, advertencia);
                    return ResponseEntity.ok().build();
                }
            }

            PedidoEntity pedido = new PedidoEntity();
            pedido.setPedidoId("pedido-" + UUID.randomUUID());
            pedido.setTelefono(telefono);
            pedidoTemporalPorTelefono.put(telefono, pedido);

            watiService.enviarMensajeTexto(telefono, "✅ Stock verificado\nCONTINUAR");
                watiService.enviarMensajeBotones(
                    telefono,
                    "¿Deseas agregar una indicación especial al pedido?",
                    "Puedes personalizarlo",
                    "",
                    List.of("Sí", "No")
                );
            

            return ResponseEntity.ok().build();
        
    }

        // ✅ Paso 2: Botón "No" => usuario elige local
        if ("interactive".equalsIgnoreCase(tipo)) {
            JsonNode btn = payload.path("interactiveButtonReply");
            String title = btn.path("title").asText("").toLowerCase();

            if (title.equals("hyatt") || title.equals("charles")) {
                String localNormalizado = title.equals("hyatt") ? "HYATT" : "CHARLES";
                log.info("✅ Local {} asignado al pedido temporal de {}", localNormalizado, telefono);

                PedidoEntity pedido = pedidoTemporalPorTelefono.get(telefono);
                if (pedido == null) {
                    watiService.enviarMensajeTexto(telefono, "⚠️ No se encontró un pedido pendiente en memoria. Intenta de nuevo desde el carrito.");
                    return ResponseEntity.ok().build();
                }

                pedido.setLocal(localNormalizado);

                // Esperar mensaje resumen después del local
                String url = "https://live-mt-server.wati.io/442590/api/v1/getMessages/" + telefono;
                var headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + watiApiKey);
                var entity = new HttpEntity<>(headers);

                JsonNode mensajes;
                List<JsonNode> mensajesOrdenados = new ArrayList<>();
                String mensajeResumen = null;
                String indicacion = pedido.getIndicaciones();

                long triggerTimestamp = payload.path("timestamp").asLong(0);
                int reintentos = 0;

                while (mensajeResumen == null && reintentos < 15) {
                    try {
                        var response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
                        mensajes = response.getBody().path("messages").path("items");

                        if (mensajes == null || !mensajes.isArray()) {
                            TimeUnit.SECONDS.sleep(2);
                            reintentos++;
                            continue;
                        }

                        mensajesOrdenados.clear();
                        mensajes.forEach(mensajesOrdenados::add);
                        mensajesOrdenados.sort(Comparator.comparingLong(this::obtenerTimestamp));

                        for (JsonNode msg : mensajesOrdenados) {
                            long msgTimestamp = obtenerTimestamp(msg);
                            if (msgTimestamp <= triggerTimestamp) continue;

                            String finalText = msg.path("finalText").asText("");
                            String textMsg = msg.path("text").asText("");
                            String contenido = !finalText.isBlank() ? finalText : textMsg;
                            String contenidoLower = contenido.toLowerCase();

                            if (contenidoLower.contains("desde el carrito") && contenidoLower.contains("total estimado")) {
                                mensajeResumen = contenido;
                            }
                            if (contenidoLower.startsWith("indicacion:") && indicacion == null) {
                                indicacion = contenido.substring(contenido.indexOf(":") + 1).trim();
                            }
                        }

                        if (mensajeResumen == null) {
                           try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("⏸️ Sleep interrumpido", e);
                }
                reintentos++;
            }
        } catch (Exception e) {
            log.error("❌ Error obteniendo mensaje resumen", e);
            reintentos++;
        }
                }

                String detalle = extraerDetalleFlexible(mensajeResumen);
                int monto = extraerMontoFlexible(mensajeResumen);
                if (detalle == null || monto <= 0) {
                    watiService.enviarMensajeTexto(telefono, "❌ No se pudo procesar el pedido. Intenta nuevamente desde el carrito.");
                    return ResponseEntity.ok().build();
                }

                pedido.setDetalle(detalle);
                pedido.setIndicaciones(indicacion);
                pedido.setMonto((double) monto);
                pedido.setEstado("pendiente");
                pedidoRepository.save(pedido);

                PagoResponseDTO pago = transbankService.generarLinkDePago(pedido.getPedidoId(), monto);
                pedido.setLinkPago(pago.getUrl());
                pedidoRepository.save(pedido);

                try {
                    watiService.enviarMensajePagoEstatico(telefono, pedido.getMonto(), pago.getUrl());
                } catch (Exception e) {
                    log.error("❌ Error enviando plantilla de pago", e);
                }

                pedidoTemporalPorTelefono.remove(telefono);
                ultimoMensajeProcesadoPorNumero.put(telefono, messageId);
                return ResponseEntity.ok().build();
            }
        }

        return ResponseEntity.ok().build();
    }

    // Métodos auxiliares
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
            return null;
        }
    }

    private List<String> extraerProductosDesdeDetalle(String detalle) {
        List<String> productos = new ArrayList<>();
        if (detalle == null) return productos;
        for (String linea : detalle.split("\n")) {
            if (linea.contains("×")) {
                String[] partes = linea.split("×");
                if (partes.length == 2) {
                    productos.add(partes[1].trim().toLowerCase());
                }
            }
        }
        return productos;
    }

    private int extraerMontoFlexible(String texto) {
        try {
            int inicio = texto.indexOf("{\"Total\":");
            int fin = texto.indexOf("}", inicio);
            if (inicio != -1 && fin != -1) {
                String jsonStr = texto.substring(inicio, fin + 1);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(jsonStr);
                return (int) node.path("Total").asDouble(0);
            }
        } catch (Exception e) {
            log.error("❌ Error extrayendo el monto", e);
        }
        return -1;
    }

    private long obtenerTimestamp(JsonNode msg) {
        if (msg.has("timestamp")) return msg.path("timestamp").asLong(0);
        if (msg.has("created")) {
            try {
                return java.time.Instant.parse(msg.path("created").asText(""))
                        .getEpochSecond();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }
}
