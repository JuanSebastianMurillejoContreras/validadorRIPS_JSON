package com.example.validadorjson.dto;

import java.util.List;

public record Servicios(
     List<Consulta> consultas,
     List<Procedimiento> procedimientos

){}
