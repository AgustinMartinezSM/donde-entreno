package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.UsuarioActualDTO;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.ImagenInvalidaException;
import com.dondeentreno.api.repository.UsuarioRepository;
import com.dondeentreno.api.storage.AlmacenArchivos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvatarUsuarioServiceTest {

    /* Firma JPEG minima valida para la deteccion por bytes. */
    private static final byte[] BYTES_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x10, 0x20};
    private static final byte[] BYTES_TEXTO = "no soy una imagen".getBytes();

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AlmacenArchivos almacenArchivos;

    @InjectMocks
    private AvatarUsuarioService service;

    @Test
    void subirAvatarGuardaPublicaYPisaLaColumna() {
        Usuario usuario = usuario(1L, null);
        when(usuarioRepository.findByIdAndActivoTrueAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(almacenArchivos.guardarPendiente(any(), eq("avatares/1"), eq("jpg")))
                .thenReturn("avatares/1/uuid.jpg");
        when(almacenArchivos.publicar("avatares/1/uuid.jpg"))
                .thenReturn("https://storage.test/publico/avatares/1/uuid.jpg");

        UsuarioActualDTO resultado = service.actualizarAvatar(1L, archivo(BYTES_JPEG));

        assertEquals("https://storage.test/publico/avatares/1/uuid.jpg", resultado.getAvatarUrl());
        assertEquals("https://storage.test/publico/avatares/1/uuid.jpg", usuario.getAvatarUrl());
        /* Sin avatar previo no hay nada que borrar del bucket. */
        verify(almacenArchivos, never()).eliminarPublicoPorUrl(anyString());
    }

    @Test
    void reemplazarAvatarBorraElAnteriorBestEffort() {
        Usuario usuario = usuario(1L, "https://storage.test/publico/avatares/1/viejo.jpg");
        when(usuarioRepository.findByIdAndActivoTrueAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(almacenArchivos.guardarPendiente(any(), eq("avatares/1"), eq("jpg")))
                .thenReturn("avatares/1/nuevo.jpg");
        when(almacenArchivos.publicar("avatares/1/nuevo.jpg"))
                .thenReturn("https://storage.test/publico/avatares/1/nuevo.jpg");

        service.actualizarAvatar(1L, archivo(BYTES_JPEG));

        verify(almacenArchivos)
                .eliminarPublicoPorUrl("https://storage.test/publico/avatares/1/viejo.jpg");
    }

    @Test
    void unFalloAlBorrarElAnteriorNoTiraLaOperacion() {
        Usuario usuario = usuario(1L, "https://storage.test/publico/avatares/1/viejo.jpg");
        when(usuarioRepository.findByIdAndActivoTrueAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(almacenArchivos.guardarPendiente(any(), anyString(), anyString()))
                .thenReturn("avatares/1/nuevo.jpg");
        when(almacenArchivos.publicar("avatares/1/nuevo.jpg"))
                .thenReturn("https://storage.test/publico/avatares/1/nuevo.jpg");
        doThrow(new IllegalArgumentException("url ajena"))
                .when(almacenArchivos)
                .eliminarPublicoPorUrl(anyString());

        UsuarioActualDTO resultado = service.actualizarAvatar(1L, archivo(BYTES_JPEG));

        assertEquals("https://storage.test/publico/avatares/1/nuevo.jpg", resultado.getAvatarUrl());
    }

    @Test
    void unArchivoQueNoEsImagenDa400SinTocarNada() {
        Usuario usuario = usuario(1L, null);
        when(usuarioRepository.findByIdAndActivoTrueAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(usuario));

        assertThrows(
                ImagenInvalidaException.class,
                () -> service.actualizarAvatar(1L, archivo(BYTES_TEXTO))
        );

        verify(almacenArchivos, never()).guardarPendiente(any(), anyString(), anyString());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void eliminarAvatarVuelveAInicialesYEsIdempotente() {
        Usuario usuario = usuario(1L, "https://storage.test/publico/avatares/1/actual.jpg");
        when(usuarioRepository.findByIdAndActivoTrueAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioActualDTO resultado = service.eliminarAvatar(1L);
        assertNull(resultado.getAvatarUrl());
        verify(almacenArchivos)
                .eliminarPublicoPorUrl("https://storage.test/publico/avatares/1/actual.jpg");

        /* Segunda pasada sin avatar: no guarda ni borra nada. */
        UsuarioActualDTO segunda = service.eliminarAvatar(1L);
        assertNull(segunda.getAvatarUrl());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void usuarioInactivoOInexistenteDa401() {
        when(usuarioRepository.findByIdAndActivoTrueAndDeletedAtIsNull(9L))
                .thenReturn(Optional.empty());

        assertThrows(
                CredencialesInvalidasException.class,
                () -> service.actualizarAvatar(9L, archivo(BYTES_JPEG))
        );
    }

    private Usuario usuario(Long id, String avatarUrl) {
        Rol rol = new Rol();
        rol.setId(1L);
        rol.setNombre("USUARIO");
        rol.setActivo(true);

        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Usuario");
        usuario.setApellido("Avatar");
        usuario.setEmail("avatar@test.test");
        usuario.setPasswordHash("hash");
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setTelefonoVerificado(false);
        usuario.setRol(rol);
        usuario.setAvatarUrl(avatarUrl);
        return usuario;
    }

    private MockMultipartFile archivo(byte[] contenido) {
        return new MockMultipartFile("archivo", "avatar.jpg", "image/jpeg", contenido);
    }
}
