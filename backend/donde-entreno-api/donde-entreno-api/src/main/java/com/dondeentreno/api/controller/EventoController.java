package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.EventoDeportivoDTO;
import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.service.EventoDeportivoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * El calendario público de eventos (Fase 9). Público: se ve sin
 * sesión. Con JWT opcional, cada evento sabe si el usuario ya marcó
 * "me interesa".
 */
@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final EventoDeportivoService eventoDeportivoService;

    public EventoController(EventoDeportivoService eventoDeportivoService) {
        this.eventoDeportivoService = eventoDeportivoService;
    }

    /**
     * GET /api/eventos?rango=hoy|finde|semana|proximos
     *
     * El rango se resuelve ACÁ, en zona argentina, y no en el
     * frontend: "este finde" es una pregunta sobre el calendario del
     * lugar, no sobre el reloj del dispositivo de quien mira.
     */
    @GetMapping
    public PaginaResponseDTO<EventoDeportivoDTO> calendario(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "proximos") String rango,
            @RequestParam(required = false) Long ciudadId,
            @RequestParam(required = false) String ciudadSlug,
            @RequestParam(required = false) Long barrioId,
            @RequestParam(required = false) Long deporteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Ventana ventana = resolverRango(rango);

        return eventoDeportivoService.calendario(
                ventana.desde(),
                ventana.hasta(),
                ciudadId,
                ciudadSlug,
                barrioId,
                deporteId,
                extraerUserIdOpcional(jwt),
                page,
                size
        );
    }

    @GetMapping("/{slug}")
    public EventoDeportivoDTO detalle(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String slug
    ) {
        return eventoDeportivoService.obtenerPorSlug(slug, extraerUserIdOpcional(jwt));
    }

    /**
     * Los eventos próximos de una actividad, para el aviso en su
     * detalle. Endpoint propio y no un campo del detalle: el detalle
     * de la actividad ya es el DTO más pesado del catálogo.
     */
    @GetMapping("/de-actividad/{actividadId}")
    public java.util.List<EventoDeportivoDTO> deActividad(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId,
            @RequestParam(defaultValue = "3") int limite
    ) {
        return eventoDeportivoService.proximosDeActividad(
                actividadId, extraerUserIdOpcional(jwt), limite);
    }

    private Ventana resolverRango(String rango) {
        OffsetDateTime ahora = OffsetDateTime.now();
        LocalDate hoy = LocalDate.now(ZONA_ARGENTINA);

        return switch (rango == null ? "proximos" : rango.toLowerCase()) {
            case "hoy" -> new Ventana(ahora, finDelDia(hoy));
            /*
              "Este finde" es de viernes a domingo inclusive. Un jueves
              devuelve el finde que viene; un sábado, lo que queda de
              este — que es lo que uno espera al preguntar.
            */
            case "finde" -> new Ventana(ahora, finDelDia(proximoDomingo(hoy)));
            case "semana" -> new Ventana(ahora, finDelDia(hoy.plusDays(7)));
            case "proximos" -> new Ventana(ahora, null);
            default -> throw new FiltroInvalidoException(
                    "El rango tiene que ser hoy, finde, semana o proximos."
            );
        };
    }

    private LocalDate proximoDomingo(LocalDate hoy) {
        if (hoy.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return hoy;
        }

        LocalDate cursor = hoy;
        while (cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
            cursor = cursor.plusDays(1);
        }

        return cursor;
    }

    private OffsetDateTime finDelDia(LocalDate dia) {
        return dia.plusDays(1).atStartOfDay(ZONA_ARGENTINA).toOffsetDateTime();
    }

    private Long extraerUserIdOpcional(Jwt jwt) {
        if (jwt == null) {
            return null;
        }

        Object userId = jwt.getClaim("userId");

        if (userId instanceof Number number) {
            return number.longValue();
        }

        if (userId instanceof String texto) {
            try {
                return Long.parseLong(texto);
            } catch (NumberFormatException excepcion) {
                return null;
            }
        }

        return null;
    }

    private record Ventana(OffsetDateTime desde, OffsetDateTime hasta) {
    }
}
