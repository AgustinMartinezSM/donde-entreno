package com.dondeentreno.api.dto;

/**
 * DTO de revisor para solicitudes de publicacion en el panel admin.
 */
public class SolicitudPublicacionAdminRevisorDTO {

    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String rol;

    public SolicitudPublicacionAdminRevisorDTO() {
    }

    public SolicitudPublicacionAdminRevisorDTO(
            Long id,
            String nombre,
            String apellido,
            String email,
            String rol
    ) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.rol = rol;
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
}
