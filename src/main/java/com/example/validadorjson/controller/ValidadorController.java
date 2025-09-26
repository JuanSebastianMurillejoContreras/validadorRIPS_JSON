package com.example.validadorjson.controller;

import com.example.validadorjson.dto.Factura;
import com.example.validadorjson.service.ValidadorService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/factura")
public class ValidadorController {

    private final ValidadorService validadorService;

    public ValidadorController(ValidadorService validadorService) {
        this.validadorService = validadorService;
    }

    @PostMapping("/validar")
    public ResponseEntity<Resource> validarFactura(@RequestBody Factura factura) {
        ByteArrayResource resource = validadorService.validarFactura(factura);
        String fileName = "errores_validacion_fact_" + factura.numFactura() + ".txt";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
    }

}
