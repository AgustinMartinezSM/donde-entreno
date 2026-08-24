package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.UsuarioActualDTO;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioPerfilServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioPerfilService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioPerfilService(usuarioRepository);
    }

    @Test
    void actualizaNombreYApellidoConTrim() {
        Usuario usuario = usuarioBase();
        when(usuarioRepository.findByIdAndActivoTrueAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        UsuarioActualDTO dto = service.actualizarDatos(10L, "  Agustín ", " Martínez  ");

        assertEquals("Agustín", usuario.getNombre());
        assertEquals("Martínez", usuario.getApellido());
        assertEquals("Agustín", dto.getNombre());
    }

    @Test
    void sinSesionExigeAutenticacion() {
        assertThrows(
                CredencialesInvalidasException.class,
                () -> service.actualizarDatos(null, "Nombre", "Apellido")
        );
    }

    private Usuario usuarioBase() {
        Rol rol = new Rol();
        rol.setNombre("USUARIO");

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Viejo");
        usuario.setApellido("Nombre");
        usuario.setEmail("test@dondeentreno.test");
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        return usuario;
    }
}
