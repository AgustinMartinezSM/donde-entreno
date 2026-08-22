package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.SolicitudCambioActividadRequestDTO;
import com.dondeentreno.api.dto.SolicitudCambioDetalleDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.SolicitudCambioActividad;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.exception.SolicitudCambioConflictoException;
import com.dondeentreno.api.exception.SolicitudCambioInvalidaException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SolicitudCambioActividadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudCambioActividadServiceTest {

    @Mock
    private SolicitudCambioActividadRepository solicitudCambioRepository;

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private com.dondeentreno.api.repository.DeporteRepository deporteRepository;

    @Mock
    private com.dondeentreno.api.repository.BarrioRepository barrioRepository;

    private SolicitudCambioActividadService service;

    @BeforeEach
    void setUp() {
        service = new SolicitudCambioActividadService(
                solicitudCambioRepository,
                perfilPublicadorRepository,
                actividadRepository,
                deporteRepository,
                barrioRepository
        );
    }

    @Test
    void crearSolicitudNormalizaCamposYGuardaPendiente() {
        PerfilPublicador perfil = perfil();
        Actividad actividad = actividad();
        configurarPerfil(perfil);
        configurarActividadPropia(actividad, perfil);
        when(solicitudCambioRepository.existsByActividad_IdAndEstadoInAndDeletedAtIsNull(
                eq(70L), anyList()
        )).thenReturn(false);
        when(solicitudCambioRepository.save(any(SolicitudCambioActividad.class)))
                .thenAnswer((invocacion) -> invocacion.getArgument(0));

        SolicitudCambioActividadRequestDTO request = new SolicitudCambioActividadRequestDTO();
        request.setTitulo("  Nuevo titulo  ");
        request.setNivel("principiante");
        request.setInstagramContacto("   ");

        SolicitudCambioDetalleDTO detalle = service.crearSolicitud(10L, 70L, request);

        ArgumentCaptor<SolicitudCambioActividad> captor =
                ArgumentCaptor.forClass(SolicitudCambioActividad.class);
        verify(solicitudCambioRepository).save(captor.capture());
        SolicitudCambioActividad guardada = captor.getValue();

        assertEquals("PENDIENTE", guardada.getEstado());
        assertEquals("Nuevo titulo", guardada.getTitulo());
        assertEquals("PRINCIPIANTE", guardada.getNivel());
        assertNull(guardada.getInstagramContacto());
        assertEquals(70L, detalle.getActividadId());
        assertEquals(2, detalle.getCambios().size());
        assertEquals("titulo", detalle.getCambios().get(0).getCampo());
        assertEquals("Titulo actual", detalle.getCambios().get(0).getValorActual());
        assertEquals("Nuevo titulo", detalle.getCambios().get(0).getValorPropuesto());
    }

    @Test
    void crearSolicitudConOtraAbiertaLanzaConflicto() {
        PerfilPublicador perfil = perfil();
        Actividad actividad = actividad();
        configurarPerfil(perfil);
        configurarActividadPropia(actividad, perfil);
        when(solicitudCambioRepository.existsByActividad_IdAndEstadoInAndDeletedAtIsNull(
                eq(70L), anyList()
        )).thenReturn(true);

        SolicitudCambioActividadRequestDTO request = new SolicitudCambioActividadRequestDTO();
        request.setTitulo("Nuevo titulo");

        assertThrows(
                SolicitudCambioConflictoException.class,
                () -> service.crearSolicitud(10L, 70L, request)
        );
        verify(solicitudCambioRepository, never()).save(any(SolicitudCambioActividad.class));
    }

    @Test
    void crearSolicitudSinCambiosLanzaInvalida() {
        PerfilPublicador perfil = perfil();
        Actividad actividad = actividad();
        configurarPerfil(perfil);
        configurarActividadPropia(actividad, perfil);

        SolicitudCambioActividadRequestDTO request = new SolicitudCambioActividadRequestDTO();
        request.setTitulo("   ");

        assertThrows(
                SolicitudCambioInvalidaException.class,
                () -> service.crearSolicitud(10L, 70L, request)
        );
    }

    @Test
    void crearSolicitudConNivelInvalidoLanzaInvalida() {
        PerfilPublicador perfil = perfil();
        Actividad actividad = actividad();
        configurarPerfil(perfil);
        configurarActividadPropia(actividad, perfil);

        SolicitudCambioActividadRequestDTO request = new SolicitudCambioActividadRequestDTO();
        request.setNivel("EXPERTO");

        assertThrows(
                SolicitudCambioInvalidaException.class,
                () -> service.crearSolicitud(10L, 70L, request)
        );
    }

    @Test
    void crearSolicitudSobreActividadAjenaLanzaNoEncontrado() {
        PerfilPublicador perfil = perfil();
        configurarPerfil(perfil);
        when(actividadRepository
                .findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionInAndDeletedAtIsNull(
                        eq(70L), eq(30L), eq(java.util.List.of("PUBLICADA", "PAUSADA"))
                ))
                .thenReturn(Optional.empty());

        SolicitudCambioActividadRequestDTO request = new SolicitudCambioActividadRequestDTO();
        request.setTitulo("Nuevo titulo");

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.crearSolicitud(10L, 70L, request)
        );
    }

    @Test
    void listarMiasConEstadoInvalidoLanzaFiltroInvalido() {
        PerfilPublicador perfil = perfil();
        configurarPerfil(perfil);

        assertThrows(
                FiltroInvalidoException.class,
                () -> service.listarMias(10L, "CUALQUIERA", 0, 20, null)
        );
    }

    @Test
    void obtenerMiaInexistenteLanzaNoEncontrado() {
        PerfilPublicador perfil = perfil();
        configurarPerfil(perfil);
        when(solicitudCambioRepository.findByIdAndPerfilPublicador_IdAndDeletedAtIsNull(99L, 30L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.obtenerMia(10L, 99L)
        );
    }

    private void configurarPerfil(PerfilPublicador perfil) {
        when(perfilPublicadorRepository.findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(perfil));
    }

    private void configurarActividadPropia(Actividad actividad, PerfilPublicador perfil) {
        when(actividadRepository
                .findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionInAndDeletedAtIsNull(
                        eq(70L), eq(perfil.getId()), eq(java.util.List.of("PUBLICADA", "PAUSADA"))
                ))
                .thenReturn(Optional.of(actividad));
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setActivo(true);
        usuario.setDeletedAt(null);
        return usuario;
    }

    private PerfilPublicador perfil() {
        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setId(30L);
        perfil.setUsuario(usuario());
        perfil.setNombre("Perfil Publicador");
        perfil.setActivo(true);
        return perfil;
    }

    private Actividad actividad() {
        Actividad actividad = new Actividad();
        actividad.setId(70L);
        actividad.setTitulo("Titulo actual");
        actividad.setSlug("titulo-actual");
        actividad.setNivel("TODOS");
        actividad.setPrecioReferencia(new BigDecimal("1000.00"));
        return actividad;
    }

    /* ============ Campos nuevos (script 24) ============ */

    @Test
    void cambiarHorariosSinFilasLanzaInvalida() {
        PerfilPublicador perfil = perfil();
        Actividad actividad = actividad();
        configurarPerfil(perfil);
        configurarActividadPropia(actividad, perfil);

        SolicitudCambioActividadRequestDTO request = new SolicitudCambioActividadRequestDTO();
        request.setCambiaHorarios(true);

        assertThrows(
                SolicitudCambioInvalidaException.class,
                () -> service.crearSolicitud(10L, 70L, request)
        );
    }

    @Test
    void unHorarioQueTerminaAntesDeEmpezarLanzaInvalida() {
        PerfilPublicador perfil = perfil();
        Actividad actividad = actividad();
        configurarPerfil(perfil);
        configurarActividadPropia(actividad, perfil);

        SolicitudCambioActividadRequestDTO request = new SolicitudCambioActividadRequestDTO();
        request.setCambiaHorarios(true);
        request.setHorarios(java.util.List.of(
                new com.dondeentreno.api.dto.SolicitudPublicacionHorarioRequestDTO(
                        "LUNES",
                        java.time.LocalTime.of(19, 0),
                        java.time.LocalTime.of(18, 0),
                        null
                )
        ));

        assertThrows(
                SolicitudCambioInvalidaException.class,
                () -> service.crearSolicitud(10L, 70L, request)
        );
    }

    @Test
    void horariosValidosQuedanComoHijasDeLaSolicitud() {
        PerfilPublicador perfil = perfil();
        Actividad actividad = actividad();
        configurarPerfil(perfil);
        configurarActividadPropia(actividad, perfil);
        when(solicitudCambioRepository.existsByActividad_IdAndEstadoInAndDeletedAtIsNull(
                eq(70L), anyList()
        )).thenReturn(false);
        when(solicitudCambioRepository.save(any(SolicitudCambioActividad.class)))
                .thenAnswer((invocacion) -> invocacion.getArgument(0));

        SolicitudCambioActividadRequestDTO request = new SolicitudCambioActividadRequestDTO();
        request.setCambiaHorarios(true);
        request.setHorarios(java.util.List.of(
                new com.dondeentreno.api.dto.SolicitudPublicacionHorarioRequestDTO(
                        "LUNES",
                        java.time.LocalTime.of(18, 0),
                        java.time.LocalTime.of(19, 30),
                        "Traer guantes"
                ),
                new com.dondeentreno.api.dto.SolicitudPublicacionHorarioRequestDTO(
                        "MIERCOLES",
                        java.time.LocalTime.of(18, 0),
                        java.time.LocalTime.of(19, 30),
                        null
                )
        ));

        SolicitudCambioDetalleDTO detalle = service.crearSolicitud(10L, 70L, request);

        ArgumentCaptor<SolicitudCambioActividad> captor =
                ArgumentCaptor.forClass(SolicitudCambioActividad.class);
        verify(solicitudCambioRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getHorarios().size());
        assertEquals(Boolean.TRUE, captor.getValue().getCambiaHorarios());
        assertEquals(1, detalle.getCambios().size());
        assertEquals("horarios", detalle.getCambios().get(0).getCampo());
    }

    @Test
    void laEdadMinimaPropuestaNoPuedeSuperarLaMaximaQueQueda() {
        PerfilPublicador perfil = perfil();
        Actividad actividad = actividad();
        actividad.setEdadMaxima(40);
        configurarPerfil(perfil);
        configurarActividadPropia(actividad, perfil);

        SolicitudCambioActividadRequestDTO request = new SolicitudCambioActividadRequestDTO();
        request.setEdadMinima(50);

        assertThrows(
                SolicitudCambioInvalidaException.class,
                () -> service.crearSolicitud(10L, 70L, request)
        );
    }

    @Test
    void laUbicacionExigeDireccionYBarrioDeLaMismaCiudad() {
        PerfilPublicador perfil = perfil();
        Actividad actividad = actividad();
        com.dondeentreno.api.entity.Ciudad ciudad = new com.dondeentreno.api.entity.Ciudad();
        ciudad.setId(2L);
        com.dondeentreno.api.entity.Ubicacion ubicacion = new com.dondeentreno.api.entity.Ubicacion();
        ubicacion.setCiudad(ciudad);
        actividad.setUbicacion(ubicacion);
        configurarPerfil(perfil);
        configurarActividadPropia(actividad, perfil);

        /* Sin barrio: invalida. */
        SolicitudCambioActividadRequestDTO sinBarrio = new SolicitudCambioActividadRequestDTO();
        sinBarrio.setUbicacionDireccion("Calle nueva 123");
        assertThrows(
                SolicitudCambioInvalidaException.class,
                () -> service.crearSolicitud(10L, 70L, sinBarrio)
        );

        /* Barrio de OTRA ciudad: invalida. */
        com.dondeentreno.api.entity.Ciudad otraCiudad = new com.dondeentreno.api.entity.Ciudad();
        otraCiudad.setId(9L);
        com.dondeentreno.api.entity.Barrio barrioAjeno = new com.dondeentreno.api.entity.Barrio();
        barrioAjeno.setActivo(true);
        barrioAjeno.setCiudad(otraCiudad);
        when(barrioRepository.findById(33L)).thenReturn(java.util.Optional.of(barrioAjeno));

        SolicitudCambioActividadRequestDTO otraCiudadRequest = new SolicitudCambioActividadRequestDTO();
        otraCiudadRequest.setUbicacionDireccion("Calle nueva 123");
        otraCiudadRequest.setUbicacionBarrioId(33L);
        assertThrows(
                SolicitudCambioInvalidaException.class,
                () -> service.crearSolicitud(10L, 70L, otraCiudadRequest)
        );
    }

    @Test
    void unDeporteInactivoOInexistenteLanzaInvalida() {
        PerfilPublicador perfil = perfil();
        Actividad actividad = actividad();
        configurarPerfil(perfil);
        configurarActividadPropia(actividad, perfil);
        when(deporteRepository.findByIdAndActivoTrue(99L))
                .thenReturn(java.util.Optional.empty());

        SolicitudCambioActividadRequestDTO request = new SolicitudCambioActividadRequestDTO();
        request.setDeporteId(99L);

        assertThrows(
                SolicitudCambioInvalidaException.class,
                () -> service.crearSolicitud(10L, 70L, request)
        );
    }
}
