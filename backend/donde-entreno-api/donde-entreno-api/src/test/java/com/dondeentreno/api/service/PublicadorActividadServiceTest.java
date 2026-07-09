package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.PublicadorActividadDetalleDTO;
import com.dondeentreno.api.dto.PublicadorActividadResumenDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.CategoriaDeportiva;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.HorarioActividad;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.SolicitudPublicacion;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.HorarioActividadRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicadorActividadServiceTest {

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private HorarioActividadRepository horarioActividadRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private SolicitudPublicacionRepository solicitudPublicacionRepository;

    private PublicadorActividadService service;

    @BeforeEach
    void setUp() {
        service = new PublicadorActividadService(
                perfilPublicadorRepository,
                actividadRepository,
                horarioActividadRepository,
                imagenRepository,
                solicitudPublicacionRepository
        );
    }

    @Test
    void listarActividadesPropiasObtienePerfilFiltraPorPerfilYMapeaResumen() {
        PerfilPublicador perfil = perfilPublicador();
        Actividad actividad = actividad(perfil);
        configurarPerfil(perfil);
        when(actividadRepository.findByPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                eq(30L),
                eq("PUBLICADA"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(actividad),
                PageRequest.of(0, 20),
                1
        ));
        when(imagenRepository.findByActivaTrueAndActividad_IdOrderByOrdenAsc(100L))
                .thenReturn(List.of(
                        imagen(301L, "GALERIA", "https://img.test/galeria.jpg", 1),
                        imagen(302L, "PRINCIPAL", "https://img.test/principal.jpg", 2)
                ));

        PaginaResponseDTO<PublicadorActividadResumenDTO> response =
                service.listarMisActividades(10L, -1, 100, "antiguos");

        assertEquals(1, response.getContenido().size());
        assertEquals("Boxeo recreativo", response.getContenido().get(0).getTitulo());
        assertEquals("boxeo-recreativo", response.getContenido().get(0).getSlugPublico());
        assertEquals("https://img.test/principal.jpg", response.getContenido().get(0).getImagenPrincipalUrl());
        assertEquals(0, response.getPaginaActual());
        assertEquals(20, response.getTamanioPagina());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(actividadRepository).findByPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                eq(30L),
                eq("PUBLICADA"),
                pageableCaptor.capture()
        );
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(50, pageableCaptor.getValue().getPageSize());
        Sort.Order createdAtOrder = pageableCaptor.getValue().getSort().getOrderFor("createdAt");
        assertEquals(Sort.Direction.ASC, createdAtOrder.getDirection());
    }

    @Test
    void listarSinActividadesDevuelvePaginaVacia() {
        PerfilPublicador perfil = perfilPublicador();
        configurarPerfil(perfil);
        when(actividadRepository.findByPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                eq(30L),
                eq("PUBLICADA"),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        PaginaResponseDTO<PublicadorActividadResumenDTO> response =
                service.listarMisActividades(10L, 0, 10, "recientes");

        assertTrue(response.getContenido().isEmpty());
        verifyNoInteractions(imagenRepository);
    }

    @Test
    void listarConPerfilInexistenteLanzaRecursoNoEncontrado() {
        when(perfilPublicadorRepository.findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(10L))
                .thenReturn(Optional.empty());

        RecursoNoEncontradoException exception = assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.listarMisActividades(10L, 0, 10, null)
        );

        assertEquals("Perfil publicador no encontrado.", exception.getMessage());
        verifyNoInteractions(actividadRepository, imagenRepository);
    }

    @Test
    void obtenerDetallePropioCargaHorariosImagenesYSolicitudOrigen() {
        PerfilPublicador perfil = perfilPublicador();
        Actividad actividad = actividad(perfil);
        configurarPerfil(perfil);
        when(actividadRepository.findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                100L,
                30L,
                "PUBLICADA"
        )).thenReturn(Optional.of(actividad));
        when(horarioActividadRepository.findByActivoTrueAndActividad_IdOrderByDiaSemanaAscHoraInicioAsc(100L))
                .thenReturn(List.of(horario()));
        when(imagenRepository.findByActivaTrueAndActividad_IdOrderByOrdenAsc(100L))
                .thenReturn(List.of(imagen(301L, "PRINCIPAL", "https://img.test/principal.jpg", 1)));
        SolicitudPublicacion solicitud = new SolicitudPublicacion();
        solicitud.setId(400L);
        solicitud.setCodigoSeguimiento("DEP-20260704-ABC12345");
        when(solicitudPublicacionRepository.findByActividadGenerada_IdAndPerfilPublicador_IdAndDeletedAtIsNull(
                100L,
                30L
        )).thenReturn(Optional.of(solicitud));

        PublicadorActividadDetalleDTO detalle = service.obtenerMiActividad(10L, 100L);

        assertEquals(100L, detalle.getId());
        assertEquals("Boxeo recreativo", detalle.getTitulo());
        assertEquals(1, detalle.getHorarios().size());
        assertEquals("LUNES", detalle.getHorarios().get(0).getDiaSemana());
        assertEquals(1, detalle.getImagenes().size());
        assertEquals("https://img.test/principal.jpg", detalle.getImagenPrincipalUrl());
        assertEquals(400L, detalle.getSolicitudOrigenId());
        assertEquals("DEP-20260704-ABC12345", detalle.getSolicitudCodigoSeguimiento());

        verify(actividadRepository, never()).save(any(Actividad.class));
    }

    @Test
    void obtenerDetalleAjenoOInexistenteDevuelveNotFound() {
        PerfilPublicador perfil = perfilPublicador();
        configurarPerfil(perfil);
        when(actividadRepository.findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                999L,
                30L,
                "PUBLICADA"
        )).thenReturn(Optional.empty());

        RecursoNoEncontradoException exception = assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.obtenerMiActividad(10L, 999L)
        );

        assertEquals("Actividad no encontrada.", exception.getMessage());
        verifyNoInteractions(horarioActividadRepository, imagenRepository, solicitudPublicacionRepository);
    }

    @Test
    void obtenerDetalleSinImagenesDejaImagenPrincipalNull() {
        PerfilPublicador perfil = perfilPublicador();
        Actividad actividad = actividad(perfil);
        configurarPerfil(perfil);
        when(actividadRepository.findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                100L,
                30L,
                "PUBLICADA"
        )).thenReturn(Optional.of(actividad));
        when(horarioActividadRepository.findByActivoTrueAndActividad_IdOrderByDiaSemanaAscHoraInicioAsc(100L))
                .thenReturn(List.of());
        when(imagenRepository.findByActivaTrueAndActividad_IdOrderByOrdenAsc(100L))
                .thenReturn(List.of());
        when(solicitudPublicacionRepository.findByActividadGenerada_IdAndPerfilPublicador_IdAndDeletedAtIsNull(
                100L,
                30L
        )).thenReturn(Optional.empty());

        PublicadorActividadDetalleDTO detalle = service.obtenerMiActividad(10L, 100L);

        assertTrue(detalle.getHorarios().isEmpty());
        assertTrue(detalle.getImagenes().isEmpty());
        assertNull(detalle.getImagenPrincipalUrl());
        assertNull(detalle.getSolicitudOrigenId());
    }

    private void configurarPerfil(PerfilPublicador perfil) {
        when(perfilPublicadorRepository.findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(perfil));
    }

    private PerfilPublicador perfilPublicador() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);

        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setId(30L);
        perfil.setUsuario(usuario);
        perfil.setNombre("Perfil Publicador");
        perfil.setTipoPublicador("PROFESOR_INDEPENDIENTE");
        perfil.setActivo(true);
        perfil.setDeletedAt(null);
        return perfil;
    }

    private Actividad actividad(PerfilPublicador perfil) {
        CategoriaDeportiva categoria = new CategoriaDeportiva();
        categoria.setId(5L);
        categoria.setNombre("Combate");
        categoria.setSlug("combate");

        Deporte deporte = new Deporte();
        deporte.setId(1L);
        deporte.setNombre("Boxeo");
        deporte.setSlug("boxeo");
        deporte.setCategoriaDeportiva(categoria);

        Ciudad ciudad = new Ciudad();
        ciudad.setId(2L);
        ciudad.setNombre("Mar del Plata");
        ciudad.setSlug("mar-del-plata");

        Barrio barrio = new Barrio();
        barrio.setId(3L);
        barrio.setNombre("Centro");

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setId(4L);
        ubicacion.setNombre("Escuela Norte");
        ubicacion.setDireccion("Av. Independencia 1234");
        ubicacion.setReferencia("Planta alta");
        ubicacion.setCiudad(ciudad);
        ubicacion.setBarrio(barrio);
        ubicacion.setPerfilPublicador(perfil);

        Actividad actividad = new Actividad();
        actividad.setId(100L);
        actividad.setPerfilPublicador(perfil);
        actividad.setDeporte(deporte);
        actividad.setUbicacion(ubicacion);
        actividad.setTitulo("Boxeo recreativo");
        actividad.setSlug("boxeo-recreativo");
        actividad.setDescripcion("Clases para principiantes.");
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(60);
        actividad.setPrecioReferencia(new BigDecimal("18000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 512-3456");
        actividad.setInstagramContacto("@escuelanorte");
        actividad.setEmailContacto("contacto@ejemplo.com");
        actividad.setEstadoPublicacion("PUBLICADA");
        actividad.setActiva(true);
        actividad.setDeletedAt(null);
        actividad.setCreatedAt(OffsetDateTime.parse("2026-07-04T10:00:00-03:00"));
        return actividad;
    }

    private HorarioActividad horario() {
        HorarioActividad horario = new HorarioActividad();
        horario.setId(200L);
        horario.setDiaSemana("LUNES");
        horario.setHoraInicio(LocalTime.of(18, 0));
        horario.setHoraFin(LocalTime.of(19, 30));
        horario.setObservacion("Traer guantes");
        return horario;
    }

    private Imagen imagen(Long id, String tipo, String url, int orden) {
        Imagen imagen = new Imagen();
        imagen.setId(id);
        imagen.setTipoImagen(tipo);
        imagen.setUrl(url);
        imagen.setTitulo("Imagen " + id);
        imagen.setDescripcion("Descripcion " + id);
        imagen.setOrden(orden);
        imagen.setActiva(true);
        return imagen;
    }
}
