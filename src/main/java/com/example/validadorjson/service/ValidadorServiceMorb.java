package com.example.validadorjson.service;

import com.example.validadorjson.dto.Consulta;
import com.example.validadorjson.dto.Factura;
import com.example.validadorjson.dto.Procedimiento;
import com.example.validadorjson.dto.Usuario;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ValidadorServiceMorb {

    // Guarda errores por número de factura (para endpoints que quieran consultarlos)
    private final ConcurrentHashMap<String, StringBuilder> erroresPorFactura = new ConcurrentHashMap<>();

    private static final Set<String> DOCUMENTOS_VALIDOS = Set.of("CC", "CE", "PA", "RC", "TI", "AS", "MS");
    private static final DateTimeFormatter FN_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FA_FMT_MIN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FA_FMT_SEC = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

       /**
     * Valida la factura completa y devuelve un ByteArrayResource con el contenido TXT para descargar.
     * También guarda el contenido en memoria (erroresPorFactura) y opcionalmente en disco.
     */
    public ByteArrayResource validarFactura(Factura factura) {
        String numFactura = Optional.ofNullable(factura.numFactura()).orElse("sin_numfact");
        StringBuilder errores = new StringBuilder();

        // Cabecera resumen (puedes enriquecer más adelante)
        errores.append("Validación factura: ").append(numFactura).append(System.lineSeparator());
        errores.append("==========================================================================").append(System.lineSeparator());

        // Recorremos usuarios (cada usuario tiene sus servicios)
        if (factura.usuarios() != null) {
            for (Usuario usuario : factura.usuarios()) {
                procesarUsuario(usuario, errores);
            }
        } else {
            errores.append("⚠️ No se encontraron usuarios en la factura.").append(System.lineSeparator());
        }

        // Guardamos en memoria para posibles consultas posteriores
        erroresPorFactura.put(numFactura, errores);

        // Opcional: también escribir al disco (si lo deseas)
        String outputPath = "errores_validacion_fact_" + numFactura + ".txt";
        try {
            Files.write(Paths.get(outputPath), errores.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // no interrumpe la respuesta, solo anotamos en el propio texto
            errores.append("⚠️ No se pudo escribir archivo en disco: ").append(e.getMessage()).append(System.lineSeparator());
        }

        // Devolver como recurso en memoria
        return new ByteArrayResource(errores.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Recupera errores por número de factura (útil si tienes endpoint GET /descargar/{numFactura}).
     */
    public StringBuilder obtenerErrores(String numFactura) {
        return erroresPorFactura.getOrDefault(numFactura,
                new StringBuilder("⚠️ No se encontraron errores para esta factura."));
    }

    /* --------------------------- Procesamiento por usuario --------------------------- */

    private void procesarUsuario(Usuario usuario, StringBuilder errores) {
        if (usuario == null) return;

        int consecutivoUsuario = usuario.consecutivo();
        String pacienteDocumento = Optional.ofNullable(usuario.numDocumentoIdentificacion())
                .orElse("ND-" + consecutivoUsuario);

        // Sets para detectar duplicados por paciente (por día)
        Set<String> consultasUnicas = new HashSet<>();
        Set<String> procedimientosUnicos = new HashSet<>();

        if (usuario.servicios() == null) {
            errores.append("Usuario consecutivo ").append(consecutivoUsuario)
                    .append(" -> No tiene sección 'servicios'.").append(System.lineSeparator());
            return;
        }

        // Consultas
        if (usuario.servicios().consultas() != null) {
            for (Consulta c : usuario.servicios().consultas()) {
                try {
                    String fechaAt = Optional.ofNullable(c.fechaInicioAtencion()).orElse("");
                    String dateKey = fechaAt.length() >= 10 ? fechaAt.substring(0, 10) : fechaAt;

                    String key = pacienteDocumento + "_" +
                            safeString(c.codConsulta()) + "_" +
                            safeString(c.finalidadTecnologiaSalud()) + "_" +
                            safeString(c.codDiagnosticoPrincipal()) + "_" +
                            dateKey;

                    if (!consultasUnicas.add(key)) {
                        registrarError(errores, consecutivoUsuario,
                                "Consulta duplicada",
                                fechaAt,
                                c.codConsulta(),
                                "El paciente tiene otra consulta con el mismo código, finalidad y diagnóstico en la misma fecha.");
                    }

                    int edad = calcularEdadEnAtencionSafe(usuario.fechaNacimiento(), fechaAt, errores, consecutivoUsuario);
                    validarDocumentoSegunEdad(usuario, fechaAt, edad, errores);


                } catch (Exception ex) {
                    registrarError(errores, consecutivoUsuario, "Error lectura consulta",
                            c != null ? c.fechaInicioAtencion() : "N/A",
                            c != null ? c.codConsulta() : "N/A",
                            "Error procesando consulta: " + ex.getMessage());
                }
            }
        }


        // Procedimientos
        if (usuario.servicios().procedimientos() != null) {
            for (Procedimiento p : usuario.servicios().procedimientos()) {
                try {
                    String fechaAt = Optional.ofNullable(p.fechaInicioAtencion()).orElse("");
                    String dateKey = fechaAt.length() >= 10 ? fechaAt.substring(0, 10) : fechaAt;

                    String key = pacienteDocumento + "_" +
                            safeString(p.codProcedimiento()) + "_" +
                            safeString(p.finalidadTecnologiaSalud()) + "_" +
                            safeString(p.codDiagnosticoPrincipal()) + "_" +
                            dateKey;

                    if (!procedimientosUnicos.add(key)) {
                        registrarError(errores, consecutivoUsuario,
                                "Procedimiento duplicado",
                                fechaAt,
                                p.codProcedimiento(),
                                "El paciente tiene otro procedimiento con el mismo código, finalidad y diagnóstico en la misma fecha. "
                                        + "(Consecutivo procedimiento: " + p.consecutivo() + ")");
                    }

                    int edad = calcularEdadEnAtencionSafe(usuario.fechaNacimiento(), fechaAt, errores, consecutivoUsuario);
                    validarDocumentoSegunEdad(usuario, fechaAt, edad, errores);


                } catch (Exception ex) {
                    registrarError(errores, consecutivoUsuario, "Error lectura procedimiento",
                            p != null ? p.fechaInicioAtencion() : "N/A",
                            p != null ? p.codProcedimiento() : "N/A",
                            "Error procesando procedimiento: " + ex.getMessage());
                }
            }
        }
    }


    /* --------------------------- Validaciones específicas ---------------------------

    (Se conservan tus métodos comentados por si quieres recuperarlos en el futuro)
     */

    /**
     * Reglas para validar el tipo de documento frente a la edad (años/días).
     */
    private void validarDocumentoSegunEdad(Usuario usuario, String fechaAtencion, int edadAnios, StringBuilder errores) {
        String tipoDoc = Optional.ofNullable(usuario.tipoDocumentoIdentificacion()).orElse("");
        int consecutivoUsuario = usuario.consecutivo();

        // validar que el tipo esté dentro de los permitidos
        if (!DOCUMENTOS_VALIDOS.contains(tipoDoc)) {
            String msg = "Usuario consecutivo " + consecutivoUsuario +
                    " -> Tipo de documento inválido: " + tipoDoc +
                    ". Debe ser uno de " + DOCUMENTOS_VALIDOS;
            appendError(errores, msg);
            return;
        }

        // calcular días de vida en la fecha de atención (para validar MS)
        LocalDate fechaNacimiento;
        LocalDate fechaAt;
        try {
            fechaNacimiento = LocalDate.parse(usuario.fechaNacimiento(), FN_FMT);
            fechaAt = parseFechaAtencionToLocalDate(fechaAtencion);
        } catch (DateTimeParseException ex) {
            appendError(errores, "Usuario consecutivo " + consecutivoUsuario + " -> Error parseando fechas: " + ex.getMessage());
            return;
        }
        long diasVida = ChronoUnit.DAYS.between(fechaNacimiento, fechaAt);

        boolean valido = true;
        String sugerencia = "";

        switch (tipoDoc) {
            case "MS":
                if (diasVida > 30) {
                    valido = false;
                    sugerencia = "MS solo es válido hasta 30 días de nacido.";
                }
                break;
            case "RC":
                if (edadAnios >= 7) {
                    valido = false;
                    sugerencia = "RC aplica para menores de 7 años; si tiene >=7 años use TI o CC según corresponda.";
                }
                break;
            case "TI":
                if (edadAnios < 7 || edadAnios > 17) {
                    valido = false;
                    sugerencia = "TI aplica entre 7 y 17 años cumplidos.";
                }
                break;
            case "AS":
                if (edadAnios <= 17) {
                    valido = false;
                    sugerencia = "AS aplica solo para mayores de 17 años (adulto sin identificación).";
                }
                break;
            case "CC":
                if (edadAnios < 18) {
                    valido = false;
                    sugerencia = "CC aplica preferiblemente para mayores de 17 años; revise el tipo de documento.";
                }
                break;
            default:
                // CE, PA: no reglas estrictas en edad (se aceptan)
                break;
        }

        // regla adicional: si es mayor de 17 no puede ser RC/TI/MS
        if (edadAnios >= 18 && ("RC".equals(tipoDoc) || "TI".equals(tipoDoc) || "MS".equals(tipoDoc))) {
            valido = false;
            sugerencia = "Para mayores de 17 años no se debe usar RC/TI/MS; use CC, CE o PA según corresponda.";
        }

        if (!valido) {
            String msg = "Usuario consecutivo " + consecutivoUsuario +
                    " -> Tipo de documento no coincide con la edad (" + edadAnios + " años, " + diasVida + " días). " + sugerencia;
            appendError(errores, msg);
        }
    }

    /* --------------------------- Utilidades y helpers --------------------------- */

    private int calcularEdadEnAtencionSafe(String fechaNacimiento, String fechaAtencion, StringBuilder errores, int consecutivoUsuario) {
        try {
            LocalDate fn = LocalDate.parse(fechaNacimiento, FN_FMT);
            LocalDate fa = parseFechaAtencionToLocalDate(fechaAtencion);
            return Period.between(fn, fa).getYears();
        } catch (DateTimeParseException ex) {
            appendError(errores, "Usuario consecutivo " + consecutivoUsuario + " -> Error parseando fechaNacimiento/fechaAtencion: " + ex.getMessage());
            return -1;
        }
    }

    private LocalDate parseFechaAtencionToLocalDate(String fechaAtencion) {
        if (fechaAtencion == null || fechaAtencion.isBlank()) {
            throw new DateTimeParseException("fechaInicioAtencion vacía", fechaAtencion, 0);
        }

        // intentamos varios formatos comunes
        try {
            return LocalDateTime.parse(fechaAtencion, FA_FMT_MIN).toLocalDate();
        } catch (DateTimeParseException ignored) { }
        try {
            return LocalDateTime.parse(fechaAtencion, FA_FMT_SEC).toLocalDate();
        } catch (DateTimeParseException ignored) { }
        try {
            // caso ISO como 2025-04-22T13:57:00
            return LocalDateTime.parse(fechaAtencion).toLocalDate();
        } catch (DateTimeParseException ignored) { }

        // fallback: si vienen al menos yyyy-MM-dd
        if (fechaAtencion.length() >= 10) {
            return LocalDate.parse(fechaAtencion.substring(0, 10), FN_FMT);
        }
        throw new DateTimeParseException("Formato de fecha inválido para fechaInicioAtencion", fechaAtencion, 0);
    }

    private void registrarError(StringBuilder errores, int consecutivoUsuario, String tipoError, String fecha, String codigo, String detalle) {
        String msg = "Usuario consecutivo " + consecutivoUsuario +
                " -> " + tipoError +
                " en " + (fecha == null ? "N/A" : fecha) +
                " con código " + safeString(codigo) +
                ". " + detalle;
        appendError(errores, msg);
    }

    private void appendError(StringBuilder sb, String line) {
        sb.append(line).append(System.lineSeparator());
    }

    private String safeString(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }


}
