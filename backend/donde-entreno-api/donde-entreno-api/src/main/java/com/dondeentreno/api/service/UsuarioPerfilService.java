package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.UsuarioActualDTO;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Edición de datos del usuario (Fase 2 social). Nombre y apellido,
 * inline desde /configuracion. El email no se toca acá: es la
 * credencial de login (ver plan de la fase).
 */
@Service
public class UsuarioPerfilService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioPerfilService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public UsuarioActualDTO actualizarDatos(Long usuarioId, String nombre, String apellido) {
        if (usuarioId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }

        Usuario usuario = usuarioRepository.findByIdAndActivoTrueAndDeletedAtIsNull(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado."));

        usuario.setNombre(nombre.trim());
        usuario.setApellido(apellido.trim());
        usuario.setUpdatedAt(OffsetDateTime.now());

        return UsuarioActualDTO.desdeUsuario(usuarioRepository.save(usuario));
    }
}
