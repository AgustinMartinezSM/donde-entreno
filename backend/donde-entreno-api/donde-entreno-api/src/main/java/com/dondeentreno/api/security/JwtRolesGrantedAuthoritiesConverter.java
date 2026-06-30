package com.dondeentreno.api.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Convierte roles del JWT a authorities ROLE_* de Spring Security.
 */
public class JwtRolesGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        Object roles = jwt.getClaims().get("roles");
        if (roles instanceof Collection<?> coleccionRoles) {
            for (Object rol : coleccionRoles) {
                agregarRol(authorities, rol);
            }
        }

        agregarRol(authorities, jwt.getClaims().get("rol"));

        return List.copyOf(authorities);
    }

    private void agregarRol(Set<GrantedAuthority> authorities, Object valor) {
        if (!(valor instanceof String rol) || rol.isBlank()) {
            return;
        }

        String nombreAuthority = rol.startsWith("ROLE_") ? rol : "ROLE_" + rol;
        authorities.add(new SimpleGrantedAuthority(nombreAuthority));
    }
}
