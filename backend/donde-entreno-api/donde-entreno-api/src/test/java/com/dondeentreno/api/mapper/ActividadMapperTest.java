package com.dondeentreno.api.mapper;

import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Ubicacion;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ActividadMapperTest {

    @Test
    void toDTOMapeaCiudadSlug() {
        Ciudad ciudad = new Ciudad();
        ciudad.setId(1L);
        ciudad.setNombre("Mar del Plata");
        ciudad.setSlug("mar-del-plata");

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setCiudad(ciudad);

        Actividad actividad = new Actividad();
        actividad.setId(10L);
        actividad.setTitulo("Boxeo recreativo");
        actividad.setSlug("boxeo-recreativo");
        actividad.setUbicacion(ubicacion);

        ActividadDTO dto = ActividadMapper.toDTO(actividad);

        assertEquals(1L, dto.getCiudadId());
        assertEquals("Mar del Plata", dto.getCiudadNombre());
        assertEquals("mar-del-plata", dto.getCiudadSlug());
    }

    @Test
    void toDTOExponeCreatedAtComoFechaPublicacion() {
        OffsetDateTime creada = OffsetDateTime.parse("2026-08-01T10:30:00-03:00");

        Actividad actividad = new Actividad();
        actividad.setId(10L);
        actividad.setTitulo("Boxeo recreativo");
        actividad.setSlug("boxeo-recreativo");
        actividad.setCreatedAt(creada);

        ActividadDTO dto = ActividadMapper.toDTO(actividad);

        assertEquals(creada, dto.getFechaPublicacion());
    }

    @Test
    void toDTOSinCreatedAtDejaFechaPublicacionNull() {
        Actividad actividad = new Actividad();
        actividad.setId(10L);
        actividad.setTitulo("Boxeo recreativo");
        actividad.setSlug("boxeo-recreativo");

        ActividadDTO dto = ActividadMapper.toDTO(actividad);

        assertNull(dto.getFechaPublicacion());
    }
}
