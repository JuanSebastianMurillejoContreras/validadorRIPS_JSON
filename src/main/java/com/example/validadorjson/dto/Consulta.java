package com.example.validadorjson.dto;

public record Consulta (
    String codPrestador,
     String fechaInicioAtencion,
     String numAutorizacion,
     String codConsulta,
     String modalidadGrupoServicioTecSal,
     String grupoServicios,
     int codServicio,
     String finalidadTecnologiaSalud,
     String causaMotivoAtencion,
     String codDiagnosticoPrincipal,
     String tipoDiagnosticoPrincipal,
     String tipoDocumentoIdentificacion,
     String numDocumentoIdentificacion,
     double vrServicio,
     String conceptoRecaudo,
     double valorPagoModerador,
     int consecutivo
){}

