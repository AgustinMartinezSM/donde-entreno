package com.dondeentreno.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Variables de entorno para el bootstrap seguro del primer SUPER_ADMIN.
 *
 * Se mapean desde:
 * DONDEENTRENO_BOOTSTRAP_SUPER_ADMIN_ENABLED
 * DONDEENTRENO_BOOTSTRAP_SUPER_ADMIN_EMAIL
 * DONDEENTRENO_BOOTSTRAP_SUPER_ADMIN_PASSWORD
 * DONDEENTRENO_BOOTSTRAP_SUPER_ADMIN_NOMBRE
 * DONDEENTRENO_BOOTSTRAP_SUPER_ADMIN_APELLIDO
 */
@ConfigurationProperties(prefix = "dondeentreno.bootstrap.super.admin")
public class SuperAdminBootstrapProperties {

    private String enabled;
    private String email;
    private String password;
    private String nombre;
    private String apellido;

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }
}
