package com.dondeentreno.api.integration;

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
import com.dondeentreno.api.service.SolicitudPublicacionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("integration-local")
class SolicitudPublicacionIT {

    private static final String ESTADO_PENDIENTE = "PENDIENTE";
    private static final String ORIGEN_FORMULARIO_WEB = "FORMULARIO_WEB";

    @Autowired
    private Environment environment;

    @Autowired
    private SolicitudPublicacionService solicitudPublicacionService;

    @Autowired
    private SolicitudPublicacionRepository solicitudPublicacionRepository;

    @Autowired
    private SolicitudPublicacionHorarioRepository solicitudPublicacionHorarioRepository;

    @Autowired
    private DeporteRepository deporteRepository;

    @Autowired
    private CiudadRepository ciudadRepository;

    @Autowired
    private BarrioRepository barrioRepository;

    private Long createdSolicitudId;
    private String createdCodigoSeguimiento;

    @BeforeEach
    void verificarDatasourceLocal() {
        String url = environment.getProperty("spring.datasource.url", "");

        assertTrue(esDatasourceLocal(url), "El perfil integration-local debe apuntar solo a localhost o 127.0.0.1.");
        assertFalse(esDatasourceRemoto(url), "El perfil integration-local no debe apuntar a Supabase, Render ni hosts remotos.");
    }

    @AfterEach
    void limpiarDatosCreadosPorElTest() {
        if (createdSolicitudId == null) {
            return;
        }

        List<SolicitudPublicacionHorario> horarios =
                solicitudPublicacionHorarioRepository.findBySolicitudPublicacion_Id(createdSolicitudId);
        solicitudPublicacionHorarioRepository.deleteAll(horarios);
        solicitudPublicacionRepository.findById(createdSolicitudId)
                .ifPresent(solicitudPublicacionRepository::delete);

        if (createdCodigoSeguimiento != null) {
            assertFalse(solicitudPublicacionRepository.existsByCodigoSeguimiento(createdCodigoSeguimiento));
        }

        createdSolicitudId = null;
        createdCodigoSeguimiento = null;
    }

    @Test
    void crearSolicitudValidaPersisteSolicitudYHorarios() {
        Referencias referencias = obtenerReferenciasActivas();
        SolicitudPublicacionRequestDTO request = crearRequestValido(
                referencias,
                null,
                List.of(
                        new SolicitudPublicacionHorarioRequestDTO(
                                "LUNES",
                                LocalTime.of(8, 30),
                                LocalTime.of(10, 0),
                                "Cancha auxiliar"
                        ),
                        new SolicitudPublicacionHorarioRequestDTO(
                                "MIERCOLES",
                                LocalTime.of(18, 0),
                                LocalTime.of(19, 30),
                                "Grupo inicial"
                        )
                )
        );

        SolicitudPublicacionResponseDTO response = solicitudPublicacionService.crearSolicitud(request);
        createdSolicitudId = response.getId();
        createdCodigoSeguimiento = response.getCodigoSeguimiento();

        assertNotNull(response.getId());
        assertNotNull(response.getCodigoSeguimiento());
        assertTrue(response.getCodigoSeguimiento().matches("DEP-\\d{8}-[A-Z0-9]{8}"));
        assertEquals(ESTADO_PENDIENTE, response.getEstado());
        assertNotNull(response.getCreatedAt());

        SolicitudPublicacion solicitud = solicitudPublicacionRepository.findById(response.getId()).orElseThrow();
        List<SolicitudPublicacionHorario> horarios =
                solicitudPublicacionHorarioRepository.findBySolicitudPublicacion_Id(response.getId());

        assertEquals(ESTADO_PENDIENTE, solicitud.getEstado());
        assertEquals(ORIGEN_FORMULARIO_WEB, solicitud.getOrigen());
        assertEquals(Boolean.FALSE, solicitud.getMostrarPrecio());
        assertEquals("test.publicacion+it@dondeentreno.com", solicitud.getEmail());
        assertEquals("5492235550000", solicitud.getWhatsappNormalizado());
        assertEquals(2, horarios.size());

        horarios.forEach(horario -> assertEquals(solicitud.getId(), horario.getSolicitudPublicacion().getId()));
        assertNull(solicitud.getIpOrigen());
        assertNull(solicitud.getRevisadoPorUsuario());
        assertNull(solicitud.getMotivoRechazo());
        assertNull(solicitud.getObservacionesRevision());
        assertNull(solicitud.getRevisionIniciadaAt());
        assertNull(solicitud.getRevisionFinalizadaAt());
        assertNull(solicitud.getActividadGenerada());
    }

    @Test
    void horarioConHoraInicioMayorOIgualAHoraFinNoPersisteDatos() {
        Referencias referencias = obtenerReferenciasActivas();
        long solicitudesAntes = solicitudPublicacionRepository.count();
        long horariosAntes = solicitudPublicacionHorarioRepository.count();
        SolicitudPublicacionRequestDTO request = crearRequestValido(
                referencias,
                Boolean.FALSE,
                List.of(new SolicitudPublicacionHorarioRequestDTO(
                        "MARTES",
                        LocalTime.of(10, 0),
                        LocalTime.of(10, 0),
                        null
                ))
        );

        assertThrows(SolicitudPublicacionInvalidaException.class, () -> solicitudPublicacionService.crearSolicitud(request));

        assertEquals(solicitudesAntes, solicitudPublicacionRepository.count());
        assertEquals(horariosAntes, solicitudPublicacionHorarioRepository.count());
    }

    @Test
    void errorFisicoAlPersistirHorarioHaceRollbackDeLaSolicitud() {
        Referencias referencias = obtenerReferenciasActivas();
        long solicitudesAntes = solicitudPublicacionRepository.count();
        long horariosAntes = solicitudPublicacionHorarioRepository.count();
        String observacionDemasiadoLarga = "x".repeat(300);
        SolicitudPublicacionRequestDTO request = crearRequestValido(
                referencias,
                Boolean.FALSE,
                List.of(new SolicitudPublicacionHorarioRequestDTO(
                        "JUEVES",
                        LocalTime.of(12, 0),
                        LocalTime.of(13, 0),
                        observacionDemasiadoLarga
                ))
        );

        assertThrows(RuntimeException.class, () -> solicitudPublicacionService.crearSolicitud(request));

        assertEquals(solicitudesAntes, solicitudPublicacionRepository.count());
        assertEquals(horariosAntes, solicitudPublicacionHorarioRepository.count());
    }

    private SolicitudPublicacionRequestDTO crearRequestValido(
            Referencias referencias,
            Boolean mostrarPrecio,
            List<SolicitudPublicacionHorarioRequestDTO> horarios
    ) {
        return new SolicitudPublicacionRequestDTO(
                "CLUB",
                "Club Integracion Local",
                "Entrenamiento funcional IT",
                referencias.deporte().getId(),
                null,
                "Actividad creada desde un test de integracion local.",
                "TODOS",
                "MIXTO",
                "PRESENCIAL",
                18,
                65,
                new BigDecimal("12500.00"),
                mostrarPrecio,
                referencias.ciudad().getId(),
                null,
                referencias.barrio().getId(),
                null,
                "Sede local de prueba",
                "Calle Falsa 123",
                "Ingreso por recepcion",
                "+54 9 223-555-0000",
                "@dondeentreno_it",
                "TEST.PUBLICACION+IT@DONDEENTRENO.COM",
                "Solicitud de prueba local.",
                Boolean.TRUE,
                horarios
        );
    }

    private Referencias obtenerReferenciasActivas() {
        Deporte deporte = deporteRepository.findByActivoTrue().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para el test de integracion."));

        return ciudadRepository.findByActivaTrueOrderByNombreAsc().stream()
                .map(ciudad -> new Referencias(
                        deporte,
                        ciudad,
                        barrioRepository.findByActivoTrueAndCiudad_IdOrderByNombreAsc(ciudad.getId()).stream()
                                .findFirst()
                                .orElse(null)
                ))
                .filter(referencias -> referencias.barrio() != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay una ciudad activa con barrio activo para el test de integracion."
                ));
    }

    private boolean esDatasourceLocal(String url) {
        return url.matches("^jdbc:postgresql://(localhost|127\\.0\\.0\\.1)(:[0-9]+)?/.*");
    }

    private boolean esDatasourceRemoto(String url) {
        return url.toLowerCase().matches(".*(supabase|render|amazonaws|azure|neon|railway|aiven|digitalocean|\\.com|\\.net|\\.io|\\.app).*");
    }

    private record Referencias(Deporte deporte, Ciudad ciudad, Barrio barrio) {
    }
}
