package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ActualizarEstadoSolicitudCambioRequestDTO;
import com.dondeentreno.api.dto.SolicitudCambioDetalleDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.SolicitudCambioActividad;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.SolicitudCambioInvalidaException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.SolicitudCambioActividadRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudCambioActividadAdminServiceTest {

    @Mock
    private SolicitudCambioActividadRepository solicitudCambioRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private SolicitudCambioActividadAdminService service;

    @BeforeEach
    void setUp() {
        service = new SolicitudCambioActividadAdminService(
                solicitudCambioRepository,
                actividadRepository,
                usuarioRepository
        );
    }

    @Test
    void aprobarAplicaSoloLosCamposPropuestos() {
        Actividad actividad = actividad();
        SolicitudCambioActividad solicitud = solicitudPendiente(actividad);
        solicitud.setTitulo("Titulo nuevo");
        solicitud.setPrecioReferencia(new BigDecimal("2500.00"));
        configurarSolicitud(solicitud);
        configurarAdmin();
        configurarActividadVigente(actividad);
        when(actividadRepository.save(actividad)).thenReturn(actividad);
        when(solicitudCambioRepository.save(solicitud)).thenReturn(solicitud);

        SolicitudCambioDetalleDTO detalle = service.aprobar(80L, 50L);

        assertEquals("Titulo nuevo", actividad.getTitulo());
        assertEquals(new BigDecimal("2500.00"), actividad.getPrecioReferencia());
        // La descripcion no estaba propuesta: no cambia.
        assertEquals("Descripcion actual", actividad.getDescripcion());
        assertNotNull(actividad.getUpdatedAt());

        assertEquals("APROBADA", solicitud.getEstado());
        assertNotNull(solicitud.getResueltoAt());
        assertEquals(50L, solicitud.getResueltoPor().getId());

        // El diff devuelto muestra el antes/despues real de la aprobacion.
        assertEquals(2, detalle.getCambios().size());
        assertEquals("Titulo actual", detalle.getCambios().get(0).getValorActual());
        assertEquals("Titulo nuevo", detalle.getCambios().get(0).getValorPropuesto());
    }

    @Test
    void aprobarConActividadNoVigenteRechazaAutomaticamente() {
        Actividad actividad = actividad();
        SolicitudCambioActividad solicitud = solicitudPendiente(actividad);
        solicitud.setTitulo("Titulo nuevo");
        configurarSolicitud(solicitud);
        configurarAdmin();
        when(actividadRepository
                .findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                        eq(70L), eq(30L), eq("PUBLICADA")
                ))
                .thenReturn(Optional.empty());
        when(solicitudCambioRepository.save(solicitud)).thenReturn(solicitud);

        SolicitudCambioDetalleDTO detalle = service.aprobar(80L, 50L);

        assertEquals("RECHAZADA", detalle.getEstado());
        assertNotNull(detalle.getMotivoRechazo());
        // La actividad original no se toca.
        assertEquals("Titulo actual", actividad.getTitulo());
        verify(actividadRepository, never()).save(any(Actividad.class));
    }

    @Test
    void aprobarSolicitudYaResueltaLanzaInvalida() {
        Actividad actividad = actividad();
        SolicitudCambioActividad solicitud = solicitudPendiente(actividad);
        solicitud.setEstado("APROBADA");
        configurarSolicitud(solicitud);

        assertThrows(
                SolicitudCambioInvalidaException.class,
                () -> service.aprobar(80L, 50L)
        );
    }

    @Test
    void rechazarSinMotivoLanzaInvalida() {
        Actividad actividad = actividad();
        SolicitudCambioActividad solicitud = solicitudPendiente(actividad);
        configurarSolicitud(solicitud);

        ActualizarEstadoSolicitudCambioRequestDTO request =
                new ActualizarEstadoSolicitudCambioRequestDTO();
        request.setEstado("RECHAZADA");
        request.setMotivoRechazo("   ");

        assertThrows(
                SolicitudCambioInvalidaException.class,
                () -> service.actualizarEstado(80L, request, 50L)
        );
        verify(solicitudCambioRepository, never()).save(any(SolicitudCambioActividad.class));
    }

    @Test
    void rechazarConMotivoResuelveLaSolicitud() {
        Actividad actividad = actividad();
        SolicitudCambioActividad solicitud = solicitudPendiente(actividad);
        configurarSolicitud(solicitud);
        configurarAdmin();
        when(solicitudCambioRepository.save(solicitud)).thenReturn(solicitud);

        ActualizarEstadoSolicitudCambioRequestDTO request =
                new ActualizarEstadoSolicitudCambioRequestDTO();
        request.setEstado("RECHAZADA");
        request.setMotivoRechazo("El precio propuesto no es coherente.");

        SolicitudCambioDetalleDTO detalle = service.actualizarEstado(80L, request, 50L);

        assertEquals("RECHAZADA", detalle.getEstado());
        assertEquals("El precio propuesto no es coherente.", detalle.getMotivoRechazo());
        assertNotNull(solicitud.getResueltoAt());
    }

    @Test
    void actualizarEstadoInvalidoLanzaInvalida() {
        Actividad actividad = actividad();
        SolicitudCambioActividad solicitud = solicitudPendiente(actividad);
        configurarSolicitud(solicitud);

        ActualizarEstadoSolicitudCambioRequestDTO request =
                new ActualizarEstadoSolicitudCambioRequestDTO();
        request.setEstado("APROBADA");

        assertThrows(
                SolicitudCambioInvalidaException.class,
                () -> service.actualizarEstado(80L, request, 50L)
        );
    }

    private void configurarSolicitud(SolicitudCambioActividad solicitud) {
        when(solicitudCambioRepository.findByIdAndDeletedAtIsNull(80L))
                .thenReturn(Optional.of(solicitud));
    }

    private void configurarAdmin() {
        Usuario admin = new Usuario();
        admin.setId(50L);
        admin.setActivo(true);
        admin.setDeletedAt(null);
        when(usuarioRepository.findById(50L)).thenReturn(Optional.of(admin));
    }

    private void configurarActividadVigente(Actividad actividad) {
        when(actividadRepository
                .findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                        eq(70L), eq(30L), eq("PUBLICADA")
                ))
                .thenReturn(Optional.of(actividad));
    }

    private PerfilPublicador perfil() {
        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setId(30L);
        perfil.setNombre("Perfil Publicador");
        return perfil;
    }

    private Actividad actividad() {
        Actividad actividad = new Actividad();
        actividad.setId(70L);
        actividad.setTitulo("Titulo actual");
        actividad.setSlug("titulo-actual");
        actividad.setDescripcion("Descripcion actual");
        return actividad;
    }

    private SolicitudCambioActividad solicitudPendiente(Actividad actividad) {
        SolicitudCambioActividad solicitud = new SolicitudCambioActividad();
        solicitud.setId(80L);
        solicitud.setActividad(actividad);
        solicitud.setPerfilPublicador(perfil());
        solicitud.setEstado("PENDIENTE");
        solicitud.setCreatedAt(OffsetDateTime.now());
        solicitud.setUpdatedAt(OffsetDateTime.now());
        return solicitud;
    }
}
