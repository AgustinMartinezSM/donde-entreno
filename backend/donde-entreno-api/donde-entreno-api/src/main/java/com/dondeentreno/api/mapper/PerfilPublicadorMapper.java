package com.dondeentreno.api.mapper;

import com.dondeentreno.api.dto.PerfilPublicadorDTO;
import com.dondeentreno.api.entity.PerfilPublicador;

/**
 * Mapper de PerfilPublicador.
 *
 * Convierte una entidad PerfilPublicador en un DTO público
 * preparado para devolver por la API.
 */
public class PerfilPublicadorMapper {

    /**
     * Convierte PerfilPublicador a PerfilPublicadorDTO.
     *
     * @param perfil entidad obtenida desde PostgreSQL.
     * @return DTO listo para devolver al frontend.
     */
    public static PerfilPublicadorDTO toDTO(PerfilPublicador perfil) {
        if (perfil == null) {
            return null;
        }

        return new PerfilPublicadorDTO(
                perfil.getId(),
                perfil.getNombre(),
                perfil.getTipoPublicador(),
                perfil.getDescripcion(),
                perfil.getEmailContacto(),
                perfil.getTelefonoContacto(),
                perfil.getWhatsapp(),
                perfil.getInstagram(),
                perfil.getSitioWeb(),
                perfil.getVerificado()
        );
    }
}