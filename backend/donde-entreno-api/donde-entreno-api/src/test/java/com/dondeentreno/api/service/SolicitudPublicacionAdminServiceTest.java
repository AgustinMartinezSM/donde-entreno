package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminDetalleDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminResumenDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAprobacionResponseDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionCambiarEstadoRequestDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.HorarioActividad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.SolicitudPublicacion;
import com.dondeentreno.api.entity.SolicitudPublicacionHorario;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.exception.SolicitudPublicacionInvalidaException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.HorarioActividadRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionHorarioRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionRepository;
import com.dondeentreno.api.repository.UbicacionRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudPublicacionAdminServiceTest {

    @Mock
    private SolicitudPublicacionRepository solicitudPublicacionRepository;

    @Mock
    private SolicitudPublicacionHorarioRepository solicitudPublicacionHorarioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private HorarioActividadRepository horarioActividadRepository;

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Mock
    private UbicacionRepository ubicacionRepository;

    @Mock
    private DeporteRepository deporteRepository;

    @Mock
    private CiudadRepository ciudadRepository;

    @Mock
    private BarrioRepository barrioRepository;

    private SolicitudPublicacionAdminService service;

    @BeforeEach
    void setUp() {
        service = new SolicitudPublicacionAdminService(
                solicitudPublicacionRepository,
                solicitudPublicacionHorarioRepository,
                usuarioRepository,
                actividadRepository,
                horarioActividadRepository,
                perfilPublicadorRepository,
                ubicacionRepository,
                deporteRepository,
                ciudadRepository,
                barrioRepository
        );
    }

    @Test
    void listarSinEstadoUsaRepositoryPaginadoSinEstadoYMapeaResumen() {
        SolicitudPublicacion solicitud = solicitud(10L);
        solicitud.setDeporte(deporte(2L, "Boxeo"));
        when(solicitudPublicacionRepository.findByDeletedAtIsNull(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(solicitud)));

        PaginaResponseDTO<SolicitudPublicacionAdminResumenDTO> response =
                service.listarSolicitudes(null, -1, 100, "antiguos");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(solicitudPublicacionRepository).findByDeletedAtIsNull(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(50, pageable.getPageSize());
        assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("createdAt").getDirection());

        assertEquals(1, response.getContenido().size());
        assertEquals(10L, response.getContenido().get(0).getId());
        assertEquals("Boxeo", response.getContenido().get(0).getDeporteNombre());
        assertEquals(0, response.getPaginaActual());
        assertEquals(1, response.getTotalElementos());
    }

    @Test
    void listarConEstadoPendienteNormalizaEstadoYUsaRepositoryPorEstado() {
        SolicitudPublicacion solicitud = solicitud(11L);
        when(solicitudPublicacionRepository.findByEstadoAndDeletedAtIsNull(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(solicitud)));

        service.listarSolicitudes(" pendiente ", 0, 10, "recientes");

        ArgumentCaptor<String> estadoCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(solicitudPublicacionRepository)
                .findByEstadoAndDeletedAtIsNull(estadoCaptor.capture(), pageableCaptor.capture());

        assertEquals("PENDIENTE", estadoCaptor.getValue());
        assertEquals(Sort.Direction.DESC, pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void listarConEstadoInvalidoLanzaExcepcion() {
        assertThrows(
                SolicitudPublicacionInvalidaException.class,
                () -> service.listarSolicitudes("INVALIDO", 0, 10, "recientes")
        );

        verify(solicitudPublicacionRepository, never()).findByDeletedAtIsNull(any());
        verify(solicitudPublicacionRepository, never()).findByEstadoAndDeletedAtIsNull(any(), any());
    }

    @Test
    void obtenerDetalleExistenteCargaHorariosYDevuelveDTO() {
        SolicitudPublicacion solicitud = solicitud(12L);
        when(solicitudPublicacionRepository.findByIdAndDeletedAtIsNull(12L)).thenReturn(Optional.of(solicitud));
        when(solicitudPublicacionHorarioRepository.findBySolicitudPublicacion_IdOrderByDiaSemanaAscHoraInicioAsc(12L))
                .thenReturn(List.of(horario(1L)));

        SolicitudPublicacionAdminDetalleDTO response = service.obtenerDetalle(12L);

        assertEquals(12L, response.getId());
        assertEquals(1, response.getHorarios().size());
        assertEquals("LUNES", response.getHorarios().get(0).getDiaSemana());
    }

    @Test
    void obtenerDetalleInexistenteLanzaRecursoNoEncontrado() {
        when(solicitudPublicacionRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerDetalle(99L));

        verify(solicitudPublicacionHorarioRepository, never())
                .findBySolicitudPublicacion_IdOrderByDiaSemanaAscHoraInicioAsc(any());
    }

    @Test
    void cambiarEstadoAEnRevisionSeteaCamposYNoTocaActividadGenerada() {
        SolicitudPublicacion solicitud = solicitud(13L);
        Actividad actividadGenerada = new Actividad();
        actividadGenerada.setId(77L);
        solicitud.setActividadGenerada(actividadGenerada);
        solicitud.setMotivoRechazo("Motivo previo");

        Usuario usuario = usuarioRevisor(20L);
        when(solicitudPublicacionRepository.findByIdAndDeletedAtIsNull(13L)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));
        when(solicitudPublicacionRepository.save(any(SolicitudPublicacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(solicitudPublicacionHorarioRepository.findBySolicitudPublicacion_IdOrderByDiaSemanaAscHoraInicioAsc(13L))
                .thenReturn(List.of());

        service.cambiarEstado(
                13L,
                new SolicitudPublicacionCambiarEstadoRequestDTO("EN_REVISION", null),
                20L
        );

        ArgumentCaptor<SolicitudPublicacion> solicitudCaptor = ArgumentCaptor.forClass(SolicitudPublicacion.class);
        verify(solicitudPublicacionRepository).save(solicitudCaptor.capture());
        SolicitudPublicacion solicitudGuardada = solicitudCaptor.getValue();
        assertEquals("EN_REVISION", solicitudGuardada.getEstado());
        assertNotNull(solicitudGuardada.getRevisionIniciadaAt());
        assertNull(solicitudGuardada.getMotivoRechazo());
        assertNull(solicitudGuardada.getRevisionFinalizadaAt());
        assertEquals(solicitudGuardada.getUpdatedAt(), solicitudGuardada.getRevisionIniciadaAt());
        assertSame(usuario, solicitudGuardada.getRevisadoPorUsuario());
        assertSame(actividadGenerada, solicitudGuardada.getActividadGenerada());
    }

    @Test
    void cambiarEstadoARechazadaExigeMotivoYSeteaCampos() {
        SolicitudPublicacion solicitud = solicitud(14L);
        Usuario usuario = usuarioRevisor(21L);
        when(solicitudPublicacionRepository.findByIdAndDeletedAtIsNull(14L)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(21L)).thenReturn(Optional.of(usuario));
        when(solicitudPublicacionRepository.save(any(SolicitudPublicacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(solicitudPublicacionHorarioRepository.findBySolicitudPublicacion_IdOrderByDiaSemanaAscHoraInicioAsc(14L))
                .thenReturn(List.of());

        SolicitudPublicacionAdminDetalleDTO response = service.cambiarEstado(
                14L,
                new SolicitudPublicacionCambiarEstadoRequestDTO(" rechazada ", "  Falta informacion  "),
                21L
        );

        ArgumentCaptor<SolicitudPublicacion> solicitudCaptor = ArgumentCaptor.forClass(SolicitudPublicacion.class);
        verify(solicitudPublicacionRepository).save(solicitudCaptor.capture());
        SolicitudPublicacion solicitudGuardada = solicitudCaptor.getValue();
        assertEquals("RECHAZADA", solicitudGuardada.getEstado());
        assertEquals("Falta informacion", solicitudGuardada.getMotivoRechazo());
        assertNotNull(solicitudGuardada.getRevisionIniciadaAt());
        assertEquals(solicitudGuardada.getUpdatedAt(), solicitudGuardada.getRevisionIniciadaAt());
        assertEquals(solicitudGuardada.getUpdatedAt(), solicitudGuardada.getRevisionFinalizadaAt());
        assertSame(usuario, solicitudGuardada.getRevisadoPorUsuario());
        assertEquals("RECHAZADA", response.getEstado());
    }

    @Test
    void cambiarEstadoARechazadaSinMotivoLanzaExcepcionYNoGuarda() {
        SolicitudPublicacion solicitud = solicitud(15L);
        when(solicitudPublicacionRepository.findByIdAndDeletedAtIsNull(15L)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuarioRevisor(20L)));

        assertThrows(
                SolicitudPublicacionInvalidaException.class,
                () -> service.cambiarEstado(
                        15L,
                        new SolicitudPublicacionCambiarEstadoRequestDTO("RECHAZADA", " "),
                        20L
                )
        );

        verify(solicitudPublicacionRepository, never()).save(any());
    }

    @Test
    void cambiarEstadoAAprobadaLanzaExcepcionYNoCreaActividad() {
        SolicitudPublicacion solicitud = solicitud(16L);
        when(solicitudPublicacionRepository.findByIdAndDeletedAtIsNull(16L)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuarioRevisor(20L)));

        assertThrows(
                SolicitudPublicacionInvalidaException.class,
                () -> service.cambiarEstado(
                        16L,
                        new SolicitudPublicacionCambiarEstadoRequestDTO("APROBADA", null),
                        20L
                )
        );

        verify(solicitudPublicacionRepository, never()).save(any());
        assertNull(solicitud.getActividadGenerada());
    }

    @Test
    void cambiarEstadoConUsuarioRevisorInexistenteLanzaExcepcionYNoGuarda() {
        SolicitudPublicacion solicitud = solicitud(17L);
        when(solicitudPublicacionRepository.findByIdAndDeletedAtIsNull(17L)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                SolicitudPublicacionInvalidaException.class,
                () -> service.cambiarEstado(
                        17L,
                        new SolicitudPublicacionCambiarEstadoRequestDTO("EN_REVISION", null),
                        999L
                )
        );

        verify(solicitudPublicacionRepository, never()).save(any());
    }

    @Test
    void aprobarSolicitudPendienteValidaCreaActividadHorariosYActualizaSolicitud() {
        SolicitudPublicacion solicitud = solicitudAprobable(30L);
        Usuario admin = usuarioRevisor(20L);
        List<SolicitudPublicacionHorario> horarios = List.of(horario(1L));
        prepararAprobacionBasica(solicitud, admin, horarios);
        when(perfilPublicadorRepository
                .findFirstByUsuario_IdAndTipoPublicadorIgnoreCaseAndNombreIgnoreCaseAndActivoTrueAndDeletedAtIsNull(
                        20L,
                        "ESCUELA_DEPORTIVA",
                        "Escuela Norte"
                ))
                .thenReturn(Optional.empty());
        when(perfilPublicadorRepository.save(any(PerfilPublicador.class))).thenAnswer(invocation -> {
            PerfilPublicador perfil = invocation.getArgument(0);
            perfil.setId(100L);
            return perfil;
        });
        when(ubicacionRepository
                .findFirstByPerfilPublicador_IdAndCiudad_IdAndBarrio_IdAndNombreIgnoreCaseAndDireccionIgnoreCaseAndActivaTrueAndDeletedAtIsNull(
                        100L,
                        3L,
                        4L,
                        "Club Norte",
                        "Av. Independencia 1234"
                ))
                .thenReturn(Optional.empty());
        when(ubicacionRepository.save(any(Ubicacion.class))).thenAnswer(invocation -> {
            Ubicacion ubicacion = invocation.getArgument(0);
            ubicacion.setId(200L);
            return ubicacion;
        });
        when(actividadRepository.existsBySlug("boxeo-recreativo")).thenReturn(false);

        SolicitudPublicacionAprobacionResponseDTO response = service.aprobarSolicitud(30L, 20L);

        ArgumentCaptor<PerfilPublicador> perfilCaptor = ArgumentCaptor.forClass(PerfilPublicador.class);
        verify(perfilPublicadorRepository).save(perfilCaptor.capture());
        PerfilPublicador perfilCreado = perfilCaptor.getValue();
        assertSame(admin, perfilCreado.getUsuario());
        assertEquals("Escuela Norte", perfilCreado.getNombre());
        assertEquals("ESCUELA_DEPORTIVA", perfilCreado.getTipoPublicador());
        assertEquals("PENDIENTE_REVISION", perfilCreado.getEstado());
        assertSame(solicitud.getCiudad(), perfilCreado.getCiudadPrincipal());
        assertEquals("contacto@example.com", perfilCreado.getEmailContacto());
        assertEquals("5492235123456", perfilCreado.getWhatsapp());
        assertEquals("5492235123456", perfilCreado.getWhatsappNormalizado());
        assertEquals("@escuelanorte", perfilCreado.getInstagram());
        assertEquals(true, perfilCreado.getActivo());
        assertFalse(perfilCreado.getVerificado());

        ArgumentCaptor<Ubicacion> ubicacionCaptor = ArgumentCaptor.forClass(Ubicacion.class);
        verify(ubicacionRepository).save(ubicacionCaptor.capture());
        Ubicacion ubicacionCreada = ubicacionCaptor.getValue();
        assertSame(perfilCreado, ubicacionCreada.getPerfilPublicador());
        assertEquals("Club Norte", ubicacionCreada.getNombre());
        assertEquals("Av. Independencia 1234", ubicacionCreada.getDireccion());
        assertEquals("Entrada por calle lateral", ubicacionCreada.getReferencia());
        assertEquals(true, ubicacionCreada.getActiva());

        ArgumentCaptor<Actividad> actividadCaptor = ArgumentCaptor.forClass(Actividad.class);
        verify(actividadRepository).save(actividadCaptor.capture());
        Actividad actividadCreada = actividadCaptor.getValue();
        assertSame(perfilCreado, actividadCreada.getPerfilPublicador());
        assertSame(solicitud.getDeporte(), actividadCreada.getDeporte());
        assertSame(ubicacionCreada, actividadCreada.getUbicacion());
        assertEquals("Boxeo recreativo", actividadCreada.getTitulo());
        assertEquals("boxeo-recreativo", actividadCreada.getSlug());
        assertEquals("PUBLICADA", actividadCreada.getEstadoPublicacion());
        assertEquals(true, actividadCreada.getActiva());
        assertEquals(false, actividadCreada.getRequiereInscripcion());
        assertEquals(false, actividadCreada.getCuposLimitados());
        assertEquals("5492235123456", actividadCreada.getWhatsappContacto());

        ArgumentCaptor<Iterable<HorarioActividad>> horariosCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(horarioActividadRepository).saveAll(horariosCaptor.capture());
        HorarioActividad horarioCreado = horariosCaptor.getValue().iterator().next();
        assertSame(actividadCreada, horarioCreado.getActividad());
        assertEquals("LUNES", horarioCreado.getDiaSemana());
        assertEquals(LocalTime.of(18, 0), horarioCreado.getHoraInicio());
        assertEquals(LocalTime.of(19, 30), horarioCreado.getHoraFin());
        assertEquals(true, horarioCreado.getActivo());

        ArgumentCaptor<SolicitudPublicacion> solicitudCaptor = ArgumentCaptor.forClass(SolicitudPublicacion.class);
        verify(solicitudPublicacionRepository).save(solicitudCaptor.capture());
        SolicitudPublicacion solicitudGuardada = solicitudCaptor.getValue();
        assertEquals("APROBADA", solicitudGuardada.getEstado());
        assertSame(actividadCreada, solicitudGuardada.getActividadGenerada());
        assertSame(admin, solicitudGuardada.getRevisadoPorUsuario());
        assertNotNull(solicitudGuardada.getRevisionIniciadaAt());
        assertNotNull(solicitudGuardada.getRevisionFinalizadaAt());
        assertNull(solicitudGuardada.getMotivoRechazo());

        assertEquals(30L, response.getSolicitudId());
        assertEquals("APROBADA", response.getEstado());
        assertEquals(300L, response.getActividadId());
        assertEquals("boxeo-recreativo", response.getActividadSlug());
        assertEquals("Boxeo recreativo", response.getActividadTitulo());
    }

    @Test
    void aprobarSolicitudEnRevisionValidaReutilizaPerfilYUbicacion() {
        SolicitudPublicacion solicitud = solicitudAprobable(31L);
        solicitud.setEstado("EN_REVISION");
        Usuario admin = usuarioRevisor(20L);
        PerfilPublicador perfil = perfilPublicador(100L, admin);
        Ubicacion ubicacion = ubicacion(200L, perfil, solicitud.getCiudad(), solicitud.getBarrio());
        prepararAprobacionBasica(solicitud, admin, List.of(horario(1L)));
        when(perfilPublicadorRepository
                .findFirstByUsuario_IdAndTipoPublicadorIgnoreCaseAndNombreIgnoreCaseAndActivoTrueAndDeletedAtIsNull(
                        20L,
                        "ESCUELA_DEPORTIVA",
                        "Escuela Norte"
                ))
                .thenReturn(Optional.of(perfil));
        when(ubicacionRepository
                .findFirstByPerfilPublicador_IdAndCiudad_IdAndBarrio_IdAndNombreIgnoreCaseAndDireccionIgnoreCaseAndActivaTrueAndDeletedAtIsNull(
                        100L,
                        3L,
                        4L,
                        "Club Norte",
                        "Av. Independencia 1234"
                ))
                .thenReturn(Optional.of(ubicacion));
        when(actividadRepository.existsBySlug("boxeo-recreativo")).thenReturn(false);

        SolicitudPublicacionAprobacionResponseDTO response = service.aprobarSolicitud(31L, 20L);

        verify(perfilPublicadorRepository, never()).save(any());
        verify(ubicacionRepository, never()).save(any());
        assertEquals("APROBADA", response.getEstado());
        assertEquals("boxeo-recreativo", response.getActividadSlug());
    }

    @Test
    void aprobarSolicitudInexistenteLanzaRecursoNoEncontrado() {
        when(solicitudPublicacionRepository.findByIdAndDeletedAtIsNullForUpdate(99L))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.aprobarSolicitud(99L, 20L));

        verify(actividadRepository, never()).save(any());
    }

    @Test
    void aprobarSolicitudRechazadaLanzaExcepcionYNoCreaActividad() {
        SolicitudPublicacion solicitud = solicitudAprobable(32L);
        solicitud.setEstado("RECHAZADA");
        when(solicitudPublicacionRepository.findByIdAndDeletedAtIsNullForUpdate(32L))
                .thenReturn(Optional.of(solicitud));

        assertThrows(SolicitudPublicacionInvalidaException.class, () -> service.aprobarSolicitud(32L, 20L));

        verify(actividadRepository, never()).save(any());
    }

    @Test
    void aprobarSolicitudAprobadaLanzaExcepcionYNoCreaActividad() {
        SolicitudPublicacion solicitud = solicitudAprobable(33L);
        solicitud.setEstado("APROBADA");
        when(solicitudPublicacionRepository.findByIdAndDeletedAtIsNullForUpdate(33L))
                .thenReturn(Optional.of(solicitud));

        assertThrows(SolicitudPublicacionInvalidaException.class, () -> service.aprobarSolicitud(33L, 20L));

        verify(actividadRepository, never()).save(any());
    }

    @Test
    void aprobarSolicitudConActividadGeneradaLanzaExcepcionYNoCreaActividad() {
        SolicitudPublicacion solicitud = solicitudAprobable(34L);
        Actividad actividadGenerada = new Actividad();
        actividadGenerada.setId(777L);
        solicitud.setActividadGenerada(actividadGenerada);
        when(solicitudPublicacionRepository.findByIdAndDeletedAtIsNullForUpdate(34L))
                .thenReturn(Optional.of(solicitud));

        assertThrows(SolicitudPublicacionInvalidaException.class, () -> service.aprobarSolicitud(34L, 20L));

        verify(actividadRepository, never()).save(any());
    }

    @Test
    void aprobarSolicitudConDeporteOtroSinReferenciaRealLanzaExcepcion() {
        SolicitudPublicacion solicitud = solicitudAprobable(35L);
        solicitud.setDeporte(null);
        solicitud.setDeporteOtro("Kickboxing");
        when(solicitudPublicacionRepository.findByIdAndDeletedAtIsNullForUpdate(35L))
                .thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuarioRevisor(20L)));

        assertThrows(SolicitudPublicacionInvalidaException.class, () -> service.aprobarSolicitud(35L, 20L));

        verify(actividadRepository, never()).save(any());
    }

    @Test
    void aprobarSolicitudConCiudadOtraSinReferenciaRealLanzaExcepcion() {
        SolicitudPublicacion solicitud = solicitudAprobable(36L);
        solicitud.setCiudad(null);
        solicitud.setCiudadOtra("Otra ciudad");
        when(solicitudPublicacionRepository.findByIdAndDeletedAtIsNullForUpdate(36L))
                .thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuarioRevisor(20L)));
        when(deporteRepository.findByIdAndActivoTrue(2L)).thenReturn(Optional.of(solicitud.getDeporte()));

        assertThrows(SolicitudPublicacionInvalidaException.class, () -> service.aprobarSolicitud(36L, 20L));

        verify(actividadRepository, never()).save(any());
    }

    @Test
    void aprobarSolicitudConBarrioOtroSinReferenciaRealLanzaExcepcion() {
        SolicitudPublicacion solicitud = solicitudAprobable(37L);
        solicitud.setBarrio(null);
        solicitud.setBarrioOtro("Otra zona");
        when(solicitudPublicacionRepository.findByIdAndDeletedAtIsNullForUpdate(37L))
                .thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuarioRevisor(20L)));
        when(deporteRepository.findByIdAndActivoTrue(2L)).thenReturn(Optional.of(solicitud.getDeporte()));
        when(ciudadRepository.findByIdAndActivaTrue(3L)).thenReturn(Optional.of(solicitud.getCiudad()));

        assertThrows(SolicitudPublicacionInvalidaException.class, () -> service.aprobarSolicitud(37L, 20L));

        verify(actividadRepository, never()).save(any());
    }

    @Test
    void aprobarSolicitudSinHorariosLanzaExcepcionYNoCreaActividad() {
        SolicitudPublicacion solicitud = solicitudAprobable(38L);
        Usuario admin = usuarioRevisor(20L);
        prepararAprobacionBasica(solicitud, admin, List.of());

        assertThrows(SolicitudPublicacionInvalidaException.class, () -> service.aprobarSolicitud(38L, 20L));

        verify(actividadRepository, never()).save(any());
    }

    @Test
    void aprobarSolicitudConUsuarioAdminInexistenteLanzaExcepcionYNoGuarda() {
        SolicitudPublicacion solicitud = solicitudAprobable(39L);
        when(solicitudPublicacionRepository.findByIdAndDeletedAtIsNullForUpdate(39L))
                .thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(SolicitudPublicacionInvalidaException.class, () -> service.aprobarSolicitud(39L, 999L));

        verify(solicitudPublicacionRepository, never()).save(any());
        verify(actividadRepository, never()).save(any());
    }

    @Test
    void aprobarSolicitudConSlugDuplicadoGeneraSlugConSufijo() {
        SolicitudPublicacion solicitud = solicitudAprobable(40L);
        Usuario admin = usuarioRevisor(20L);
        prepararAprobacionBasica(solicitud, admin, List.of(horario(1L)));
        when(perfilPublicadorRepository
                .findFirstByUsuario_IdAndTipoPublicadorIgnoreCaseAndNombreIgnoreCaseAndActivoTrueAndDeletedAtIsNull(
                        20L,
                        "ESCUELA_DEPORTIVA",
                        "Escuela Norte"
                ))
                .thenReturn(Optional.of(perfilPublicador(100L, admin)));
        when(ubicacionRepository
                .findFirstByPerfilPublicador_IdAndCiudad_IdAndBarrio_IdAndNombreIgnoreCaseAndDireccionIgnoreCaseAndActivaTrueAndDeletedAtIsNull(
                        100L,
                        3L,
                        4L,
                        "Club Norte",
                        "Av. Independencia 1234"
                ))
                .thenReturn(Optional.of(ubicacion(200L, perfilPublicador(100L, admin), solicitud.getCiudad(), solicitud.getBarrio())));
        when(actividadRepository.existsBySlug("boxeo-recreativo")).thenReturn(true);
        when(actividadRepository.existsBySlug("boxeo-recreativo-2")).thenReturn(false);

        SolicitudPublicacionAprobacionResponseDTO response = service.aprobarSolicitud(40L, 20L);

        assertEquals("boxeo-recreativo-2", response.getActividadSlug());
    }

    @Test
    void aprobarSolicitudSiFallaGuardarHorariosPropagaExcepcionYNoGuardaSolicitud() {
        SolicitudPublicacion solicitud = solicitudAprobable(41L);
        Usuario admin = usuarioRevisor(20L);
        prepararAprobacionBasica(solicitud, admin, List.of(horario(1L)));
        when(perfilPublicadorRepository
                .findFirstByUsuario_IdAndTipoPublicadorIgnoreCaseAndNombreIgnoreCaseAndActivoTrueAndDeletedAtIsNull(
                        20L,
                        "ESCUELA_DEPORTIVA",
                        "Escuela Norte"
                ))
                .thenReturn(Optional.of(perfilPublicador(100L, admin)));
        when(ubicacionRepository
                .findFirstByPerfilPublicador_IdAndCiudad_IdAndBarrio_IdAndNombreIgnoreCaseAndDireccionIgnoreCaseAndActivaTrueAndDeletedAtIsNull(
                        100L,
                        3L,
                        4L,
                        "Club Norte",
                        "Av. Independencia 1234"
                ))
                .thenReturn(Optional.of(ubicacion(200L, perfilPublicador(100L, admin), solicitud.getCiudad(), solicitud.getBarrio())));
        when(actividadRepository.existsBySlug("boxeo-recreativo")).thenReturn(false);
        doThrow(new RuntimeException("fallo saveAll"))
                .when(horarioActividadRepository)
                .saveAll(any());

        assertThrows(RuntimeException.class, () -> service.aprobarSolicitud(41L, 20L));

        verify(solicitudPublicacionRepository, never()).save(any());
    }

    private void prepararAprobacionBasica(
            SolicitudPublicacion solicitud,
            Usuario admin,
            List<SolicitudPublicacionHorario> horarios
    ) {
        when(solicitudPublicacionRepository.findByIdAndDeletedAtIsNullForUpdate(solicitud.getId()))
                .thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(deporteRepository.findByIdAndActivoTrue(solicitud.getDeporte().getId()))
                .thenReturn(Optional.of(solicitud.getDeporte()));
        when(ciudadRepository.findByIdAndActivaTrue(solicitud.getCiudad().getId()))
                .thenReturn(Optional.of(solicitud.getCiudad()));
        when(barrioRepository.findByIdAndActivoTrueAndCiudad_Id(
                solicitud.getBarrio().getId(),
                solicitud.getCiudad().getId()
        ))
                .thenReturn(Optional.of(solicitud.getBarrio()));
        when(solicitudPublicacionHorarioRepository
                .findBySolicitudPublicacion_IdOrderByDiaSemanaAscHoraInicioAsc(solicitud.getId()))
                .thenReturn(horarios);
        lenient().when(actividadRepository.save(any(Actividad.class))).thenAnswer(invocation -> {
            Actividad actividad = invocation.getArgument(0);
            actividad.setId(300L);
            return actividad;
        });
        lenient().when(solicitudPublicacionRepository.save(any(SolicitudPublicacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private SolicitudPublicacion solicitudAprobable(Long id) {
        SolicitudPublicacion solicitud = solicitud(id);
        Ciudad ciudad = ciudad(3L, "Mar del Plata");
        Barrio barrio = barrio(4L, "Centro", ciudad);

        solicitud.setDeporte(deporteActivo(2L, "Boxeo"));
        solicitud.setCiudad(ciudad);
        solicitud.setBarrio(barrio);
        solicitud.setNombreLugar("Club Norte");
        solicitud.setDireccion("Av. Independencia 1234");
        solicitud.setReferenciaUbicacion("Entrada por calle lateral");
        solicitud.setWhatsappNormalizado("5492235123456");
        solicitud.setInstagram("@escuelanorte");
        return solicitud;
    }

    private Ciudad ciudad(Long id, String nombre) {
        Ciudad ciudad = new Ciudad();
        ciudad.setId(id);
        ciudad.setNombre(nombre);
        ciudad.setProvincia("Buenos Aires");
        ciudad.setPais("Argentina");
        ciudad.setActiva(true);
        return ciudad;
    }

    private Barrio barrio(Long id, String nombre, Ciudad ciudad) {
        Barrio barrio = new Barrio();
        barrio.setId(id);
        barrio.setNombre(nombre);
        barrio.setCiudad(ciudad);
        barrio.setActivo(true);
        return barrio;
    }

    private Deporte deporteActivo(Long id, String nombre) {
        Deporte deporte = deporte(id, nombre);
        deporte.setActivo(true);
        return deporte;
    }

    private PerfilPublicador perfilPublicador(Long id, Usuario usuario) {
        PerfilPublicador perfilPublicador = new PerfilPublicador();
        perfilPublicador.setId(id);
        perfilPublicador.setUsuario(usuario);
        perfilPublicador.setNombre("Escuela Norte");
        perfilPublicador.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfilPublicador.setActivo(true);
        perfilPublicador.setVerificado(false);
        return perfilPublicador;
    }

    private Ubicacion ubicacion(Long id, PerfilPublicador perfilPublicador, Ciudad ciudad, Barrio barrio) {
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setId(id);
        ubicacion.setPerfilPublicador(perfilPublicador);
        ubicacion.setCiudad(ciudad);
        ubicacion.setBarrio(barrio);
        ubicacion.setNombre("Club Norte");
        ubicacion.setDireccion("Av. Independencia 1234");
        ubicacion.setActiva(true);
        return ubicacion;
    }

    private SolicitudPublicacion solicitud(Long id) {
        SolicitudPublicacion solicitud = new SolicitudPublicacion();
        solicitud.setId(id);
        solicitud.setCodigoSeguimiento("DEP-20260630-ABC12345");
        solicitud.setEstado("PENDIENTE");
        solicitud.setOrigen("FORMULARIO_WEB");
        solicitud.setTipoPublicador("ESCUELA_DEPORTIVA");
        solicitud.setNombrePublicador("Escuela Norte");
        solicitud.setNombreActividad("Boxeo recreativo");
        solicitud.setDescripcion("Clases para principiantes.");
        solicitud.setNivel("PRINCIPIANTE");
        solicitud.setEnfoque("RECREATIVO");
        solicitud.setModalidad("PRESENCIAL");
        solicitud.setMostrarPrecio(true);
        solicitud.setEmail("contacto@example.com");
        solicitud.setWhatsapp("+54 9 223 512-3456");
        solicitud.setCreatedAt(OffsetDateTime.parse("2026-06-01T10:00:00-03:00"));
        solicitud.setUpdatedAt(OffsetDateTime.parse("2026-06-01T10:00:00-03:00"));
        solicitud.setAceptaCondiciones(true);
        return solicitud;
    }

    private SolicitudPublicacionHorario horario(Long id) {
        SolicitudPublicacionHorario horario = new SolicitudPublicacionHorario();
        horario.setId(id);
        horario.setDiaSemana("LUNES");
        horario.setHoraInicio(LocalTime.of(18, 0));
        horario.setHoraFin(LocalTime.of(19, 30));
        horario.setObservacion("Salon 1");
        return horario;
    }

    private Deporte deporte(Long id, String nombre) {
        Deporte deporte = new Deporte();
        deporte.setId(id);
        deporte.setNombre(nombre);
        return deporte;
    }

    private Usuario usuarioRevisor(Long id) {
        Rol rol = new Rol();
        rol.setId(1L);
        rol.setNombre("SUPER_ADMIN");

        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Admin");
        usuario.setApellido("Principal");
        usuario.setEmail("admin@example.com");
        usuario.setPasswordHash("hash-ficticio");
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        return usuario;
    }
}
