package com.example.validadorjson.dto;

public record Procedimiento (
    String codPrestador,
    String fechaInicioAtencion,
     String numAutorizacion,
     String codProcedimiento,
     String viaIngresoServicioSalud,
     String modalidadGrupoServicioTecSal,
     String grupoServicios,
     int codServicio,
     String finalidadTecnologiaSalud,
     String tipoDocumentoIdentificacion,
     String numDocumentoIdentificacion,
     String codDiagnosticoPrincipal,
     double vrServicio,
     String conceptoRecaudo,
     double valorPagoModerador,
     int consecutivo
){}
