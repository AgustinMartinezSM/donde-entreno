package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.AvisoGrupo;
import com.dondeentreno.api.entity.MiembroActividad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.AvisoGrupoRepository;
import com.dondeentreno.api.repository.ComentarioAvisoRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.MeGustaAvisoRepository;
import com.dondeentreno.api.repository.MiembroActividadRepository;
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
 * Grupos por actividad (script 38): pertenencia explícita, quién puede
 * leer y escribir, y el tope de avisos.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GrupoActividadServiceTest {

    private static final Long USUARIO_ID = 10L;
    private static final Long DUENIO_ID = 20L;
    private static final Long PERFIL_ID = 8L;
    private static final Long ACTIVIDAD_ID = 11L;

    @Mock
    private MiembroActividadRepository miembroRepository;

    @Mock
    private AvisoGrupoRepository avisoRepository;

    @Mock
    private ComentarioAvisoRepository comentarioRepository;

    @Mock
    private MeGustaAvisoRepository meGustaRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private NotificacionService notificacionService;

    private GrupoActividadService service;

    @BeforeEach
    void setUp() {
        service = new GrupoActividadService(
                miembroRepository,
                avisoRepository,
                comentarioRepository,
                meGustaRepository,
                actividadRepository,
                perfilPublicadorRepository,
                usuarioRepository,
                imagenRepository,
                notificacionService
        );

        when(actividadRepository.findById(ACTIVIDAD_ID))
                .thenReturn(Optional.of(actividadPublicada()));
        when(perfilPublicadorRepository
                .findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(DUENIO_ID))
                .thenReturn(Optional.of(perfil()));
        when(miembroRepository.saveAndFlush(any(MiembroActividad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(avisoRepository.saveAndFlush(any(AvisoGrupo.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(meGustaRepository.contarPorAvisos(anyList())).thenReturn(List.of());
        when(comentarioRepository.contarPorAvisos(anyList())).thenReturn(List.of());
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(miembroRepository.usuarioIdsActivosDe(ACTIVIDAD_ID))
                .thenReturn(List.of(USUARIO_ID));
    }

    /* ===================== pertenencia ===================== */

    @Test
    void unirseDejaAlUsuarioComoMiembroActivo() {
        service.unirse(USUARIO_ID, ACTIVIDAD_ID);

        ArgumentCaptor<MiembroActividad> guardado =
                ArgumentCaptor.forClass(MiembroActividad.class);
        verify(miembroRepository).saveAndFlush(guardado.capture());
        assertThat(guardado.getValue().getEstado()).isEqualTo("ACTIVO");
        assertThat(guardado.getValue().getUsuarioId()).isEqualTo(USUARIO_ID);
    }

    /** Volver a entrar reusa la fila: no se pierde la fecha original. */
    @Test
    void volverAEntrarReusaLaFilaExistente() {
        MiembroActividad previo = miembro("SALIO");
        OffsetDateTime original = previo.getCreatedAt();
        when(miembroRepository.findByUsuarioIdAndActividadId(USUARIO_ID, ACTIVIDAD_ID))
                .thenReturn(Optional.of(previo));

        service.unirse(USUARIO_ID, ACTIVIDAD_ID);

        assertThat(previo.getEstado()).isEqualTo("ACTIVO");
        assertThat(previo.getCreatedAt()).isEqualTo(original);
    }

    @Test
    void salirDejaDeSerMiembroSinBorrarLaFila() {
        MiembroActividad activo = miembro("ACTIVO");
        when(miembroRepository.findByUsuarioIdAndActividadId(USUARIO_ID, ACTIVIDAD_ID))
                .thenReturn(Optional.of(activo));

        service.salir(USUARIO_ID, ACTIVIDAD_ID);

        assertThat(activo.getEstado()).isEqualTo("SALIO");
        verify(miembroRepository, never()).delete(any(MiembroActividad.class));
    }

    /**
     * El grupo de una actividad NO publicada no existe: sin esto habría
     * grupos colgando de actividades pausadas o en revisión.
     */
    @Test
    void unaActividadNoPublicadaNoTieneGrupo() {
        Actividad pausada = actividadPublicada();
        pausada.setEstadoPublicacion("PAUSADA");
        when(actividadRepository.findById(ACTIVIDAD_ID)).thenReturn(Optional.of(pausada));

        assertThatThrownBy(() -> service.unirse(USUARIO_ID, ACTIVIDAD_ID))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    /* ===================== privacidad ===================== */

    /**
     * LO CENTRAL DEL BLOQUE: quien no es miembro recibe la ficha SIN
     * contenido. Los avisos no salen del backend.
     */
    @Test
    void unNoMiembroNoRecibeNingunAviso() {
        when(miembroRepository.existsByUsuarioIdAndActividadIdAndEstado(
                USUARIO_ID, ACTIVIDAD_ID, "ACTIVO")).thenReturn(false);

        var grupo = service.verGrupo(USUARIO_ID, ACTIVIDAD_ID);

        assertThat(grupo.getEsMiembro()).isFalse();
        assertThat(grupo.getAvisos()).isEmpty();
        /* Ni siquiera se consultan. */
        verify(avisoRepository, never())
                .findByActividadIdAndEstadoOrderByCreatedAtDesc(anyLong(), anyString(), any());
    }

    /**
     * El DUEÑO ve su grupo sin ser miembro: es su espacio y es quien
     * lo modera. Sin esto, su propio panel le devolvería vacío —el
     * publicador no "va" a su actividad, así que nunca se suma—.
     */
    @Test
    void elDuenioVeSuGrupoAunqueNoSeaMiembro() {
        when(miembroRepository.existsByUsuarioIdAndActividadIdAndEstado(
                DUENIO_ID, ACTIVIDAD_ID, "ACTIVO")).thenReturn(false);
        when(avisoRepository.findByActividadIdAndEstadoOrderByCreatedAtDesc(
                eq(ACTIVIDAD_ID), eq("VISIBLE"), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        List.of(avisoVisible())));

        var grupo = service.verGrupo(DUENIO_ID, ACTIVIDAD_ID);

        /* No es miembro (el botón lo refleja), pero ve el contenido. */
        assertThat(grupo.getEsMiembro()).isFalse();
        assertThat(grupo.getAvisos()).hasSize(1);
    }

    /** Un no-miembro tampoco puede comentar ni reaccionar. */
    @Test
    void unNoMiembroNoPuedeComentarUnAviso() {
        when(avisoRepository.findById(5L)).thenReturn(Optional.of(avisoVisible()));
        when(miembroRepository.existsByUsuarioIdAndActividadIdAndEstado(
                USUARIO_ID, ACTIVIDAD_ID, "ACTIVO")).thenReturn(false);

        assertThatThrownBy(() -> service.comentar(USUARIO_ID, 5L, "Hola"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    /* ===================== avisos ===================== */

    @Test
    void avisarNotificaAlosMiembros() {
        service.avisar(DUENIO_ID, ACTIVIDAD_ID, "Mañana se suspende por lluvia", null);

        verify(notificacionService).emitirATodos(
                eq(List.of(USUARIO_ID)), eq("AVISO_GRUPO"), anyString(), anyString());
    }

    @Test
    void pasadoElTopeDiarioNoSePuedeAvisar() {
        when(avisoRepository.countByActividadIdAndCreatedAtGreaterThanEqual(
                eq(ACTIVIDAD_ID), any(OffsetDateTime.class))).thenReturn(2L);

        assertThatThrownBy(() ->
                service.avisar(DUENIO_ID, ACTIVIDAD_ID, "Tercer aviso del día", null))
                .isInstanceOf(FiltroInvalidoException.class);

        verify(avisoRepository, never()).saveAndFlush(any(AvisoGrupo.class));
    }

    /** El publicador no puede avisar en el grupo de otro. */
    @Test
    void noSePuedeAvisarEnLaActividadDeOtroPublicador() {
        Actividad ajena = actividadPublicada();
        PerfilPublicador otro = new PerfilPublicador();
        otro.setId(999L);
        ajena.setPerfilPublicador(otro);
        when(actividadRepository.findById(ACTIVIDAD_ID)).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> service.avisar(DUENIO_ID, ACTIVIDAD_ID, "Hola", null))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    /**
     * Un comentario avisa SOLO al publicador: quince comentarios serían
     * quince campanitas para cada miembro.
     */
    @Test
    void comentarAvisaSoloAlPublicadorYNoAlResto() {
        when(avisoRepository.findById(5L)).thenReturn(Optional.of(avisoVisible()));
        when(miembroRepository.existsByUsuarioIdAndActividadIdAndEstado(
                USUARIO_ID, ACTIVIDAD_ID, "ACTIVO")).thenReturn(true);
        when(comentarioRepository.saveAndFlush(any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        service.comentar(USUARIO_ID, 5L, "Perfecto, gracias!");

        verify(notificacionService).emitir(
                eq(DUENIO_ID), eq("COMENTARIO_GRUPO"), anyString(), anyString());
        verify(notificacionService, never())
                .emitirATodos(anyList(), anyString(), anyString(), anyString());
    }

    /* ======================= fixtures ======================= */

    private MiembroActividad miembro(String estado) {
        MiembroActividad miembro = new MiembroActividad();
        miembro.setUsuarioId(USUARIO_ID);
        miembro.setActividadId(ACTIVIDAD_ID);
        miembro.setEstado(estado);
        miembro.setCreatedAt(OffsetDateTime.now().minusDays(30));
        miembro.setUpdatedAt(OffsetDateTime.now().minusDays(30));
        return miembro;
    }

    private AvisoGrupo avisoVisible() {
        AvisoGrupo aviso = new AvisoGrupo();
        aviso.setActividadId(ACTIVIDAD_ID);
        aviso.setTexto("Un aviso");
        aviso.setEstado("VISIBLE");
        aviso.setCreatedAt(OffsetDateTime.now());
        aviso.setUpdatedAt(OffsetDateTime.now());
        return aviso;
    }

    private Actividad actividadPublicada() {
        Actividad actividad = new Actividad();
        actividad.setId(ACTIVIDAD_ID);
        actividad.setTitulo("Karate");
        actividad.setSlug("karate");
        actividad.setActiva(true);
        actividad.setEstadoPublicacion("PUBLICADA");
        actividad.setPerfilPublicador(perfil());
        return actividad;
    }

    private PerfilPublicador perfil() {
        Usuario duenio = new Usuario();
        duenio.setId(DUENIO_ID);

        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setId(PERFIL_ID);
        perfil.setNombre("Club Atlético Sur");
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
