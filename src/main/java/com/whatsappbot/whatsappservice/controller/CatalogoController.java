package com.whatsappbot.whatsappservice.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.whatsappbot.whatsappservice.model.ProductoCatalogo;
import com.whatsappbot.whatsappservice.service.CatalogoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping
    public List<ProductoCatalogo> obtenerCatalogo() {
        return catalogoService.obtenerCatalogo();
    }
}
