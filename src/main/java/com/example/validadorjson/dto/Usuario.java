package com.example.validadorjson.dto;

public record Usuario (
     String tipoDocumentoIdentificacion,
     String numDocumentoIdentificacion,
     String tipoUsuario,
     String fechaNacimiento,
     String codSexo,
     String codPaisResidencia,
     String codMunicipioResidencia,
     String codZonaTerritorialResidencia,
     String incapacidad,
     int consecutivo,
     String codPaisOrigen,
     Servicios servicios
     ){}
