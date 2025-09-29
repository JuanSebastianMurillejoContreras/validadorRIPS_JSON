package com.example.validadorjson.controller;

import com.example.validadorjson.dto.Factura;
import com.example.validadorjson.service.ValidadorServicePYP;
import com.example.validadorjson.service.ValidadorServiceMorb;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/factura")
public class ValidadorController {

    private final ValidadorServicePYP validadorServicePYP;
    private final ValidadorServiceMorb validadorServiceMorb;


    public ValidadorController(ValidadorServicePYP validadorServicePYP, ValidadorServiceMorb validadorServiceMorb) {
        this.validadorServicePYP = validadorServicePYP;
        this.validadorServiceMorb = validadorServiceMorb;
    }

    @PostMapping("/validar_pyp")
    public ResponseEntity<Resource> validarFacturaPyp(@RequestBody Factura factura) {
        ByteArrayResource resource = validadorServicePYP.validarFactura(factura);
        String fileName = "errores_validacion_fact_" + factura.numFactura() + ".txt";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
    }

    @PostMapping("/validar_morb")
    public ResponseEntity<Resource> validarFacturaMorb(@RequestBody Factura factura) {
        ByteArrayResource resource = validadorServiceMorb.validarFactura(factura);
        String fileName = "errores_validacion_fact_" + factura.numFactura() + ".txt";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
    }
}