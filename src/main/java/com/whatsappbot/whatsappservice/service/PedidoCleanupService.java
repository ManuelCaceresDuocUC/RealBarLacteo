package com.whatsappbot.whatsappservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.whatsappbot.whatsappservice.model.PedidoEntity;
import com.whatsappbot.whatsappservice.repository.PedidoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoCleanupService {

    private final PedidoRepository pedidoRepository;
    private final PedidoContextService pedidoContext;

    @Scheduled(fixedRate = 300000) // Cada 5 minutos
    public void limpiarPedidosPendientesAntiguos() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(15);
        List<PedidoEntity> antiguos = pedidoRepository.findByEstadoAndFechaCreacionBefore("pendiente", limite);

        if (!antiguos.isEmpty()) {
            pedidoRepository.deleteAll(antiguos);
            log.info("🧹 Se eliminaron {} pedidos pendientes antiguos", antiguos.size());

            // 🔄 Limpiar también en memoria
            antiguos.forEach(pedido -> {
                String telefono = pedido.getTelefono();
                pedidoContext.pedidoTemporalPorTelefono.remove(telefono);
                pedidoContext.indicacionPreguntadaPorTelefono.remove(telefono);
                pedidoContext.ultimoMensajeProcesadoPorNumero.remove(telefono);
            });
        }
    }
}
