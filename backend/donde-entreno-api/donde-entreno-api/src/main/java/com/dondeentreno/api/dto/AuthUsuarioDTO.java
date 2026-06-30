package com.dondeentreno.api.dto;

import com.dondeentreno.api.security.UsuarioPrincipal;

/**
 * Usuario autenticado que se devuelve al frontend sin datos sensibles.
 */
public class AuthUsuarioDTO {

    private Long id;
    private String email;
    private String nombre;
    private String apellido;
    private String rol;

    public AuthUsuarioDTO() {
    }

    public AuthUsuarioDTO(Long id, String email, String nombre, String apellido, String rol) {
        this.id = id;
        this.email = email;
        this.nombre = nombre;
        this.apellido = apellido;
        this.rol = rol;
    }

    public static AuthUsuarioDTO desdePrincipal(UsuarioPrincipal principal) {
        return new AuthUsuarioDTO(
                principal.getId(),
                principal.getEmail(),
                principal.getNombre(),
                principal.getApellido(),
                principal.getRol()
        );
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getRol() {
        return rol;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
