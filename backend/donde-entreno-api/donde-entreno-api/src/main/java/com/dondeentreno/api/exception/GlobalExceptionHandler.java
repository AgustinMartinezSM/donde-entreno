package com.dondeentreno.api.exception;

import com.dondeentreno.api.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

/**
 * Manejador global de errores de la API.
 *
 * Esta clase captura excepciones lanzadas desde controllers o services
 * y devuelve respuestas JSON prolijas en vez de mostrar Whitelabel Error Page.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja errores de recurso no encontrado.
     *
     * Ejemplo:
     * GET /api/actividades/no-existe
     *
     * En vez de devolver un error 500, devuelve un 404.
     *
     * @param exception excepción lanzada.
     * @param request información de la petición HTTP.
     * @return respuesta JSON con status 404.
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponseDTO> manejarRecursoNoEncontrado(
            RecursoNoEncontradoException exception,
            HttpServletRequest request
    ) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Maneja errores generales no controlados.
     *
     * Esto evita que el usuario vea una página Whitelabel.
     * Más adelante, cuando tengamos logs más profesionales,
     * podemos mejorar este manejo.
     *
     * @param exception excepción inesperada.
     * @param request información de la petición HTTP.
     * @return respuesta JSON con status 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> manejarErrorGeneral(
            Exception exception,
            HttpServletRequest request
    ) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ocurrió un error inesperado en el servidor",
                request.getRequestURI(),
                OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
