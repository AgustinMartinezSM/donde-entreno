package com.dondeentreno.api.dto;

import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Usuario;

/**
 * Datos seguros del usuario autenticado.
 */
public class UsuarioActualDTO {

    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String rol;
    private String telefono;
    private Boolean activo;
    private Boolean emailVerificado;

    public UsuarioActualDTO() {
    }

    public UsuarioActualDTO(
            Long id,
            String nombre,
            String apellido,
            String email,
            String rol,
            String telefono,
            Boolean activo,
            Boolean emailVerificado
    ) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.rol = rol;
        this.telefono = telefono;
        this.activo = activo;
        this.emailVerificado = emailVerificado;
    }

    public static UsuarioActualDTO desdeUsuario(Usuario usuario) {
        Rol rolUsuario = usuario.getRol();
        return new UsuarioActualDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                rolUsuario != null ? rolUsuario.getNombre() : null,
                usuario.getTelefono(),
                usuario.getActivo(),
                usuario.getEmailVerificado()
        );
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getEmail() {
        return email;
    }

    public String getRol() {
        return rol;
    }

    public String getTelefono() {
        return telefono;
    }

    public Boolean getActivo() {
        return activo;
    }

    public Boolean getEmailVerificado() {
        return emailVerificado;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public void setEmailVerificado(Boolean emailVerificado) {
        this.emailVerificado = emailVerificado;
    }
}
