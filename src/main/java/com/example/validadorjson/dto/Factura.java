package com.example.validadorjson.dto;

import java.util.List;

public record Factura (
     String numDocumentoIdObligado,
     String numFactura,
     String tipoNota,
     String numNota,
     List<Usuario> usuarios
){}
