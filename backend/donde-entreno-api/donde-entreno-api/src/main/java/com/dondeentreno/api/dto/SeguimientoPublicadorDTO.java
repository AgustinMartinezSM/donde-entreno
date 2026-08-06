package com.dondeentreno.api.dto;

import java.time.OffsetDateTime;

/**
 * Un publicador seguido por el usuario, para la lista de
 * "Publicadores que sigo".
 */
public class SeguimientoPublicadorDTO {

    private final Long perfilPublicadorId;
    private final String perfilPublicadorNombre;
    private final String tipoPublicador;
    private final String ciudadPrincipalNombre;
    private final OffsetDateTime seguidoDesde;

    public SeguimientoPublicadorDTO(
            Long perfilPublicadorId,
            String perfilPublicadorNombre,
            String tipoPublicador,
            String ciudadPrincipalNombre,
            OffsetDateTime seguidoDesde
    ) {
        this.perfilPublicadorId = perfilPublicadorId;
        this.perfilPublicadorNombre = perfilPublicadorNombre;
        this.tipoPublicador = tipoPublicador;
        this.ciudadPrincipalNombre = ciudadPrincipalNombre;
        this.seguidoDesde = seguidoDesde;
    }

    public Long getPerfilPublicadorId() {
        return perfilPublicadorId;
    }

    public String getPerfilPublicadorNombre() {
        return perfilPublicadorNombre;
    }

    public String getTipoPublicador() {
        return tipoPublicador;
    }

    public String getCiudadPrincipalNombre() {
        return ciudadPrincipalNombre;
    }

    public OffsetDateTime getSeguidoDesde() {
        return seguidoDesde;
    }
}
