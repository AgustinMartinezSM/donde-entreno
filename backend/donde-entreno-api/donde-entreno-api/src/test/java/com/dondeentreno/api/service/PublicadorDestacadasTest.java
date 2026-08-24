package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.HorarioActividadRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Actividades destacadas del publicador (Fase 5, script 31): el PUT
 * reemplaza la selección, valida contra la BASE (no contra lo que
 * mande el cliente) y respeta el tope.
 */
@ExtendWith(MockitoExtension.class)
class PublicadorDestacadasTest {

    private static final Long USER_ID = 5L;
    private static final Long PERFIL_ID = 8L;

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

        lenient().when(perfilPublicadorRepository
                        .findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(USER_ID))
                .thenReturn(Optional.of(perfil()));
    }

    @Test
    void laSeleccionSePersisteEnElOrdenPedido() {
        Actividad primera = actividad(20L);
        Actividad segunda = actividad(21L);

        when(actividadRepository.findByPerfilPublicador_IdAndDestacadaOrdenIsNotNull(PERFIL_ID))
                .thenReturn(List.of());
        when(actividadRepository
                .findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionInAndDeletedAtIsNull(
                        eqId(20L), anyLong(), anyList()))
                .thenReturn(Optional.of(primera));
        when(actividadRepository
                .findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionInAndDeletedAtIsNull(
                        eqId(21L), anyLong(), anyList()))
                .thenReturn(Optional.of(segunda));
        when(actividadRepository.save(any(Actividad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(actividadRepository
                .findByPerfilPublicador_IdAndDestacadaOrdenIsNotNullAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNullOrderByDestacadaOrdenAsc(
                        anyLong(), anyString()))
                .thenReturn(List.of());

        service.definirDestacadas(USER_ID, List.of(20L, 21L));

        assertEquals(1, primera.getDestacadaOrden());
        assertEquals(2, segunda.getDestacadaOrden());
    }

    /** El PUT REEMPLAZA: lo que estaba destacado y no viene, se limpia. */
    @Test
    void loQueYaNoSeElegeVuelveANull() {
        Actividad anterior = actividad(30L);
        anterior.setDestacadaOrden(1);

        when(actividadRepository.findByPerfilPublicador_IdAndDestacadaOrdenIsNotNull(PERFIL_ID))
                .thenReturn(List.of(anterior));
        when(actividadRepository.save(any(Actividad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(actividadRepository
                .findByPerfilPublicador_IdAndDestacadaOrdenIsNotNullAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNullOrderByDestacadaOrdenAsc(
                        anyLong(), anyString()))
                .thenReturn(List.of());

        service.definirDestacadas(USER_ID, List.of());

        assertNull(anterior.getDestacadaOrden());
    }

    @Test
    void masDeTresODuplicadasDan400SinTocarNada() {
        assertThrows(
                FiltroInvalidoException.class,
                () -> service.definirDestacadas(USER_ID, List.of(1L, 2L, 3L, 4L))
        );

        assertThrows(
                FiltroInvalidoException.class,
                () -> service.definirDestacadas(USER_ID, List.of(1L, 1L))
        );

        verify(actividadRepository, never()).save(any());
    }

    /**
     * Destacar una actividad ajena (o pausada) da 404: la validación
     * corre contra la base, no contra lo que mande el cliente.
     */
    @Test
    void unaActividadAjenaOPausadaNoSePuedeDestacar() {
        when(actividadRepository.findByPerfilPublicador_IdAndDestacadaOrdenIsNotNull(PERFIL_ID))
                .thenReturn(List.of());
        when(actividadRepository
                .findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionInAndDeletedAtIsNull(
                        anyLong(), anyLong(), anyList()))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.definirDestacadas(USER_ID, List.of(999L))
        );
    }

    private Long eqId(long valor) {
        return org.mockito.ArgumentMatchers.eq(valor);
    }

    private Actividad actividad(Long id) {
        Actividad actividad = new Actividad();
        try {
            java.lang.reflect.Field campo = Actividad.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(actividad, id);
        } catch (ReflectiveOperationException excepcion) {
            throw new IllegalStateException(excepcion);
        }
        actividad.setTitulo("Actividad " + id);
        actividad.setActiva(true);
        actividad.setEstadoPublicacion("PUBLICADA");
        return actividad;
    }

    private PerfilPublicador perfil() {
        PerfilPublicador perfil = new PerfilPublicador();
        try {
            java.lang.reflect.Field campo = PerfilPublicador.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(perfil, PERFIL_ID);
        } catch (ReflectiveOperationException excepcion) {
            throw new IllegalStateException(excepcion);
        }
        perfil.setNombre("Club Atletico Sur");
        perfil.setActivo(true);
        return perfil;
    }
}
