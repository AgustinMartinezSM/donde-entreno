package com.dondeentreno.api.exception;

import com.dondeentreno.api.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador global de errores de la API.
 *
 * Esta clase captura excepciones lanzadas desde controllers o services
 * y devuelve respuestas JSON prolijas en vez de mostrar Whitelabel Error Page.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja errores de validacion de campos enviados en el cuerpo de la solicitud.
     *
     * @param exception excepcion de Bean Validation.
     * @param request informacion de la peticion HTTP.
     * @return respuesta JSON con status 400 y errores por campo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> manejarValidacionDeCampos(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errores = new LinkedHashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "La solicitud contiene datos invalidos.",
                errores,
                request.getRequestURI(),
                OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Maneja errores de JSON mal formado o cuerpo no legible.
     *
     * @param exception excepcion lanzada al leer el cuerpo HTTP.
     * @param request informacion de la peticion HTTP.
     * @return respuesta JSON con status 400.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> manejarJsonMalFormado(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "El cuerpo de la solicitud no tiene un formato JSON valido.",
                request.getRequestURI(),
                OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Maneja parametros HTTP con formato invalido.
     *
     * @param exception excepcion lanzada al convertir un parametro.
     * @param request informacion de la peticion HTTP.
     * @return respuesta JSON con status 400 y errores por parametro.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> manejarParametroConTipoInvalido(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errores = new LinkedHashMap<>();
        errores.put(
                exception.getName(),
                "El parametro debe tener un valor valido."
        );

        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Uno o mas parametros tienen un formato invalido.",
                errores,
                request.getRequestURI(),
                OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    /**
     * Maneja reglas de negocio invalidas al crear solicitudes publicas.
     *
     * @param exception excepcion de negocio controlada.
     * @param request informacion de la peticion HTTP.
     * @return respuesta JSON con status 400.
     */
    @ExceptionHandler(SolicitudPublicacionInvalidaException.class)
    public ResponseEntity<ErrorResponseDTO> manejarSolicitudPublicacionInvalida(
            SolicitudPublicacionInvalidaException exception,
            HttpServletRequest request
    ) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

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
