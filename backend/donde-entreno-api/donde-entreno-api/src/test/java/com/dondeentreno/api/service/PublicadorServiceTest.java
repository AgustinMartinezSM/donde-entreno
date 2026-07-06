package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionHorarioRequestDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionResponseDTO;
import com.dondeentreno.api.dto.SolicitudPublicadorDetalleDTO;
import com.dondeentreno.api.dto.SolicitudPublicadorRequestDTO;
import com.dondeentreno.api.dto.SolicitudPublicadorResumenDTO;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.SolicitudPublicacion;
import com.dondeentreno.api.entity.SolicitudPublicacionHorario;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.exception.SolicitudPublicacionInvalidaException;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
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

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicadorServiceTest {

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Mock
    private SolicitudPublicacionRepository solicitudPublicacionRepository;

    @Mock
    private SolicitudPublicacionHorarioRepository solicitudPublicacionHorarioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private DeporteRepository deporteRepository;

    @Mock
    private CiudadRepository ciudadRepository;

    @Mock
    private BarrioRepository barrioRepository;

    private PublicadorService service;

    @BeforeEach
    void setUp() {
        SolicitudPublicacionService solicitudPublicacionService = new SolicitudPublicacionService(
                solicitudPublicacionRepository,
                solicitudPublicacionHorarioRepository,
                deporteRepository,
                ciudadRepository,
                barrioRepository
        );
        service = new PublicadorService(
                perfilPublicadorRepository,
                solicitudPublicacionRepository,
                solicitudPublicacionHorarioRepository,
                usuarioRepository,
                solicitudPublicacionService
        );
    }

    @Test
    void listarMisSolicitudesSinEstadoFiltraPorUsuarioYPerfil() {
        Usuario usuario = usuario();
        PerfilPublicador perfil = perfil(usuario);
        SolicitudPublicacion solicitud = solicitudPropia();
        configurarContexto(usuario, perfil);
        when(solicitudPublicacionRepository.findByUsuario_IdAndPerfilPublicador_IdAndDeletedAtIsNull(
                eq(10L),
                eq(30L),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(solicitud)));

        PaginaResponseDTO<SolicitudPublicadorResumenDTO> response =
                service.listarMisSolicitudes(10L, null, -1, 100, null);

        assertEquals(1, response.getContenido().size());
        assertEquals("Boxeo recreativo", response.getContenido().get(0).getNombreActividad());
        assertEquals(0, response.getPaginaActual());
        assertEquals(1, response.getTamanioPagina());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(solicitudPublicacionRepository).findByUsuario_IdAndPerfilPublicador_IdAndDeletedAtIsNull(
                eq(10L),
                eq(30L),
                pageableCaptor.capture()
        );
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(50, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void listarMisSolicitudesPorEstadoNormalizaEstado() {
        Usuario usuario = usuario();
        PerfilPublicador perfil = perfil(usuario);
        configurarContexto(usuario, perfil);
        when(solicitudPublicacionRepository.findByUsuario_IdAndPerfilPublicador_IdAndEstadoAndDeletedAtIsNull(
                eq(10L),
                eq(30L),
                eq("PENDIENTE"),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        service.listarMisSolicitudes(10L, " pendiente ", 0, 10, "antiguos");

        verify(solicitudPublicacionRepository).findByUsuario_IdAndPerfilPublicador_IdAndEstadoAndDeletedAtIsNull(
                eq(10L),
                eq(30L),
                eq("PENDIENTE"),
                any(Pageable.class)
        );
    }

    @Test
    void listarMisSolicitudesConEstadoInvalidoLanzaExcepcion() {
        configurarContexto(usuario(), perfil(usuario()));

        assertThrows(
                SolicitudPublicacionInvalidaException.class,
                () -> service.listarMisSolicitudes(10L, "PUBLICADA", 0, 10, null)
        );

        verify(solicitudPublicacionRepository, never())
                .findByUsuario_IdAndPerfilPublicador_IdAndEstadoAndDeletedAtIsNull(
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void obtenerMiSolicitudDevuelveDetalleConHorarios() {
        Usuario usuario = usuario();
        PerfilPublicador perfil = perfil(usuario);
        SolicitudPublicacion solicitud = solicitudPropia();
        configurarContexto(usuario, perfil);
        when(solicitudPublicacionRepository.findByIdAndUsuario_IdAndPerfilPublicador_IdAndDeletedAtIsNull(
                40L,
                10L,
                30L
        )).thenReturn(Optional.of(solicitud));
        when(solicitudPublicacionHorarioRepository.findBySolicitudPublicacion_IdOrderByDiaSemanaAscHoraInicioAsc(40L))
                .thenReturn(List.of(horario()));

        SolicitudPublicadorDetalleDTO detalle = service.obtenerMiSolicitud(10L, 40L);

        assertEquals(40L, detalle.getId());
        assertEquals(1, detalle.getHorarios().size());
        assertEquals("LUNES", detalle.getHorarios().get(0).getDiaSemana());
    }

    @Test
    void obtenerMiSolicitudAjenaOInexistenteDevuelveNotFound() {
        Usuario usuario = usuario();
        PerfilPublicador perfil = perfil(usuario);
        configurarContexto(usuario, perfil);
        when(solicitudPublicacionRepository.findByIdAndUsuario_IdAndPerfilPublicador_IdAndDeletedAtIsNull(
                999L,
                10L,
                30L
        )).thenReturn(Optional.empty());

        RecursoNoEncontradoException exception = assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.obtenerMiSolicitud(10L, 999L)
        );

        assertEquals("Solicitud no encontrada.", exception.getMessage());
    }

    @Test
    void crearMiSolicitudValidaAsociaUsuarioPerfilYGuardaHorarios() {
        Usuario usuario = usuario();
        PerfilPublicador perfil = perfil(usuario);
        SolicitudPublicadorRequestDTO request = requestValido();
        Ciudad ciudad = ciudadActiva(2L);
        Barrio barrio = barrioActivo(3L, ciudad);
        configurarContexto(usuario, perfil);
        when(deporteRepository.findById(1L)).thenReturn(Optional.of(deporteActivo(1L)));
        when(ciudadRepository.findById(2L)).thenReturn(Optional.of(ciudad));
        when(barrioRepository.findById(3L)).thenReturn(Optional.of(barrio));
        when(solicitudPublicacionRepository.existsByCodigoSeguimiento(anyString())).thenReturn(false);
        when(solicitudPublicacionRepository.save(any(SolicitudPublicacion.class))).thenAnswer(invocation -> {
            SolicitudPublicacion solicitud = invocation.getArgument(0);
            solicitud.setId(40L);
            return solicitud;
        });
        when(solicitudPublicacionHorarioRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SolicitudPublicacionResponseDTO response = service.crearMiSolicitud(10L, request);

        assertEquals(40L, response.getId());
        assertEquals("PENDIENTE", response.getEstado());

        ArgumentCaptor<SolicitudPublicacion> solicitudCaptor =
                ArgumentCaptor.forClass(SolicitudPublicacion.class);
        verify(solicitudPublicacionRepository).save(solicitudCaptor.capture());
        SolicitudPublicacion solicitud = solicitudCaptor.getValue();
        assertSame(usuario, solicitud.getUsuario());
        assertSame(perfil, solicitud.getPerfilPublicador());
        assertEquals("PROFESOR_INDEPENDIENTE", solicitud.getTipoPublicador());
        assertEquals("Perfil Publicador", solicitud.getNombrePublicador());
        assertEquals("PENDIENTE", solicitud.getEstado());
        assertEquals("FORMULARIO_WEB", solicitud.getOrigen());
        assertEquals("+54 9 223 555-9999", solicitud.getWhatsapp());
        assertEquals("5492235559999", solicitud.getWhatsappNormalizado());
        assertEquals("contacto@perfil.test", solicitud.getEmail());

        ArgumentCaptor<Iterable<SolicitudPublicacionHorario>> horariosCaptor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(solicitudPublicacionHorarioRepository).saveAll(horariosCaptor.capture());
        List<SolicitudPublicacionHorario> horarios = (List<SolicitudPublicacionHorario>) horariosCaptor.getValue();
        assertEquals(1, horarios.size());
        assertSame(solicitud, horarios.get(0).getSolicitudPublicacion());
    }

    @Test
    void crearMiSolicitudSinPerfilDevuelveErrorControlado() {
        Usuario usuario = usuario();
        when(usuarioRepository.findByIdAndActivoTrueAndDeletedAtIsNull(10L)).thenReturn(Optional.of(usuario));
        when(perfilPublicadorRepository.findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(10L))
                .thenReturn(Optional.empty());

        RecursoNoEncontradoException exception = assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.crearMiSolicitud(10L, requestValido())
        );

        assertEquals("Perfil publicador no encontrado.", exception.getMessage());
        verify(solicitudPublicacionRepository, never()).save(any());
    }

    @Test
    void crearMiSolicitudInvalidaNoGuarda() {
        SolicitudPublicadorRequestDTO request = requestValido();
        request.setDeporteOtro("Boxeo otro");
        configurarContexto(usuario(), perfil(usuario()));

        assertThrows(
                SolicitudPublicacionInvalidaException.class,
                () -> service.crearMiSolicitud(10L, request)
        );

        verify(solicitudPublicacionRepository, never()).save(any());
        verify(solicitudPublicacionHorarioRepository, never()).saveAll(any());
    }

    private void configurarContexto(Usuario usuario, PerfilPublicador perfil) {
        when(usuarioRepository.findByIdAndActivoTrueAndDeletedAtIsNull(usuario.getId())).thenReturn(Optional.of(usuario));
        when(perfilPublicadorRepository.findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(usuario.getId()))
                .thenReturn(Optional.of(perfil));
    }

    private SolicitudPublicadorRequestDTO requestValido() {
        SolicitudPublicadorRequestDTO request = new SolicitudPublicadorRequestDTO();
        request.setNombreActividad("Boxeo recreativo");
        request.setDeporteId(1L);
        request.setDescripcion("Clases para principiantes.");
        request.setNivel("PRINCIPIANTE");
        request.setEnfoque("RECREATIVO");
        request.setModalidad("PRESENCIAL");
        request.setEdadMinima(18);
        request.setEdadMaxima(60);
        request.setPrecioReferencia(new BigDecimal("18000.00"));
        request.setMostrarPrecio(true);
        request.setCiudadId(2L);
        request.setBarrioId(3L);
        request.setNombreLugar("Escuela Norte");
        request.setDireccion("Av. Independencia 1234");
        request.setAceptaCondiciones(true);
        request.setHorarios(List.of(new SolicitudPublicacionHorarioRequestDTO(
                "LUNES",
                LocalTime.of(18, 0),
                LocalTime.of(19, 30),
                null
        )));
        return request;
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setEmail("usuario@perfil.test");
        usuario.setActivo(true);
        usuario.setDeletedAt(null);
        return usuario;
    }

    private PerfilPublicador perfil(Usuario usuario) {
        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setId(30L);
        perfil.setUsuario(usuario);
        perfil.setNombre("Perfil Publicador");
        perfil.setTipoPublicador("PROFESOR_INDEPENDIENTE");
        perfil.setWhatsapp("+54 9 223 555-9999");
        perfil.setWhatsappNormalizado("5492235559999");
        perfil.setEmailContacto("contacto@perfil.test");
        perfil.setActivo(true);
        return perfil;
    }

    private SolicitudPublicacion solicitudPropia() {
        SolicitudPublicacion solicitud = new SolicitudPublicacion();
        solicitud.setId(40L);
        solicitud.setCodigoSeguimiento("DEP-20260704-ABC12345");
        solicitud.setEstado("PENDIENTE");
        solicitud.setNombreActividad("Boxeo recreativo");
        solicitud.setDescripcion("Clases para principiantes.");
        solicitud.setCreatedAt(OffsetDateTime.parse("2026-07-04T10:00:00-03:00"));
        solicitud.setUpdatedAt(OffsetDateTime.parse("2026-07-04T10:00:00-03:00"));
        return solicitud;
    }

    private SolicitudPublicacionHorario horario() {
        SolicitudPublicacionHorario horario = new SolicitudPublicacionHorario();
        horario.setId(50L);
        horario.setDiaSemana("LUNES");
        horario.setHoraInicio(LocalTime.of(18, 0));
        horario.setHoraFin(LocalTime.of(19, 30));
        return horario;
    }

    private Deporte deporteActivo(Long id) {
        Deporte deporte = new Deporte();
        deporte.setId(id);
        deporte.setActivo(true);
        return deporte;
    }

    private Ciudad ciudadActiva(Long id) {
        Ciudad ciudad = new Ciudad();
        ciudad.setId(id);
        ciudad.setActiva(true);
        return ciudad;
    }

    private Barrio barrioActivo(Long id, Ciudad ciudad) {
        Barrio barrio = new Barrio();
        barrio.setId(id);
        barrio.setActivo(true);
        barrio.setCiudad(ciudad);
        return barrio;
    }
}
