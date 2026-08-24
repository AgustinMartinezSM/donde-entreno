package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.ComentarioImagen;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ComentarioImagenRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComentarioImagenServiceTest {

    private static final Long USUARIO_ID = 10L;
    private static final Long DUENIO_ID = 50L;
    private static final Long IMAGEN_ID = 7L;

    @Mock
    private ComentarioImagenRepository comentarioImagenRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NotificacionService notificacionService;

    private ComentarioImagenService service;

    @BeforeEach
    void setUp() {
        service = new ComentarioImagenService(
                comentarioImagenRepository,
                imagenRepository,
                usuarioRepository,
                notificacionService
        );
    }

    @Test
    void comentarPublicaDirectoYNotificaAlDuenio() {
        when(imagenRepository.findById(IMAGEN_ID))
                .thenReturn(Optional.of(fotoDePerfilVisible(true)));
        when(comentarioImagenRepository
                .countByUsuarioIdAndCreatedAtGreaterThanEqual(eq(USUARIO_ID), any()))
                .thenReturn(0L);
        when(comentarioImagenRepository.save(any(ComentarioImagen.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());

        var dto = service.comentar(USUARIO_ID, IMAGEN_ID, "  ¡Qué buen lugar!  ");

        assertEquals("¡Qué buen lugar!", dto.getTexto());
        verify(notificacionService).emitir(eq(DUENIO_ID), eq("COMENTARIO_NUEVO"), any(), any());
    }

    @Test
    void conComentariosDesactivadosNoSePuedeComentar() {
        when(imagenRepository.findById(IMAGEN_ID))
                .thenReturn(Optional.of(fotoDePerfilVisible(false)));

        assertThrows(
                FiltroInvalidoException.class,
                () -> service.comentar(USUARIO_ID, IMAGEN_ID, "Hola")
        );
    }

    @Test
    void alTopeDiarioCorta() {
        when(imagenRepository.findById(IMAGEN_ID))
                .thenReturn(Optional.of(fotoDePerfilVisible(true)));
        when(comentarioImagenRepository
                .countByUsuarioIdAndCreatedAtGreaterThanEqual(eq(USUARIO_ID), any()))
                .thenReturn(20L);

        assertThrows(
                FiltroInvalidoException.class,
                () -> service.comentar(USUARIO_ID, IMAGEN_ID, "Otro más")
        );
    }

    @Test
    void soloElDuenioDeLaFotoOculta() {
        ComentarioImagen comentario = comentarioVisible();
        when(comentarioImagenRepository.findById(1L)).thenReturn(Optional.of(comentario));
        when(imagenRepository.findById(IMAGEN_ID))
                .thenReturn(Optional.of(fotoDePerfilVisible(true)));

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.ocultarPorPublicador(999L, 1L)
        );

        when(comentarioImagenRepository.save(comentario)).thenReturn(comentario);
        service.ocultarPorPublicador(DUENIO_ID, 1L);
        assertEquals("OCULTO_POR_PUBLICADOR", comentario.getEstado());
    }

    @Test
    void eliminarPropioSoloTocaLoDelAutor() {
        ComentarioImagen ajeno = comentarioVisible();
        ajeno.setUsuarioId(999L);
        when(comentarioImagenRepository.findById(1L)).thenReturn(Optional.of(ajeno));

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.eliminarPropio(USUARIO_ID, 1L)
        );

        ComentarioImagen propio = comentarioVisible();
        lenient().when(comentarioImagenRepository.findById(2L)).thenReturn(Optional.of(propio));
        lenient().when(comentarioImagenRepository.save(propio)).thenReturn(propio);

        service.eliminarPropio(USUARIO_ID, 2L);
        assertEquals("ELIMINADO_POR_USUARIO", propio.getEstado());
    }

    private ComentarioImagen comentarioVisible() {
        ComentarioImagen comentario = new ComentarioImagen();
        comentario.setImagenId(IMAGEN_ID);
        comentario.setUsuarioId(USUARIO_ID);
        comentario.setTexto("Hola");
        comentario.setEstado("VISIBLE");
        comentario.setCreatedAt(OffsetDateTime.now());
        return comentario;
    }

    private Imagen fotoDePerfilVisible(boolean comentariosActivados) {
        Usuario duenio = new Usuario();
        try {
            java.lang.reflect.Field campo = Usuario.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(duenio, DUENIO_ID);
        } catch (ReflectiveOperationException excepcion) {
            throw new IllegalStateException(excepcion);
        }

        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setUsuario(duenio);

        Imagen imagen = new Imagen();
        imagen.setPerfilPublicador(perfil);
        imagen.setActiva(true);
        imagen.setEstadoModeracion("APROBADA");
        imagen.setComentariosActivados(comentariosActivados);
        return imagen;
    }
}
