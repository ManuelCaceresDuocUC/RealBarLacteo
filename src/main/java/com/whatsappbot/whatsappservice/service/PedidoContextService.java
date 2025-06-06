package com.whatsappbot.whatsappservice.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.whatsappbot.whatsappservice.model.PedidoEntity;

@Service
public class PedidoContextService {
    public final ConcurrentHashMap<String, PedidoEntity> pedidoTemporalPorTelefono = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, Boolean> indicacionPreguntadaPorTelefono = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, String> ultimoMensajeProcesadoPorNumero = new ConcurrentHashMap<>();
}