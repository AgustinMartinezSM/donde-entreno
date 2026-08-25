package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.Conversacion;
import com.dondeentreno.api.entity.Mensaje;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.ConversacionRepository;
import com.dondeentreno.api.repository.MensajeRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Inbox de consultas (script 36): los topes, quién puede escribir y
 * qué ve el admin.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InboxServiceTest {

    private static final Long USUARIO_ID = 10L;
    private static final Long DUENIO_ID = 20L;
    private static final Long PERFIL_ID = 8L;

    @Mock
    private ConversacionRepository conversacionRepository;

    @Mock
    private MensajeRepository mensajeRepository;

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ImagenService imagenService;

    @Mock
    private NotificacionService notificacionService;

    private InboxService service;

    @BeforeEach
    void setUp() {
        service = new InboxService(
                conversacionRepository,
                mensajeRepository,
                perfilPublicadorRepository,
                actividadRepository,
                usuarioRepository,
                imagenService,
                notificacionService
        );

        when(perfilPublicadorRepository.findByIdAndActivoTrue(PERFIL_ID))
                .thenReturn(Optional.of(perfil()));
        when(perfilPublicadorRepository
                .findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(DUENIO_ID))
                .thenReturn(Optional.of(perfil()));
        when(conversacionRepository.saveAndFlush(any(Conversacion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(mensajeRepository.saveAndFlush(any(Mensaje.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(mensajeRepository.contarNoLeidos(anyList(), anyString())).thenReturn(List.of());
        when(mensajeRepository.findByConversacionIdOrderByCreatedAtAsc(any()))
                .thenReturn(List.of());
        when(perfilPublicadorRepository.findAllById(anyList())).thenReturn(List.of(perfil()));
        when(imagenService.obtenerLogosAprobadosPorPerfil(anyList())).thenReturn(Map.of());
        when(usuarioRepository.findAllById(anyList())).thenReturn(List.of(usuario()));
    }

    @Test
    void consultarAbreElHiloYAvisaAlPublicador() {
        service.consultar(USUARIO_ID, PERFIL_ID, null, "  Hola, ¿cuánto sale?  ");

        ArgumentCaptor<Mensaje> mensaje = ArgumentCaptor.forClass(Mensaje.class);
        verify(mensajeRepository).saveAndFlush(mensaje.capture());
        assertThat(mensaje.getValue().getAutor()).isEqualTo("USUARIO");
        assertThat(mensaje.getValue().getTexto()).isEqualTo("Hola, ¿cuánto sale?");

        /* Le llega al DUEÑO del perfil, que es quien responde. */
        verify(notificacionService).emitir(
                eq(DUENIO_ID), eq("MENSAJE_NUEVO"), anyString(), anyString());
    }

    /** Volver a escribirle al mismo club no abre un hilo nuevo. */
    @Test
    void volverAEscribirReusaLaConversacionExistente() {
        Conversacion existente = conversacion();
        when(conversacionRepository.buscarExistente(USUARIO_ID, PERFIL_ID, null))
                .thenReturn(Optional.of(existente));

        service.consultar(USUARIO_ID, PERFIL_ID, null, "Otra consulta");

        /* No se cuenta contra el tope de conversaciones nuevas. */
        verify(conversacionRepository, never())
                .countByUsuarioIdAndCreatedAtGreaterThanEqual(anyLong(), any());
    }

    @Test
    void pasadoElTopeDeConversacionesDelDiaNoSeAbreOtra() {
        when(conversacionRepository.countByUsuarioIdAndCreatedAtGreaterThanEqual(
                eq(USUARIO_ID), any(OffsetDateTime.class))).thenReturn(5L);

        assertThatThrownBy(() -> service.consultar(USUARIO_ID, PERFIL_ID, null, "Hola"))
                .isInstanceOf(FiltroInvalidoException.class);

        verify(mensajeRepository, never()).saveAndFlush(any(Mensaje.class));
    }

    @Test
    void pasadoElTopeDeMensajesDelDiaNoSeEscribe() {
        when(mensajeRepository.contarDelUsuarioDesde(eq(USUARIO_ID), any(OffsetDateTime.class)))
                .thenReturn(20L);

        assertThatThrownBy(() -> service.consultar(USUARIO_ID, PERFIL_ID, null, "Hola"))
                .isInstanceOf(FiltroInvalidoException.class);

        verify(mensajeRepository, never()).saveAndFlush(any(Mensaje.class));
    }

    /**
     * La regla que sostiene la confianza en la bandeja: el publicador
     * no tiene forma de arrancar una conversación. Ni siquiera existe
     * el método — solo puede responder una que ya existe.
     */
    @Test
    void elPublicadorNoPuedeResponderUnaConversacionAjena() {
        Conversacion ajena = conversacion();
        ajena.setPerfilPublicadorId(999L);
        when(conversacionRepository.findById(5L)).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> service.responder(DUENIO_ID, 5L, "Hola"))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(mensajeRepository, never()).saveAndFlush(any(Mensaje.class));
    }

    /** Cerrar significa algo: el publicador no puede seguir escribiendo. */
    @Test
    void cerradaPorElUsuarioElPublicadorNoPuedeResponder() {
        Conversacion cerrada = conversacion();
        cerrada.setEstado("CERRADA_POR_USUARIO");
        when(conversacionRepository.findById(5L)).thenReturn(Optional.of(cerrada));

        assertThatThrownBy(() -> service.responder(DUENIO_ID, 5L, "¿Hola?"))
                .isInstanceOf(FiltroInvalidoException.class);

        verify(mensajeRepository, never()).saveAndFlush(any(Mensaje.class));
    }

    /** Pero el usuario puede reabrirla escribiendo de nuevo. */
    @Test
    void elUsuarioReabreLaConversacionAlVolverAEscribir() {
        Conversacion cerrada = conversacion();
        cerrada.setEstado("CERRADA_POR_USUARIO");
        when(conversacionRepository.buscarExistente(USUARIO_ID, PERFIL_ID, null))
                .thenReturn(Optional.of(cerrada));

        service.consultar(USUARIO_ID, PERFIL_ID, null, "Perdón, una cosa más");

        assertThat(cerrada.getEstado()).isEqualTo("ABIERTA");
    }

    /** 404 y no 403: no se delata que la conversación ajena existe. */
    @Test
    void unUsuarioNoPuedeAbrirElHiloDeOtro() {
        Conversacion ajena = conversacion();
        ajena.setUsuarioId(999L);
        when(conversacionRepository.findById(5L)).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> service.verHilo(USUARIO_ID, 5L, false))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void nadieSeConsultaASiMismo() {
        assertThatThrownBy(() -> service.consultar(DUENIO_ID, PERFIL_ID, null, "Hola"))
                .isInstanceOf(FiltroInvalidoException.class);
    }

    /**
     * LO QUE VE EL ADMIN: el mensaje reportado y a lo sumo los dos
     * anteriores, en orden de lectura. Nunca el hilo entero.
     */
    @Test
    void elContextoDeUnReporteSonTresMensajesEnOrden() {
        Mensaje reportado = mensaje("el reportado", OffsetDateTime.now());
        when(mensajeRepository.findById(7L)).thenReturn(Optional.of(reportado));
        when(mensajeRepository
                .findTop3ByConversacionIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                        any(), any()))
                .thenReturn(List.of(
                        reportado,
                        mensaje("el del medio", OffsetDateTime.now().minusMinutes(1)),
                        mensaje("el primero", OffsetDateTime.now().minusMinutes(2))
                ));

        List<com.dondeentreno.api.dto.MensajeDTO> contexto = service.contextoDeReporte(7L);

        assertThat(contexto).hasSize(3);
        assertThat(contexto.get(0).getTexto()).isEqualTo("el primero");
        assertThat(contexto.get(2).getTexto()).isEqualTo("el reportado");
    }

    @Test
    void ocultarUnMensajeLoSacaDeCirculacionYDejaDeSerReportable() {
        Mensaje mensaje = mensaje("un exabrupto", OffsetDateTime.now());
        when(mensajeRepository.findById(7L)).thenReturn(Optional.of(mensaje));

        assertThat(service.esVisibleMensaje(7L)).isTrue();

        service.ocultarMensajePorAdmin(7L);

        assertThat(mensaje.getEstado()).isEqualTo("OCULTO_POR_ADMIN");
        assertThat(service.esVisibleMensaje(7L)).isFalse();
    }

    private Conversacion conversacion() {
        Conversacion conversacion = new Conversacion();
        conversacion.setUsuarioId(USUARIO_ID);
        conversacion.setPerfilPublicadorId(PERFIL_ID);
        conversacion.setEstado("ABIERTA");
        conversacion.setUltimoMensajeAt(OffsetDateTime.now());
        conversacion.setCreatedAt(OffsetDateTime.now());
        conversacion.setUpdatedAt(OffsetDateTime.now());
        return conversacion;
    }

    private Mensaje mensaje(String texto, OffsetDateTime cuando) {
        Mensaje mensaje = new Mensaje();
        mensaje.setConversacionId(5L);
        mensaje.setAutor("USUARIO");
        mensaje.setTexto(texto);
        mensaje.setEstado("VISIBLE");
        mensaje.setCreatedAt(cuando);
        return mensaje;
    }

    private PerfilPublicador perfil() {
        Usuario duenio = new Usuario();
        duenio.setId(DUENIO_ID);

        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setId(PERFIL_ID);
        perfil.setNombre("Club Atlético Sur");
        perfil.setSlug("club-atletico-sur");
        perfil.setUsuario(duenio);
        return perfil;
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setNombre("Ana");
        usuario.setApellido("Gomez");
        return usuario;
    }
}
