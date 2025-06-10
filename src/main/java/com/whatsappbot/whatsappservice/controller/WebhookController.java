package com.whatsappbot.whatsappservice.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.repository.PedidoRepository;
import com.whatsappbot.whatsappservice.repository.ProductoStockRepository;
import com.whatsappbot.whatsappservice.service.PedidoContextService;
import com.whatsappbot.whatsappservice.service.TransbankService;
import com.whatsappbot.whatsappservice.service.WatiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final PedidoContextService pedidoContext;
    private final WatiService watiService;
    private final TransbankService transbankService;
    private final PedidoRepository pedidoRepository;
    private final ProductoStockRepository productoStockRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${wati.api.key}")
    private String watiApiKey;

    private final Map<String, LocalDateTime> pedidosFinalizados = new ConcurrentHashMap<>();
    private final Map<String, Long> timestampUltimoResumen = new ConcurrentHashMap<>();

    @PostMapping("/wati")
    public ResponseEntity<?> recibirMensaje(@RequestBody JsonNode payload) {
        String telefono = payload.path("waId").asText();
        String texto = payload.path("text").asText("");
        String tipo = payload.path("type").asText();
        String messageId = payload.path("whatsappMessageId").asText();

        if (telefono.isBlank() || messageId.isBlank()) return ResponseEntity.ok().build();
        if (messageId.equals(pedidoContext.ultimoMensajeProcesadoPorNumero.get(telefono))) return ResponseEntity.ok().build();

        if (pedidosFinalizados.containsKey(telefono)) {
            LocalDateTime fin = pedidosFinalizados.get(telefono);
            if (fin.plusMinutes(2).isAfter(LocalDateTime.now())) {
                log.info("📦 Pedido ya finalizado recientemente para {}, ignorando mensaje", telefono);
                return ResponseEntity.ok().build();
            } else {
                pedidosFinalizados.remove(telefono);
                timestampUltimoResumen.remove(telefono);
            }
        }

        if ("order".equalsIgnoreCase(tipo) && texto.equalsIgnoreCase("#trigger_view_cart")) {
            if (pedidoContext.pedidoTemporalPorTelefono.containsKey(telefono)) {
                log.info("🔁 Trigger ignorado: ya hay un pedido pendiente en memoria para {}", telefono);
                return ResponseEntity.ok().build();
            }

            log.info("🟢 Trigger recibido para validar stock de {}", telefono);

            String url = "https://live-mt-server.wati.io/442590/api/v1/getMessages/" + telefono;
            var headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + watiApiKey);
            var entity = new HttpEntity<>(headers);

            JsonNode mensajes;
            List<JsonNode> mensajesOrdenados = new ArrayList<>();
            String mensajeCarrito = null;
            int reintentos = 0;
            long triggerTimestamp = payload.path("timestamp").asLong(0);

            while (mensajeCarrito == null && reintentos < 20) {
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

                        if (contenidoLower.contains("items del carrito:")) {
                            mensajeCarrito = contenido;
                            break;
                        }
                    }

                    if (mensajeCarrito == null) reintentos++;

                } catch (Exception e) {
                    log.error("❌ Error obteniendo mensaje del carrito", e);
                    reintentos++;
                }
            }

            if (mensajeCarrito == null) {
                watiService.enviarMensajeTexto(telefono, "❌ No se encontró el mensaje con los items del carrito. Intenta de nuevo.");
                log.warn("⚠️ Carrito no encontrado para {}. Trigger cancelado.", telefono);
                return ResponseEntity.ok().build();
            }

            String detalle = extraerDetalleFlexible(mensajeCarrito);
            List<String> productos = extraerProductosDesdeDetalle(detalle);

            if (productos.isEmpty()) {
                watiService.enviarMensajeTexto(telefono, "❌ No se detectaron productos válidos en tu carrito. Intenta nuevamente.");
                log.warn("⚠️ Carrito sin productos válidos para {}. Trigger cancelado.", telefono);
                return ResponseEntity.ok().build();
            }

            for (String producto : productos) {
                var stock = productoStockRepository.findByNombreIgnoreCase(producto);
                if (stock.isEmpty() || !stock.get().getDisponible()) {
                    String advertencia = "❌ El producto '" + producto + "' no está disponible. Por favor edita tu pedido.";
                    watiService.enviarMensajeTexto(telefono, advertencia);
                    log.warn("🚫 Producto sin stock '{}' detectado en carrito de {}. Pedido descartado.", producto, telefono);

                    pedidoContext.pedidoTemporalPorTelefono.remove(telefono);
                    pedidoContext.indicacionPreguntadaPorTelefono.remove(telefono);

                    return ResponseEntity.ok().build();
                }
            }

            PedidoEntity pedido = new PedidoEntity();
            String pedidoId = "pedido-" + UUID.randomUUID().toString().substring(0, 18);
            pedido.setPedidoId(pedidoId);
            pedido.setTelefono(telefono);
            pedido.setDetalle(detalle);
            pedidoContext.pedidoTemporalPorTelefono.put(telefono, pedido);

            watiService.enviarMensajeTexto(telefono, "✅ Stock verificado\nCONTINUAR");
            watiService.enviarMensajeBotones(
                telefono,
                "¿Deseas agregar una indicación especial al pedido?",
                "Puedes personalizarlo",
                "",
                List.of("Sí", "No")
            );
            pedidoContext.indicacionPreguntadaPorTelefono.put(telefono, true);

            log.info("✅ Pedido válido inicializado en memoria para {} con ID {}", telefono, pedidoId);

            return ResponseEntity.ok().build();
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
                return java.time.Instant.parse(msg.path("created").asText("")).getEpochSecond();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }
}
