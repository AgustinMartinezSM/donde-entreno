package com.dondeentreno.api.mapper;

import com.dondeentreno.api.dto.SolicitudPublicadorDetalleDTO;
import com.dondeentreno.api.dto.SolicitudPublicadorHorarioDTO;
import com.dondeentreno.api.dto.SolicitudPublicadorResumenDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.SolicitudPublicacion;
import com.dondeentreno.api.entity.SolicitudPublicacionHorario;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolicitudPublicadorMapperTest {

    @Test
    void toResumenDTOMapeaCamposVisibles() {
        SolicitudPublicacion solicitud = solicitudCompleta();

        SolicitudPublicadorResumenDTO dto = SolicitudPublicadorMapper.toResumenDTO(solicitud);

        assertEquals(10L, dto.getId());
        assertEquals("DEP-20260704-ABC12345", dto.getCodigoSeguimiento());
        assertEquals("PENDIENTE", dto.getEstado());
        assertEquals("Boxeo recreativo", dto.getNombreActividad());
        assertEquals(1L, dto.getDeporteId());
        assertEquals("Boxeo", dto.getDeporteNombre());
        assertEquals(2L, dto.getCiudadId());
        assertEquals("Mar del Plata", dto.getCiudadNombre());
        assertEquals(3L, dto.getBarrioId());
        assertEquals("Centro", dto.getBarrioNombre());
        assertEquals("Motivo visible", dto.getMotivoRechazo());
    }

    @Test
    void toDetalleDTOMapeaDetalleYHorariosSinDatosSensibles() {
        SolicitudPublicacion solicitud = solicitudCompleta();
        SolicitudPublicacionHorario horario = horario();

        SolicitudPublicadorDetalleDTO dto = SolicitudPublicadorMapper.toDetalleDTO(solicitud, List.of(horario));

        assertEquals(10L, dto.getId());
        assertEquals("Clases para principiantes.", dto.getDescripcion());
        assertEquals("PRINCIPIANTE", dto.getNivel());
        assertEquals(new BigDecimal("18000.00"), dto.getPrecioReferencia());
        assertEquals("Escuela Norte", dto.getNombreLugar());
        assertEquals("+54 9 223 512-3456", dto.getWhatsapp());
        assertEquals("contacto@ejemplo.com", dto.getEmail());
        assertEquals(99L, dto.getActividadGeneradaId());
        assertEquals(1, dto.getHorarios().size());
        assertEquals("LUNES", dto.getHorarios().get(0).getDiaSemana());
        assertFalse(tieneCampo(dto.getClass(), "ipOrigen"));
        assertFalse(tieneCampo(dto.getClass(), "whatsappNormalizado"));
        assertFalse(tieneCampo(dto.getClass(), "observacionesRevision"));
        assertFalse(tieneCampo(dto.getClass(), "revisadoPorUsuario"));
        assertFalse(tieneCampo(dto.getClass(), "passwordHash"));
    }

    @Test
    void toDetalleDTOSinHorariosDevuelveListaVaciaYNullSafe() {
        assertNull(SolicitudPublicadorMapper.toDetalleDTO(null, null));

        SolicitudPublicadorDetalleDTO dto = SolicitudPublicadorMapper.toDetalleDTO(solicitudCompleta(), null);

        assertNotNull(dto.getHorarios());
        assertTrue(dto.getHorarios().isEmpty());
    }

    @Test
    void toHorarioDTOMapeaHorario() {
        SolicitudPublicadorHorarioDTO dto = SolicitudPublicadorMapper.toHorarioDTO(horario());

        assertEquals(50L, dto.getId());
        assertEquals("LUNES", dto.getDiaSemana());
        assertEquals(LocalTime.of(18, 0), dto.getHoraInicio());
        assertEquals(LocalTime.of(19, 30), dto.getHoraFin());
        assertEquals("Cancha 1", dto.getObservacion());
    }

    private SolicitudPublicacion solicitudCompleta() {
        OffsetDateTime ahora = OffsetDateTime.parse("2026-07-04T10:00:00-03:00");
        Deporte deporte = new Deporte();
        deporte.setId(1L);
        deporte.setNombre("Boxeo");

        Ciudad ciudad = new Ciudad();
        ciudad.setId(2L);
        ciudad.setNombre("Mar del Plata");

        Barrio barrio = new Barrio();
        barrio.setId(3L);
        barrio.setNombre("Centro");

        Actividad actividad = new Actividad();
        actividad.setId(99L);

        SolicitudPublicacion solicitud = new SolicitudPublicacion();
        solicitud.setId(10L);
        solicitud.setCodigoSeguimiento("DEP-20260704-ABC12345");
        solicitud.setEstado("PENDIENTE");
        solicitud.setNombreActividad("Boxeo recreativo");
        solicitud.setDeporte(deporte);
        solicitud.setCiudad(ciudad);
        solicitud.setBarrio(barrio);
        solicitud.setCreatedAt(ahora);
        solicitud.setUpdatedAt(ahora);
        solicitud.setRevisionIniciadaAt(ahora.plusHours(1));
        solicitud.setRevisionFinalizadaAt(ahora.plusHours(2));
        solicitud.setDescripcion("Clases para principiantes.");
        solicitud.setNivel("PRINCIPIANTE");
        solicitud.setEnfoque("RECREATIVO");
        solicitud.setModalidad("PRESENCIAL");
        solicitud.setEdadMinima(18);
        solicitud.setEdadMaxima(60);
        solicitud.setPrecioReferencia(new BigDecimal("18000.00"));
        solicitud.setMostrarPrecio(true);
        solicitud.setNombreLugar("Escuela Norte");
        solicitud.setDireccion("Av. Independencia 1234");
        solicitud.setReferenciaUbicacion("Entrada lateral");
        solicitud.setWhatsapp("+54 9 223 512-3456");
        solicitud.setWhatsappNormalizado("5492235123456");
        solicitud.setInstagram("@escuelanorte");
        solicitud.setEmail("contacto@ejemplo.com");
        solicitud.setObservacionesSolicitante("Observacion visible");
        solicitud.setMotivoRechazo("Motivo visible");
        solicitud.setObservacionesRevision("Observacion interna");
        solicitud.setActividadGenerada(actividad);
        return solicitud;
    }

    private SolicitudPublicacionHorario horario() {
        SolicitudPublicacionHorario horario = new SolicitudPublicacionHorario();
        horario.setId(50L);
        horario.setDiaSemana("LUNES");
        horario.setHoraInicio(LocalTime.of(18, 0));
        horario.setHoraFin(LocalTime.of(19, 30));
        horario.setObservacion("Cancha 1");
        return horario;
    }

    private boolean tieneCampo(Class<?> clase, String nombreCampo) {
        return Arrays.stream(clase.getDeclaredFields())
                .map(Field::getName)
                .anyMatch(nombreCampo::equals);
    }
}
