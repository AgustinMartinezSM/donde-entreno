package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.FeedEvent;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.SeguimientoPublicador;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.FeedEventRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SeguimientoPublicadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feed de eventos (script 32, Fase 6): emisión best-effort y
 * paginación real.
 */
@ExtendWith(MockitoExtension.class)
class FeedEventServiceTest {

    private static final Long USUARIO_ID = 10L;
    private static final Long PERFIL_ID = 8L;

    @Mock
    private FeedEventRepository feedEventRepository;

    @Mock
    private SeguimientoPublicadorRepository seguimientoPublicadorRepository;

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private ImagenService imagenService;

    private FeedEventService service;

    @BeforeEach
    void setUp() {
        service = new FeedEventService(
                feedEventRepository,
                seguimientoPublicadorRepository,
                perfilPublicadorRepository,
                actividadRepository,
                imagenRepository,
                imagenService
        );
    }

    /**
     * EL contrato de la fase: si guardar el evento explota, el hecho
     * real (aprobar una actividad, publicar una foto) no se cae con
     * él. Un feed roto no puede voltear el negocio.
     */
    @Test
    void siLaEmisionFallaNoPropagaLaExcepcion() {
        when(feedEventRepository.saveAndFlush(any(FeedEvent.class)))
                .thenThrow(new RuntimeException("base caida"));

        service.emitir(
                FeedEventService.TIPO_ACTIVIDAD_NUEVA,
                PERFIL_ID,
                20L,
                null,
                null
        );
        /* Sin assert de excepción: que el test termine YA es el assert. */
    }

    @Test
    void sinTipoOSinPerfilNoGuardaNada() {
        service.emitir(null, PERFIL_ID, null, null, null);
        service.emitir(FeedEventService.TIPO_ACTIVIDAD_NUEVA, null, null, null, null);

        verify(feedEventRepository, never()).saveAndFlush(any());
    }

    /** El resumen se recorta al ancho de la columna (VARCHAR 200). */
    @Test
    void elResumenSeRecortaAlAnchoDeLaColumna() {
        when(feedEventRepository.saveAndFlush(any(FeedEvent.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        service.emitir(
                FeedEventService.TIPO_FOTOS_NUEVAS,
                PERFIL_ID,
                null,
                7L,
                "x".repeat(500)
        );

        org.mockito.ArgumentCaptor<FeedEvent> captor =
                org.mockito.ArgumentCaptor.forClass(FeedEvent.class);
        verify(feedEventRepository).saveAndFlush(captor.capture());
        assertEquals(200, captor.getValue().getResumen().length());
    }

    /**
     * Con una transacción en curso el evento NO se guarda todavía: se
     * difiere al commit, porque las FKs apuntan a filas que esa
     * transacción aún no confirmó. Guardar acá violaba la FK y —al
     * fallar recién en el commit de la transacción paralela— rompía
     * el flujo entero con un 500.
     */
    @Test
    void conTransaccionEnCursoElGuardadoSeDifiereAlCommit() {
        org.springframework.transaction.support.TransactionSynchronizationManager
                .initSynchronization();

        try {
            service.emitir(
                    FeedEventService.TIPO_FOTOS_NUEVAS,
                    PERFIL_ID,
                    20L,
                    7L,
                    null
            );

            verify(feedEventRepository, never()).saveAndFlush(any());
            assertEquals(
                    1,
                    org.springframework.transaction.support.TransactionSynchronizationManager
                            .getSynchronizations().size()
            );
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager
                    .clearSynchronization();
        }
    }

    /**
     * Sin seguidos, el feed devuelve la página vacía SIN tocar la
     * tabla de eventos (el mismo corte temprano que ya tenía la V1).
     */
    @Test
    void sinSeguidosNoConsultaLosEventos() {
        when(seguimientoPublicadorRepository.findByUsuario_IdOrderByCreatedAtDesc(USUARIO_ID))
                .thenReturn(List.of());

        var pagina = service.listarParaUsuario(USUARIO_ID, 0, 10);

        assertTrue(pagina.getContenido().isEmpty());
        assertTrue(pagina.isUltima());
        assertEquals(0L, pagina.getTotalElementos());
        verify(feedEventRepository, never())
                .findByPerfilPublicadorIdInOrderByCreatedAtDesc(anyList(), any());
    }

    @Test
    void sinUsuarioNoHayFeed() {
        assertThrows(
                CredencialesInvalidasException.class,
                () -> service.listarParaUsuario(null, 0, 10)
        );
    }

    /** El tamaño de página se sanea: nunca lanza por valores feos. */
    @Test
    void elTamanioDePaginaSeSanea() {
        when(seguimientoPublicadorRepository.findByUsuario_IdOrderByCreatedAtDesc(USUARIO_ID))
                .thenReturn(List.of(seguimiento()));
        when(feedEventRepository.findByPerfilPublicadorIdInOrderByCreatedAtDesc(
                anyList(),
                any()
        )).thenReturn(org.springframework.data.domain.Page.empty());

        service.listarParaUsuario(USUARIO_ID, -5, 9999);

        org.mockito.ArgumentCaptor<org.springframework.data.domain.Pageable> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(feedEventRepository)
                .findByPerfilPublicadorIdInOrderByCreatedAtDesc(anyList(), captor.capture());

        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(50, captor.getValue().getPageSize());
    }

    private SeguimientoPublicador seguimiento() {
        PerfilPublicador perfil = new PerfilPublicador();
        try {
            java.lang.reflect.Field campo = PerfilPublicador.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(perfil, PERFIL_ID);
        } catch (ReflectiveOperationException excepcion) {
            throw new IllegalStateException(excepcion);
        }

        SeguimientoPublicador seguimiento = new SeguimientoPublicador();
        seguimiento.setPerfilPublicador(perfil);
        seguimiento.setCreatedAt(OffsetDateTime.now());
        return seguimiento;
    }
}
