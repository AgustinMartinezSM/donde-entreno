package com.dondeentreno.api.exception;

import com.dondeentreno.api.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
     * Maneja credenciales invalidas sin revelar si el usuario existe.
     *
     * @param exception excepcion de autenticacion controlada.
     * @param request informacion de la peticion HTTP.
     * @return respuesta JSON con status 401.
     */
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponseDTO> manejarCredencialesInvalidas(
            CredencialesInvalidasException exception,
            HttpServletRequest request
    ) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(EmailYaRegistradoException.class)
    public ResponseEntity<ErrorResponseDTO> manejarEmailYaRegistrado(
            EmailYaRegistradoException exception,
            HttpServletRequest request
    ) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(RegistroInvalidoException.class)
    public ResponseEntity<ErrorResponseDTO> manejarRegistroInvalido(
            RegistroInvalidoException exception,
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

    @ExceptionHandler(ConfiguracionSistemaInvalidaException.class)
    public ResponseEntity<ErrorResponseDTO> manejarConfiguracionSistemaInvalida(
            ConfiguracionSistemaInvalidaException exception,
            HttpServletRequest request
    ) {
        log.error(
                "Configuracion de sistema invalida al procesar {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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
     * Maneja reglas de negocio invalidas del flujo de cambios de actividad.
     *
     * @param exception excepcion de negocio controlada.
     * @param request informacion de la peticion HTTP.
     * @return respuesta JSON con status 400.
     */
    @ExceptionHandler(SolicitudCambioInvalidaException.class)
    public ResponseEntity<ErrorResponseDTO> manejarSolicitudCambioInvalida(
            SolicitudCambioInvalidaException exception,
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
     * Maneja el conflicto de solicitudes de cambio duplicadas
     * (la actividad ya tiene una solicitud abierta).
     *
     * @param exception excepcion de conflicto controlada.
     * @param request informacion de la peticion HTTP.
     * @return respuesta JSON con status 409.
     */
    @ExceptionHandler(SolicitudCambioConflictoException.class)
    public ResponseEntity<ErrorResponseDTO> manejarSolicitudCambioConflicto(
            SolicitudCambioConflictoException exception,
            HttpServletRequest request
    ) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Maneja filtros de busqueda con valores invalidos.
     *
     * Ejemplo:
     * GET /api/actividades?nivel=CUALQUIERA
     *
     * En vez de descartar el filtro en silencio, devuelve un 400
     * indicando el parametro invalido y los valores permitidos.
     *
     * @param exception excepcion de filtro invalido.
     * @param request informacion de la peticion HTTP.
     * @return respuesta JSON con status 400.
     */
    @ExceptionHandler(FiltroInvalidoException.class)
    public ResponseEntity<ErrorResponseDTO> manejarFiltroInvalido(
            FiltroInvalidoException exception,
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
     * Maneja imagenes invalidas (tipo, tamano o formato) en la subida
     * y las reglas de moderacion (por ejemplo revisar dos veces).
     */
    @ExceptionHandler(ImagenInvalidaException.class)
    public ResponseEntity<ErrorResponseDTO> manejarImagenInvalida(
            ImagenInvalidaException exception,
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
     * Maneja archivos que superan el limite de multipart configurado.
     */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDTO> manejarArchivoDemasiadoGrande(
            org.springframework.web.multipart.MaxUploadSizeExceededException exception,
            HttpServletRequest request
    ) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase(),
                "El archivo supera el tamano maximo permitido (2 MB).",
                request.getRequestURI(),
                OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    /**
     * Maneja operaciones de imagenes cuando el almacenamiento externo
     * (Supabase Storage) todavia no esta configurado en el entorno.
     */
    @ExceptionHandler(AlmacenNoConfiguradoException.class)
    public ResponseEntity<ErrorResponseDTO> manejarAlmacenNoConfigurado(
            AlmacenNoConfiguradoException exception,
            HttpServletRequest request
    ) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * Maneja consultas al asistente con entrada invalida
     * (vacia o mas larga que el maximo configurado).
     */
    @ExceptionHandler(ConsultaAsistenteInvalidaException.class)
    public ResponseEntity<ErrorResponseDTO> manejarConsultaAsistenteInvalida(
            ConsultaAsistenteInvalidaException exception,
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
     * Maneja el limite de consultas por IP del asistente.
     *
     * No se loguea el texto de la consulta: para diagnosticar alcanza con
     * saber que la ruta recibio mas trafico del permitido.
     */
    @ExceptionHandler(LimiteConsultasExcedidoException.class)
    public ResponseEntity<ErrorResponseDTO> manejarLimiteConsultasExcedido(
            LimiteConsultasExcedidoException exception,
            HttpServletRequest request
    ) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }

    /**
     * Maneja rutas inexistentes que Spring MVC resuelve como recurso no encontrado.
     *
     * @param exception excepcion de ruta o recurso inexistente.
     * @param request informacion de la peticion HTTP.
     * @return respuesta JSON con status 404.
     */
    @ExceptionHandler({
            NoResourceFoundException.class,
            NoHandlerFoundException.class
    })
    public ResponseEntity<ErrorResponseDTO> manejarRutaNoEncontrada(
            Exception exception,
            HttpServletRequest request
    ) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                "Recurso no encontrado.",
                request.getRequestURI(),
                OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
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
     * La excepción se loguea con stack trace completo porque es la única
     * forma de diagnosticar el 500 desde los logs del servidor.
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
        log.error(
                "Error no controlado al procesar {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

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
