package com.whatsappbot.whatsappservice.controller;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/wati")
    public ResponseEntity<?> recibirMensaje(@RequestBody JsonNode payload) {
        log.info("📥 Payload recibido: {}", payload.toPrettyString());

        try {
            String tipo = payload.path("type").asText("");
            String telefono = payload.path("waId").asText("");
            String nombre = payload.path("senderName").asText("Cliente");

            if ("text".equalsIgnoreCase(tipo)) {
                String texto = payload.path("text").asText("").toLowerCase();
                log.info("📥 Mensaje de texto: {} desde {}", texto, telefono);

                if (texto.contains("ayuda")) {
                    watiService.enviarTemplateAyuda(telefono, nombre);
                }

                if (texto.contains("confirmar pedido")) {
                    String url = "https://live-server.wati.io/api/v1/getMessages?waId=" + telefono;
                    var headers = new org.springframework.http.HttpHeaders();
                    String apiKey = System.getenv("WATI_API_KEY");
                    headers.set("Authorization", "Bearer " + apiKey);
                    var entity = new org.springframework.http.HttpEntity<>(headers);
                    var response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, JsonNode.class);

                    JsonNode mensajes = response.getBody();
                    String detalle = "Pedido desde carrito";
                    int monto = 1000;
                    boolean encontrado = false;

                    for (int i = mensajes.size() - 1; i >= 0; i--) {
                        JsonNode mensaje = mensajes.get(i);
                        if (mensaje.has("direction") && mensaje.get("direction").asText().equals("in") && mensaje.has("text")) {
                            String contenido = mensaje.get("text").asText();
                            boolean tieneProductos = contenido.contains("×");
                            
                            Pattern patternTotal = Pattern.compile("Total: \\$(\\d+)");
                            Matcher mTotal = patternTotal.matcher(contenido);
                            if (tieneProductos && mTotal.find()) {
                                monto = Integer.parseInt(mTotal.group(1));
                                detalle = contenido;
                                encontrado = true;

                                String respuestaResumen = "✅ Recibimos tu pedido.\n\n" + detalle + "\n\n💳 Estamos generando tu link de pago. En breve lo recibirás por este mismo chat.";
                                watiService.enviarMensajeTexto(telefono, respuestaResumen);
                                break;
                            }
                        }
                    }

                    if (!encontrado) {
                        log.warn("⚠️ No se pudo encontrar un mensaje con detalle y total válido para {}", telefono);
                        watiService.enviarMensajeTexto(telefono, "❌ No pudimos procesar tu pedido. Por favor revisa tu carrito y vuelve a intentarlo.");
                        return ResponseEntity.ok().build();
                    }

                    PedidoEntity pedidoExistente = pedidoRepository.findFirstByTelefonoAndEstado(telefono, "pendiente");
                    if (pedidoExistente != null) {
                        log.warn("⚠️ Ya existe un pedido pendiente para el número {}", telefono);
                        String linkPagoExistente = pedidoExistente.getLinkPago();
                        if (linkPagoExistente != null && !linkPagoExistente.isBlank()) {
                            log.info("🔁 Reenviando link de pago existente: {}", linkPagoExistente);
                            watiService.enviarMensajePagoEstatico(telefono, (double) monto, linkPagoExistente);
                        } else {
                            log.warn("⚠️ Pedido pendiente sin link de pago registrado.");
                        }
                        return ResponseEntity.ok().build();
                    }

                    String pedidoId = "pedido-" + UUID.randomUUID().toString().substring(0, 8);
                    PedidoEntity pedido = new PedidoEntity();
                    pedido.setPedidoId(pedidoId);
                    pedido.setTelefono(telefono);
                    pedido.setDetalle(detalle);
                    pedido.setEstado("pendiente");
                    pedidoRepository.save(pedido);

                    PagoResponseDTO pago = transbankService.generarLinkDePago(pedidoId, monto);
                    String linkPago = pago.getUrl();
                    pedido.setLinkPago(linkPago);
                    pedidoRepository.save(pedido);

                    watiService.enviarMensajePagoEstatico(telefono, (double) monto, linkPago);
                }
            }

        } catch (Exception e) {
            log.error("❌ Error procesando webhook", e);
        }

        return ResponseEntity.ok().build();
    }
}
