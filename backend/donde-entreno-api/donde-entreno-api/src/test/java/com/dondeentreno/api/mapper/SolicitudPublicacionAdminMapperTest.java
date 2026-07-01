package com.dondeentreno.api.mapper;

import com.dondeentreno.api.dto.SolicitudPublicacionAdminDetalleDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminHorarioDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminResumenDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminRevisorDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.SolicitudPublicacion;
import com.dondeentreno.api.entity.SolicitudPublicacionHorario;
import com.dondeentreno.api.entity.Usuario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolicitudPublicacionAdminMapperTest {

    @Test
    void toResumenDTOConReferenciasExistentesMapeaCamposPrincipalesYFechasAdmin() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-01T10:00:00-03:00");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-01T11:00:00-03:00");
        OffsetDateTime revisionIniciadaAt = OffsetDateTime.parse("2026-06-02T09:00:00-03:00");
        OffsetDateTime revisionFinalizadaAt = OffsetDateTime.parse("2026-06-02T10:00:00-03:00");
        SolicitudPublicacion solicitud = solicitudBase(createdAt, updatedAt, revisionIniciadaAt, revisionFinalizadaAt);
        solicitud.setDeporte(deporte(7L, "Boxeo"));
        solicitud.setCiudad(ciudad(8L, "Mar del Plata"));
        solicitud.setBarrio(barrio(9L, "Centro"));

        SolicitudPublicacionAdminResumenDTO dto = SolicitudPublicacionAdminMapper.toResumenDTO(solicitud);

        assertEquals(1L, dto.getId());
        assertEquals("DEP-20260601-ABC12345", dto.getCodigoSeguimiento());
        assertEquals("EN_REVISION", dto.getEstado());
        assertEquals("FORMULARIO_WEB", dto.getOrigen());
        assertEquals("ESCUELA_DEPORTIVA", dto.getTipoPublicador());
        assertEquals("Escuela Norte", dto.getNombrePublicador());
        assertEquals("Boxeo recreativo", dto.getNombreActividad());
        assertEquals(7L, dto.getDeporteId());
        assertEquals("Boxeo", dto.getDeporteNombre());
        assertEquals(8L, dto.getCiudadId());
        assertEquals("Mar del Plata", dto.getCiudadNombre());
        assertEquals(9L, dto.getBarrioId());
        assertEquals("Centro", dto.getBarrioNombre());
        assertEquals("admin-test@example.com", dto.getEmail());
        assertEquals("+54 9 223 512-3456", dto.getWhatsapp());
        assertEquals(createdAt, dto.getCreatedAt());
        assertEquals(updatedAt, dto.getUpdatedAt());
        assertEquals(revisionIniciadaAt, dto.getRevisionIniciadaAt());
        assertEquals(revisionFinalizadaAt, dto.getRevisionFinalizadaAt());
    }

    @Test
    void toResumenDTOConValoresOtroMapeaReferenciasNull() {
        SolicitudPublicacion solicitud = solicitudBase(
                OffsetDateTime.parse("2026-06-01T10:00:00-03:00"),
                OffsetDateTime.parse("2026-06-01T11:00:00-03:00"),
                null,
                null
        );
        solicitud.setDeporte(null);
        solicitud.setDeporteOtro("Calistenia");
        solicitud.setCiudad(null);
        solicitud.setCiudadOtra("Miramar");
        solicitud.setBarrio(null);
        solicitud.setBarrioOtro("Zona norte");

        SolicitudPublicacionAdminResumenDTO dto = SolicitudPublicacionAdminMapper.toResumenDTO(solicitud);

        assertNull(dto.getDeporteId());
        assertNull(dto.getDeporteNombre());
        assertEquals("Calistenia", dto.getDeporteOtro());
        assertNull(dto.getCiudadId());
        assertNull(dto.getCiudadNombre());
        assertEquals("Miramar", dto.getCiudadOtra());
        assertNull(dto.getBarrioId());
        assertNull(dto.getBarrioNombre());
        assertEquals("Zona norte", dto.getBarrioOtro());
    }

    @Test
    void toDetalleDTOMapeaDatosCompletosHorariosRevisorYActividadGenerada() throws Exception {
        SolicitudPublicacion solicitud = solicitudBase(
                OffsetDateTime.parse("2026-06-01T10:00:00-03:00"),
                OffsetDateTime.parse("2026-06-01T11:00:00-03:00"),
                OffsetDateTime.parse("2026-06-02T09:00:00-03:00"),
                OffsetDateTime.parse("2026-06-02T10:00:00-03:00")
        );
        solicitud.setDeporte(deporte(7L, "Boxeo"));
        solicitud.setCiudad(ciudad(8L, "Mar del Plata"));
        solicitud.setBarrio(barrio(9L, "Centro"));
        solicitud.setRevisadoPorUsuario(usuarioRevisor());

        Actividad actividad = new Actividad();
        actividad.setId(55L);
        solicitud.setActividadGenerada(actividad);

        List<SolicitudPublicacionHorario> horarios = List.of(horario(101L, "LUNES"));

        SolicitudPublicacionAdminDetalleDTO dto = SolicitudPublicacionAdminMapper.toDetalleDTO(solicitud, horarios);

        assertEquals(1L, dto.getId());
        assertEquals("Clases para principiantes.", dto.getDescripcion());
        assertEquals("PRINCIPIANTE", dto.getNivel());
        assertEquals("RECREATIVO", dto.getEnfoque());
        assertEquals("PRESENCIAL", dto.getModalidad());
        assertEquals(18, dto.getEdadMinima());
        assertEquals(60, dto.getEdadMaxima());
        assertEquals(new BigDecimal("18000.00"), dto.getPrecioReferencia());
        assertEquals(Boolean.TRUE, dto.getMostrarPrecio());
        assertEquals("Escuela Norte", dto.getNombreLugar());
        assertEquals("Av. Independencia 1234", dto.getDireccion());
        assertEquals("Entrada por calle lateral", dto.getReferenciaUbicacion());
        assertEquals("@escuelanorte", dto.getInstagram());
        assertEquals("Prefiere contacto por la tarde.", dto.getObservacionesSolicitante());
        assertEquals("Falta documentacion", dto.getMotivoRechazo());
        assertEquals("Revision interna", dto.getObservacionesRevision());
        assertEquals(55L, dto.getActividadGeneradaId());

        assertNotNull(dto.getRevisor());
        assertEquals(20L, dto.getRevisor().getId());
        assertEquals("Admin", dto.getRevisor().getNombre());
        assertEquals("Principal", dto.getRevisor().getApellido());
        assertEquals("admin@example.com", dto.getRevisor().getEmail());
        assertEquals("SUPER_ADMIN", dto.getRevisor().getRol());

        assertEquals(1, dto.getHorarios().size());
        assertEquals(101L, dto.getHorarios().get(0).getId());
        assertEquals("LUNES", dto.getHorarios().get(0).getDiaSemana());

        assertThrows(NoSuchMethodException.class,
                () -> SolicitudPublicacionAdminRevisorDTO.class.getDeclaredMethod("getPasswordHash"));
        assertThrows(NoSuchMethodException.class,
                () -> SolicitudPublicacionAdminDetalleDTO.class.getDeclaredMethod("getWhatsappNormalizado"));
        assertThrows(NoSuchMethodException.class,
                () -> SolicitudPublicacionAdminDetalleDTO.class.getDeclaredMethod("getIpOrigen"));
    }

    @Test
    void toDetalleDTOConHorariosNullDevuelveListaVacia() {
        SolicitudPublicacion solicitud = solicitudBase(
                OffsetDateTime.parse("2026-06-01T10:00:00-03:00"),
                OffsetDateTime.parse("2026-06-01T11:00:00-03:00"),
                null,
                null
        );

        SolicitudPublicacionAdminDetalleDTO dto = SolicitudPublicacionAdminMapper.toDetalleDTO(solicitud, null);

        assertNotNull(dto.getHorarios());
        assertTrue(dto.getHorarios().isEmpty());
    }

    @Test
    void toHorarioDTOMapeaCampos() {
        SolicitudPublicacionHorario horario = horario(101L, "LUNES");

        SolicitudPublicacionAdminHorarioDTO dto = SolicitudPublicacionAdminMapper.toHorarioDTO(horario);

        assertEquals(101L, dto.getId());
        assertEquals("LUNES", dto.getDiaSemana());
        assertEquals(LocalTime.of(18, 0), dto.getHoraInicio());
        assertEquals(LocalTime.of(19, 30), dto.getHoraFin());
        assertEquals("Salon 1", dto.getObservacion());
    }

    @Test
    void toRevisorDTOMapeaDatosSinPasswordHash() {
        Usuario usuario = usuarioRevisor();

        SolicitudPublicacionAdminRevisorDTO dto = SolicitudPublicacionAdminMapper.toRevisorDTO(usuario);

        assertEquals(20L, dto.getId());
        assertEquals("Admin", dto.getNombre());
        assertEquals("Principal", dto.getApellido());
        assertEquals("admin@example.com", dto.getEmail());
        assertEquals("SUPER_ADMIN", dto.getRol());
        assertThrows(NoSuchMethodException.class,
                () -> SolicitudPublicacionAdminRevisorDTO.class.getDeclaredMethod("getPasswordHash"));
    }

    private SolicitudPublicacion solicitudBase(
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime revisionIniciadaAt,
            OffsetDateTime revisionFinalizadaAt
    ) {
        SolicitudPublicacion solicitud = new SolicitudPublicacion();
        solicitud.setId(1L);
        solicitud.setCodigoSeguimiento("DEP-20260601-ABC12345");
        solicitud.setEstado("EN_REVISION");
        solicitud.setOrigen("FORMULARIO_WEB");
        solicitud.setTipoPublicador("ESCUELA_DEPORTIVA");
        solicitud.setNombrePublicador("Escuela Norte");
        solicitud.setNombreActividad("Boxeo recreativo");
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
        solicitud.setReferenciaUbicacion("Entrada por calle lateral");
        solicitud.setWhatsapp("+54 9 223 512-3456");
        solicitud.setWhatsappNormalizado("5492235123456");
        solicitud.setInstagram("@escuelanorte");
        solicitud.setEmail("admin-test@example.com");
        solicitud.setObservacionesSolicitante("Prefiere contacto por la tarde.");
        solicitud.setAceptaCondiciones(true);
        solicitud.setMotivoRechazo("Falta documentacion");
        solicitud.setObservacionesRevision("Revision interna");
        solicitud.setCreatedAt(createdAt);
        solicitud.setUpdatedAt(updatedAt);
        solicitud.setRevisionIniciadaAt(revisionIniciadaAt);
        solicitud.setRevisionFinalizadaAt(revisionFinalizadaAt);
        return solicitud;
    }

    private Deporte deporte(Long id, String nombre) {
        Deporte deporte = new Deporte();
        deporte.setId(id);
        deporte.setNombre(nombre);
        return deporte;
    }

    private Ciudad ciudad(Long id, String nombre) {
        Ciudad ciudad = new Ciudad();
        ciudad.setId(id);
        ciudad.setNombre(nombre);
        return ciudad;
    }

    private Barrio barrio(Long id, String nombre) {
        Barrio barrio = new Barrio();
        barrio.setId(id);
        barrio.setNombre(nombre);
        return barrio;
    }

    private SolicitudPublicacionHorario horario(Long id, String diaSemana) {
        SolicitudPublicacionHorario horario = new SolicitudPublicacionHorario();
        horario.setId(id);
        horario.setDiaSemana(diaSemana);
        horario.setHoraInicio(LocalTime.of(18, 0));
        horario.setHoraFin(LocalTime.of(19, 30));
        horario.setObservacion("Salon 1");
        return horario;
    }

    private Usuario usuarioRevisor() {
        Rol rol = new Rol();
        rol.setId(1L);
        rol.setNombre("SUPER_ADMIN");

        Usuario usuario = new Usuario();
        usuario.setId(20L);
        usuario.setNombre("Admin");
        usuario.setApellido("Principal");
        usuario.setEmail("admin@example.com");
        usuario.setPasswordHash("hash-no-debe-salir");
        usuario.setRol(rol);
        return usuario;
    }
}
