package com.dondeentreno.api.mapper;

import com.dondeentreno.api.dto.PublicadorActividadDetalleDTO;
import com.dondeentreno.api.dto.PublicadorActividadResumenDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.CategoriaDeportiva;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.HorarioActividad;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.SolicitudPublicacion;
import com.dondeentreno.api.entity.Ubicacion;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class PublicadorActividadMapperTest {

    @Test
    void toResumenDTOMapeaRelacionesEImagenPrincipal() {
        Actividad actividad = actividadCompleta();

        PublicadorActividadResumenDTO dto =
                PublicadorActividadMapper.toResumenDTO(actividad, "https://img.test/principal.jpg");

        assertEquals(100L, dto.getId());
        assertEquals("Boxeo recreativo", dto.getTitulo());
        assertEquals("boxeo-recreativo", dto.getSlug());
        assertEquals("boxeo-recreativo", dto.getSlugPublico());
        assertEquals("Boxeo", dto.getDeporteNombre());
        assertEquals("boxeo", dto.getDeporteSlug());
        assertEquals("Combate", dto.getCategoriaDeportivaNombre());
        assertEquals("Mar del Plata", dto.getCiudadNombre());
        assertEquals("mar-del-plata", dto.getCiudadSlug());
        assertEquals("Centro", dto.getBarrioNombre());
        assertEquals("PUBLICADA", dto.getEstadoPublicacion());
        assertEquals(true, dto.getActiva());
        assertEquals("PRESENCIAL", dto.getModalidad());
        assertEquals("PRINCIPIANTE", dto.getNivel());
        assertEquals(new BigDecimal("18000.00"), dto.getPrecioReferencia());
        assertEquals("https://img.test/principal.jpg", dto.getImagenPrincipalUrl());
    }

    @Test
    void toDetalleDTOMapeaHorariosImagenesYSolicitudOrigen() {
        Actividad actividad = actividadCompleta();
        HorarioActividad horario = new HorarioActividad();
        horario.setId(200L);
        horario.setDiaSemana("LUNES");
        horario.setHoraInicio(LocalTime.of(18, 0));
        horario.setHoraFin(LocalTime.of(19, 30));
        horario.setObservacion("Traer guantes");

        Imagen galeria = imagen(301L, "GALERIA", "https://img.test/galeria.jpg", 1);
        Imagen principal = imagen(302L, "PRINCIPAL", "https://img.test/principal.jpg", 2);

        SolicitudPublicacion solicitud = new SolicitudPublicacion();
        solicitud.setId(400L);
        solicitud.setCodigoSeguimiento("DEP-20260704-ABC12345");

        PublicadorActividadDetalleDTO dto = PublicadorActividadMapper.toDetalleDTO(
                actividad,
                List.of(horario),
                List.of(galeria, principal),
                solicitud
        );

        assertEquals(100L, dto.getId());
        assertEquals("Clases para principiantes.", dto.getDescripcion());
        assertEquals("RECREATIVO", dto.getEnfoque());
        assertEquals(false, dto.getRequiereInscripcion());
        assertEquals(false, dto.getCuposLimitados());
        assertEquals("Escuela Norte", dto.getNombreLugar());
        assertEquals("Av. Independencia 1234", dto.getDireccion());
        assertEquals("Planta alta", dto.getReferenciaUbicacion());
        assertEquals("+54 9 223 512-3456", dto.getWhatsapp());
        assertEquals("@escuelanorte", dto.getInstagram());
        assertEquals("contacto@ejemplo.com", dto.getEmail());
        assertEquals(30L, dto.getPerfilPublicadorId());
        assertEquals("Perfil Publicador", dto.getPerfilPublicadorNombre());
        assertEquals("PROFESOR_INDEPENDIENTE", dto.getPerfilPublicadorTipo());
        assertEquals(400L, dto.getSolicitudOrigenId());
        assertEquals("DEP-20260704-ABC12345", dto.getSolicitudCodigoSeguimiento());
        assertEquals("https://img.test/principal.jpg", dto.getImagenPrincipalUrl());
        assertEquals(1, dto.getHorarios().size());
        assertEquals("LUNES", dto.getHorarios().get(0).getDiaSemana());
        assertEquals(2, dto.getImagenes().size());
        assertEquals("GALERIA", dto.getImagenes().get(0).getTipoImagen());
    }

    @Test
    void resolverImagenPrincipalUrlPrefiereTipoPrincipalYSiNoUsaPrimera() {
        Imagen galeria = imagen(301L, "GALERIA", "https://img.test/galeria.jpg", 1);
        Imagen principal = imagen(302L, "PRINCIPAL", "https://img.test/principal.jpg", 2);

        assertEquals(
                "https://img.test/principal.jpg",
                PublicadorActividadMapper.resolverImagenPrincipalUrl(List.of(galeria, principal))
        );
        assertEquals(
                "https://img.test/galeria.jpg",
                PublicadorActividadMapper.resolverImagenPrincipalUrl(List.of(galeria))
        );
        assertNull(PublicadorActividadMapper.resolverImagenPrincipalUrl(List.of()));
    }

    @Test
    void mappersSonNullSafeYNoExponenCamposSensibles() {
        assertNull(PublicadorActividadMapper.toResumenDTO(null, null));
        assertNull(PublicadorActividadMapper.toDetalleDTO(null, null, null, null));
        assertNull(PublicadorActividadMapper.toHorarioDTO(null));
        assertNull(PublicadorActividadMapper.toImagenDTO(null));

        assertFalse(tieneCampo(PublicadorActividadDetalleDTO.class, "passwordHash"));
        assertFalse(tieneCampo(PublicadorActividadDetalleDTO.class, "token"));
        assertFalse(tieneCampo(PublicadorActividadDetalleDTO.class, "ipOrigen"));
    }

    private boolean tieneCampo(Class<?> tipo, String nombreCampo) {
        return Arrays.stream(tipo.getDeclaredFields())
                .map(Field::getName)
                .anyMatch(nombreCampo::equals);
    }

    private Actividad actividadCompleta() {
        CategoriaDeportiva categoria = new CategoriaDeportiva();
        categoria.setId(5L);
        categoria.setNombre("Combate");
        categoria.setSlug("combate");

        Deporte deporte = new Deporte();
        deporte.setId(1L);
        deporte.setNombre("Boxeo");
        deporte.setSlug("boxeo");
        deporte.setCategoriaDeportiva(categoria);

        Ciudad ciudad = new Ciudad();
        ciudad.setId(2L);
        ciudad.setNombre("Mar del Plata");
        ciudad.setSlug("mar-del-plata");

        Barrio barrio = new Barrio();
        barrio.setId(3L);
        barrio.setNombre("Centro");

        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setId(30L);
        perfil.setNombre("Perfil Publicador");
        perfil.setTipoPublicador("PROFESOR_INDEPENDIENTE");

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setId(4L);
        ubicacion.setNombre("Escuela Norte");
        ubicacion.setDireccion("Av. Independencia 1234");
        ubicacion.setReferencia("Planta alta");
        ubicacion.setCiudad(ciudad);
        ubicacion.setBarrio(barrio);
        ubicacion.setPerfilPublicador(perfil);

        Actividad actividad = new Actividad();
        actividad.setId(100L);
        actividad.setTitulo("Boxeo recreativo");
        actividad.setSlug("boxeo-recreativo");
        actividad.setDescripcion("Clases para principiantes.");
        actividad.setPerfilPublicador(perfil);
        actividad.setDeporte(deporte);
        actividad.setUbicacion(ubicacion);
        actividad.setEstadoPublicacion("PUBLICADA");
        actividad.setActiva(true);
        actividad.setModalidad("PRESENCIAL");
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(60);
        actividad.setPrecioReferencia(new BigDecimal("18000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 512-3456");
        actividad.setInstagramContacto("@escuelanorte");
        actividad.setEmailContacto("contacto@ejemplo.com");
        actividad.setCreatedAt(OffsetDateTime.parse("2026-07-04T10:00:00-03:00"));
        return actividad;
    }

    private Imagen imagen(Long id, String tipo, String url, int orden) {
        Imagen imagen = new Imagen();
        imagen.setId(id);
        imagen.setTipoImagen(tipo);
        imagen.setUrl(url);
        imagen.setTitulo("Imagen " + id);
        imagen.setDescripcion("Descripcion " + id);
        imagen.setOrden(orden);
        imagen.setActiva(true);
        return imagen;
    }
}
