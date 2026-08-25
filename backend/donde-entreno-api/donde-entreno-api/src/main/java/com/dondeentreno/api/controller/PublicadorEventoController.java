package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.EventoDeportivoDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.service.EventoDeportivoService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * La agenda del publicador (Fase 9). Bajo /api/publicador/** → exige
 * rol PUBLICADOR.
 */
@RestController
@RequestMapping("/api/publicador/eventos")
public class PublicadorEventoController {

    private final EventoDeportivoService eventoDeportivoService;

    public PublicadorEventoController(EventoDeportivoService eventoDeportivoService) {
        this.eventoDeportivoService = eventoDeportivoService;
    }

    @GetMapping
    public List<EventoDeportivoDTO> listarMios(@AuthenticationPrincipal Jwt jwt) {
        return eventoDeportivoService.listarMios(extraerUserId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventoDeportivoDTO publicar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> cuerpo
    ) {
        return eventoDeportivoService.publicar(
                extraerUserId(jwt),
                new EventoDeportivoService.DatosEvento(
                        cuerpo.get("titulo"),
                        cuerpo.get("descripcion"),
                        fecha(cuerpo.get("iniciaAt"), "La fecha de inicio no es válida."),
                        fecha(cuerpo.get("terminaAt"), "La fecha de fin no es válida."),
                        numero(cuerpo.get("actividadId")),
                        numero(cuerpo.get("ubicacionId")),
                        numero(cuerpo.get("deporteId")),
                        numero(cuerpo.get("imagenId")),
                        entero(cuerpo.get("cupo")),
                        booleano(cuerpo.get("esGratis")),
                        decimal(cuerpo.get("precioReferencia")),
                        booleano(cuerpo.get("mostrarPrecio"))
                )
        );
    }

    /**
     * Cancelar NO es borrar: el evento sale del calendario pero su
     * detalle sigue vivo avisando que se canceló, porque el link ya
     * circuló por WhatsApp.
     */
    @PatchMapping("/{id}/cancelar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        eventoDeportivoService.cancelar(extraerUserId(jwt), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        eventoDeportivoService.eliminarPropio(extraerUserId(jwt), id);
    }

    private OffsetDateTime fecha(String valor, String mensajeError) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(valor);
        } catch (DateTimeParseException excepcion) {
            throw new FiltroInvalidoException(mensajeError);
        }
    }

    private Long numero(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException excepcion) {
            return null;
        }
    }

    private Integer entero(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException excepcion) {
            return null;
        }
    }

    private BigDecimal decimal(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(valor);
        } catch (NumberFormatException excepcion) {
            return null;
        }
    }

    private Boolean booleano(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return Boolean.valueOf(valor);
    }

    private Long extraerUserId(Jwt jwt) {
        if (jwt == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }

        Object userId = jwt.getClaim("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }

        if (userId instanceof String texto) {
            try {
                return Long.parseLong(texto);
            } catch (NumberFormatException excepcion) {
                throw new CredencialesInvalidasException("No autenticado.");
            }
        }

        throw new CredencialesInvalidasException("No autenticado.");
    }
}
