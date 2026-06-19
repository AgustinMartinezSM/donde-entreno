package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.SolicitudPublicacionHorarioRequestDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionRequestDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionResponseDTO;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.SolicitudPublicacion;
import com.dondeentreno.api.entity.SolicitudPublicacionHorario;
import com.dondeentreno.api.exception.SolicitudPublicacionInvalidaException;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionHorarioRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Service para crear solicitudes publicas de publicacion.
 */
@Service
public class SolicitudPublicacionService {

    private static final String ORIGEN_FORMULARIO_WEB = "FORMULARIO_WEB";
    private static final String ESTADO_PENDIENTE = "PENDIENTE";
    private static final String MENSAJE_SOLICITUD_CREADA =
            "La solicitud fue recibida correctamente y quedo pendiente de revision.";
    private static final int MAX_INTENTOS_CODIGO_SEGUIMIENTO = 10;
    private static final int MAX_LONGITUD_WHATSAPP_NORMALIZADO = 30;
    private static final DateTimeFormatter FORMATO_FECHA_CODIGO = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Set<String> DIAS_SEMANA_VALIDOS = Set.of(
            "LUNES",
            "MARTES",
            "MIERCOLES",
            "JUEVES",
            "VIERNES",
            "SABADO",
            "DOMINGO"
    );

    private final SolicitudPublicacionRepository solicitudPublicacionRepository;
    private final SolicitudPublicacionHorarioRepository solicitudPublicacionHorarioRepository;
    private final DeporteRepository deporteRepository;
    private final CiudadRepository ciudadRepository;
    private final BarrioRepository barrioRepository;

    /**
     * Inyeccion de dependencias por constructor.
     */
    public SolicitudPublicacionService(
            SolicitudPublicacionRepository solicitudPublicacionRepository,
            SolicitudPublicacionHorarioRepository solicitudPublicacionHorarioRepository,
            DeporteRepository deporteRepository,
            CiudadRepository ciudadRepository,
            BarrioRepository barrioRepository
    ) {
        this.solicitudPublicacionRepository = solicitudPublicacionRepository;
        this.solicitudPublicacionHorarioRepository = solicitudPublicacionHorarioRepository;
        this.deporteRepository = deporteRepository;
        this.ciudadRepository = ciudadRepository;
        this.barrioRepository = barrioRepository;
    }

    /**
     * Crea una solicitud publica y sus horarios dentro de una unica transaccion.
     *
     * @param request datos enviados por el formulario publico.
     * @return respuesta con el codigo de seguimiento generado.
     */
    @Transactional
    public SolicitudPublicacionResponseDTO crearSolicitud(SolicitudPublicacionRequestDTO request) {
        if (request == null) {
            throw new SolicitudPublicacionInvalidaException("La solicitud no puede estar vacia.");
        }

        DatosValidados datos = validarRequest(request);
        OffsetDateTime ahora = OffsetDateTime.now();
        String codigoSeguimiento = generarCodigoSeguimiento();

        SolicitudPublicacion solicitud = crearSolicitudPublicacion(
                request,
                datos,
                codigoSeguimiento,
                ahora
        );

        SolicitudPublicacion solicitudGuardada = solicitudPublicacionRepository.save(solicitud);
        List<SolicitudPublicacionHorario> horarios = crearHorarios(
                datos.horarios(),
                solicitudGuardada,
                ahora
        );

        solicitudPublicacionHorarioRepository.saveAll(horarios);

        return new SolicitudPublicacionResponseDTO(
                solicitudGuardada.getId(),
                solicitudGuardada.getCodigoSeguimiento(),
                solicitudGuardada.getEstado(),
                solicitudGuardada.getCreatedAt(),
                MENSAJE_SOLICITUD_CREADA
        );
    }

    private DatosValidados validarRequest(SolicitudPublicacionRequestDTO request) {
        Deporte deporte = validarDeporte(request.getDeporteId(), request.getDeporteOtro());
        String deporteOtro = validarTextoExclusivoOtro(
                request.getDeporteId(),
                request.getDeporteOtro(),
                "Debe informar un deporte existente o un deporte nuevo, pero no ambos.",
                "Debe informar un deporte existente o un deporte nuevo.",
                "El deporte informado no puede estar vacio."
        );

        Ciudad ciudad = validarCiudad(request.getCiudadId(), request.getCiudadOtra());
        String ciudadOtra = validarTextoExclusivoOtro(
                request.getCiudadId(),
                request.getCiudadOtra(),
                "Debe informar una ciudad existente o una ciudad nueva, pero no ambas.",
                "Debe informar una ciudad existente o una ciudad nueva.",
                "La ciudad informada no puede estar vacia."
        );

        Barrio barrio = validarBarrio(request.getBarrioId(), request.getBarrioOtro(), ciudad);
        String barrioOtro = validarTextoExclusivoOtro(
                request.getBarrioId(),
                request.getBarrioOtro(),
                "Debe informar un barrio existente o un barrio nuevo, pero no ambos.",
                "Debe informar un barrio existente o un barrio nuevo.",
                "El barrio informado no puede estar vacio."
        );

        String nombreLugar = limpiarTextoOpcional(request.getNombreLugar());
        String direccion = limpiarTextoOpcional(request.getDireccion());
        validarLugar(nombreLugar, direccion);

        String whatsapp = limpiarTextoOpcional(request.getWhatsapp());
        String email = normalizarEmail(request.getEmail());
        validarContacto(whatsapp, email);
        String whatsappNormalizado = normalizarWhatsapp(whatsapp);

        validarEdades(request);
        validarPrecio(request);
        validarCondiciones(request);

        List<HorarioValidado> horarios = validarHorarios(request.getHorarios());

        return new DatosValidados(
                deporte,
                deporteOtro,
                ciudad,
                ciudadOtra,
                barrio,
                barrioOtro,
                nombreLugar,
                direccion,
                limpiarTextoOpcional(request.getReferenciaUbicacion()),
                whatsapp,
                whatsappNormalizado,
                limpiarTextoOpcional(request.getInstagram()),
                email,
                limpiarTextoOpcional(request.getObservacionesSolicitante()),
                horarios
        );
    }

    private Deporte validarDeporte(Long deporteId, String deporteOtro) {
        validarExclusividad(
                deporteId,
                deporteOtro,
                "Debe informar un deporte existente o un deporte nuevo, pero no ambos.",
                "Debe informar un deporte existente o un deporte nuevo.",
                "El deporte informado no puede estar vacio."
        );

        if (deporteId == null) {
            return null;
        }

        Deporte deporte = deporteRepository.findById(deporteId)
                .orElseThrow(() -> new SolicitudPublicacionInvalidaException("El deporte seleccionado no existe."));

        if (!Boolean.TRUE.equals(deporte.getActivo())) {
            throw new SolicitudPublicacionInvalidaException("El deporte seleccionado no esta activo.");
        }

        return deporte;
    }

    private Ciudad validarCiudad(Long ciudadId, String ciudadOtra) {
        validarExclusividad(
                ciudadId,
                ciudadOtra,
                "Debe informar una ciudad existente o una ciudad nueva, pero no ambas.",
                "Debe informar una ciudad existente o una ciudad nueva.",
                "La ciudad informada no puede estar vacia."
        );

        if (ciudadId == null) {
            return null;
        }

        Ciudad ciudad = ciudadRepository.findById(ciudadId)
                .orElseThrow(() -> new SolicitudPublicacionInvalidaException("La ciudad seleccionada no existe."));

        if (!Boolean.TRUE.equals(ciudad.getActiva())) {
            throw new SolicitudPublicacionInvalidaException("La ciudad seleccionada no esta activa.");
        }

        return ciudad;
    }

    private Barrio validarBarrio(Long barrioId, String barrioOtro, Ciudad ciudad) {
        validarExclusividad(
                barrioId,
                barrioOtro,
                "Debe informar un barrio existente o un barrio nuevo, pero no ambos.",
                "Debe informar un barrio existente o un barrio nuevo.",
                "El barrio informado no puede estar vacio."
        );

        if (barrioId == null) {
            return null;
        }

        if (ciudad == null) {
            throw new SolicitudPublicacionInvalidaException(
                    "Para seleccionar un barrio existente debe seleccionar una ciudad existente."
            );
        }

        Barrio barrio = barrioRepository.findById(barrioId)
                .orElseThrow(() -> new SolicitudPublicacionInvalidaException("El barrio seleccionado no existe."));

        if (!Boolean.TRUE.equals(barrio.getActivo())) {
            throw new SolicitudPublicacionInvalidaException("El barrio seleccionado no esta activo.");
        }

        if (barrio.getCiudad() == null || !ciudad.getId().equals(barrio.getCiudad().getId())) {
            throw new SolicitudPublicacionInvalidaException(
                    "El barrio seleccionado no pertenece a la ciudad seleccionada."
            );
        }

        return barrio;
    }

    private void validarExclusividad(
            Long id,
            String texto,
            String mensajeAmbos,
            String mensajeNinguno,
            String mensajeTextoVacio
    ) {
        boolean tieneId = id != null;
        boolean textoInformado = texto != null;
        String textoLimpio = limpiarTextoOpcional(texto);

        if (textoInformado && textoLimpio == null) {
            throw new SolicitudPublicacionInvalidaException(mensajeTextoVacio);
        }

        boolean tieneTexto = textoLimpio != null;

        if (tieneId && tieneTexto) {
            throw new SolicitudPublicacionInvalidaException(mensajeAmbos);
        }

        if (!tieneId && !tieneTexto) {
            throw new SolicitudPublicacionInvalidaException(mensajeNinguno);
        }
    }

    private String validarTextoExclusivoOtro(
            Long id,
            String texto,
            String mensajeAmbos,
            String mensajeNinguno,
            String mensajeTextoVacio
    ) {
        validarExclusividad(id, texto, mensajeAmbos, mensajeNinguno, mensajeTextoVacio);

        if (id != null) {
            return null;
        }

        return limpiarTextoOpcional(texto);
    }

    private void validarLugar(String nombreLugar, String direccion) {
        if (nombreLugar == null && direccion == null) {
            throw new SolicitudPublicacionInvalidaException(
                    "Debe informar al menos el nombre del lugar o la direccion."
            );
        }
    }

    private void validarContacto(String whatsapp, String email) {
        if (whatsapp == null && email == null) {
            throw new SolicitudPublicacionInvalidaException("Debe informar al menos un WhatsApp o un email.");
        }
    }

    private String normalizarWhatsapp(String whatsapp) {
        if (whatsapp == null) {
            return null;
        }

        String whatsappNormalizado = whatsapp.replaceAll("[^0-9]", "");

        if (whatsappNormalizado.isBlank()) {
            throw new SolicitudPublicacionInvalidaException(
                    "El WhatsApp debe contener al menos un digito."
            );
        }

        if (whatsappNormalizado.length() > MAX_LONGITUD_WHATSAPP_NORMALIZADO) {
            throw new SolicitudPublicacionInvalidaException(
                    "El WhatsApp normalizado no puede superar los 30 digitos."
            );
        }

        return whatsappNormalizado;
    }

    private void validarEdades(SolicitudPublicacionRequestDTO request) {
        Integer edadMinima = request.getEdadMinima();
        Integer edadMaxima = request.getEdadMaxima();

        if (edadMinima != null && edadMinima < 0) {
            throw new SolicitudPublicacionInvalidaException("La edad minima no puede ser negativa.");
        }

        if (edadMaxima != null && edadMaxima < 0) {
            throw new SolicitudPublicacionInvalidaException("La edad maxima no puede ser negativa.");
        }

        if (edadMinima != null && edadMaxima != null && edadMinima > edadMaxima) {
            throw new SolicitudPublicacionInvalidaException(
                    "La edad minima no puede ser mayor que la edad maxima."
            );
        }
    }

    private void validarPrecio(SolicitudPublicacionRequestDTO request) {
        if (request.getPrecioReferencia() != null && request.getPrecioReferencia().signum() < 0) {
            throw new SolicitudPublicacionInvalidaException("El precio de referencia no puede ser negativo.");
        }

        if (Boolean.TRUE.equals(request.getMostrarPrecio()) && request.getPrecioReferencia() == null) {
            throw new SolicitudPublicacionInvalidaException(
                    "Para mostrar el precio debe informar un precio de referencia."
            );
        }
    }

    private void validarCondiciones(SolicitudPublicacionRequestDTO request) {
        if (!Boolean.TRUE.equals(request.getAceptaCondiciones())) {
            throw new SolicitudPublicacionInvalidaException("Debe aceptar las condiciones.");
        }
    }

    private List<HorarioValidado> validarHorarios(List<SolicitudPublicacionHorarioRequestDTO> horariosRequest) {
        if (horariosRequest == null || horariosRequest.isEmpty()) {
            throw new SolicitudPublicacionInvalidaException("Debe incluir al menos un horario.");
        }

        List<HorarioValidado> horarios = new ArrayList<>();
        Set<String> clavesUnicas = new HashSet<>();

        for (SolicitudPublicacionHorarioRequestDTO horarioRequest : horariosRequest) {
            if (horarioRequest == null) {
                throw new SolicitudPublicacionInvalidaException("Los horarios informados no pueden estar vacios.");
            }

            String diaSemana = normalizarDiaSemana(horarioRequest.getDiaSemana());

            if (horarioRequest.getHoraInicio() == null) {
                throw new SolicitudPublicacionInvalidaException("La hora de inicio es obligatoria.");
            }

            if (horarioRequest.getHoraFin() == null) {
                throw new SolicitudPublicacionInvalidaException("La hora de finalizacion es obligatoria.");
            }

            if (!horarioRequest.getHoraInicio().isBefore(horarioRequest.getHoraFin())) {
                throw new SolicitudPublicacionInvalidaException(
                        "La hora de inicio debe ser anterior a la hora de finalizacion."
                );
            }

            String clave = diaSemana + "|" + horarioRequest.getHoraInicio() + "|" + horarioRequest.getHoraFin();

            if (!clavesUnicas.add(clave)) {
                throw new SolicitudPublicacionInvalidaException("No se permiten horarios duplicados.");
            }

            horarios.add(new HorarioValidado(
                    diaSemana,
                    horarioRequest.getHoraInicio(),
                    horarioRequest.getHoraFin(),
                    limpiarTextoOpcional(horarioRequest.getObservacion())
            ));
        }

        return horarios;
    }

    private String normalizarDiaSemana(String diaSemana) {
        String diaNormalizado = limpiarTextoOpcional(diaSemana);

        if (diaNormalizado == null) {
            throw new SolicitudPublicacionInvalidaException("El dia de la semana es obligatorio.");
        }

        diaNormalizado = diaNormalizado.toUpperCase(Locale.ROOT);

        if (!DIAS_SEMANA_VALIDOS.contains(diaNormalizado)) {
            throw new SolicitudPublicacionInvalidaException("El dia de la semana informado no es valido.");
        }

        return diaNormalizado;
    }

    private String generarCodigoSeguimiento() {
        String fecha = LocalDate.now().format(FORMATO_FECHA_CODIGO);

        for (int intento = 0; intento < MAX_INTENTOS_CODIGO_SEGUIMIENTO; intento++) {
            String aleatorio = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase(Locale.ROOT);

            String codigoSeguimiento = "DEP-" + fecha + "-" + aleatorio;

            if (!solicitudPublicacionRepository.existsByCodigoSeguimiento(codigoSeguimiento)) {
                return codigoSeguimiento;
            }
        }

        throw new SolicitudPublicacionInvalidaException(
                "No se pudo generar un codigo de seguimiento. Intente nuevamente."
        );
    }

    private SolicitudPublicacion crearSolicitudPublicacion(
            SolicitudPublicacionRequestDTO request,
            DatosValidados datos,
            String codigoSeguimiento,
            OffsetDateTime ahora
    ) {
        SolicitudPublicacion solicitud = new SolicitudPublicacion();

        solicitud.setCodigoSeguimiento(codigoSeguimiento);
        solicitud.setOrigen(ORIGEN_FORMULARIO_WEB);
        solicitud.setEstado(ESTADO_PENDIENTE);
        solicitud.setTipoPublicador(limpiarTextoRequerido(request.getTipoPublicador(), "El tipo de publicador es obligatorio."));
        solicitud.setNombrePublicador(limpiarTextoRequerido(request.getNombrePublicador(), "El nombre del publicador es obligatorio."));
        solicitud.setNombreActividad(limpiarTextoRequerido(request.getNombreActividad(), "El nombre de la actividad es obligatorio."));
        solicitud.setDeporte(datos.deporte());
        solicitud.setDeporteOtro(datos.deporteOtro());
        solicitud.setDescripcion(limpiarTextoRequerido(request.getDescripcion(), "La descripcion es obligatoria."));
        solicitud.setNivel(limpiarTextoRequerido(request.getNivel(), "El nivel es obligatorio."));
        solicitud.setEnfoque(limpiarTextoRequerido(request.getEnfoque(), "El enfoque es obligatorio."));
        solicitud.setModalidad(limpiarTextoRequerido(request.getModalidad(), "La modalidad es obligatoria."));
        solicitud.setEdadMinima(request.getEdadMinima());
        solicitud.setEdadMaxima(request.getEdadMaxima());
        solicitud.setPrecioReferencia(request.getPrecioReferencia());
        solicitud.setMostrarPrecio(Boolean.TRUE.equals(request.getMostrarPrecio()));
        solicitud.setCiudad(datos.ciudad());
        solicitud.setCiudadOtra(datos.ciudadOtra());
        solicitud.setBarrio(datos.barrio());
        solicitud.setBarrioOtro(datos.barrioOtro());
        solicitud.setNombreLugar(datos.nombreLugar());
        solicitud.setDireccion(datos.direccion());
        solicitud.setReferenciaUbicacion(datos.referenciaUbicacion());
        solicitud.setWhatsapp(datos.whatsapp());
        solicitud.setWhatsappNormalizado(datos.whatsappNormalizado());
        solicitud.setInstagram(datos.instagram());
        solicitud.setEmail(datos.email());
        solicitud.setObservacionesSolicitante(datos.observacionesSolicitante());
        solicitud.setAceptaCondiciones(Boolean.TRUE);
        solicitud.setCreatedAt(ahora);
        solicitud.setUpdatedAt(ahora);

        return solicitud;
    }

    private List<SolicitudPublicacionHorario> crearHorarios(
            List<HorarioValidado> horariosValidados,
            SolicitudPublicacion solicitud,
            OffsetDateTime ahora
    ) {
        List<SolicitudPublicacionHorario> horarios = new ArrayList<>();

        for (HorarioValidado horarioValidado : horariosValidados) {
            SolicitudPublicacionHorario horario = new SolicitudPublicacionHorario();
            horario.setSolicitudPublicacion(solicitud);
            horario.setDiaSemana(horarioValidado.diaSemana());
            horario.setHoraInicio(horarioValidado.horaInicio());
            horario.setHoraFin(horarioValidado.horaFin());
            horario.setObservacion(horarioValidado.observacion());
            horario.setCreatedAt(ahora);
            horario.setUpdatedAt(ahora);
            horarios.add(horario);
        }

        return horarios;
    }

    private String limpiarTextoRequerido(String texto, String mensajeSiFalta) {
        String textoLimpio = limpiarTextoOpcional(texto);

        if (textoLimpio == null) {
            throw new SolicitudPublicacionInvalidaException(mensajeSiFalta);
        }

        return textoLimpio;
    }

    private String limpiarTextoOpcional(String texto) {
        if (texto == null) {
            return null;
        }

        String textoLimpio = texto.trim();

        if (textoLimpio.isEmpty()) {
            return null;
        }

        return textoLimpio;
    }

    private String normalizarEmail(String email) {
        String emailLimpio = limpiarTextoOpcional(email);

        if (emailLimpio == null) {
            return null;
        }

        return emailLimpio.toLowerCase(Locale.ROOT);
    }

    private record DatosValidados(
            Deporte deporte,
            String deporteOtro,
            Ciudad ciudad,
            String ciudadOtra,
            Barrio barrio,
            String barrioOtro,
            String nombreLugar,
            String direccion,
            String referenciaUbicacion,
            String whatsapp,
            String whatsappNormalizado,
            String instagram,
            String email,
            String observacionesSolicitante,
            List<HorarioValidado> horarios
    ) {
    }

    private record HorarioValidado(
            String diaSemana,
            java.time.LocalTime horaInicio,
            java.time.LocalTime horaFin,
            String observacion
    ) {
    }
}
