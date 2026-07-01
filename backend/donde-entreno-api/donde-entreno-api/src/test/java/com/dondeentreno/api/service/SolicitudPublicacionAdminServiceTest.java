package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminDetalleDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminResumenDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionCambiarEstadoRequestDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.SolicitudPublicacion;
import com.dondeentreno.api.entity.SolicitudPublicacionHorario;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.exception.SolicitudPublicacionInvalidaException;
import com.dondeentreno.api.repository.SolicitudPublicacionHorarioRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    private SolicitudPublicacionAdminService service;

    @BeforeEach
    void setUp() {
        service = new SolicitudPublicacionAdminService(
                solicitudPublicacionRepository,
                solicitudPublicacionHorarioRepository,
                usuarioRepository
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
