package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.SolicitudPublicacionHorarioRequestDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionRequestDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionResponseDTO;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.SolicitudPublicacion;
import com.dondeentreno.api.entity.SolicitudPublicacionHorario;
import com.dondeentreno.api.exception.SolicitudPublicacionInvalidaException;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionHorarioRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudPublicacionServiceTest {

    @Mock
    private SolicitudPublicacionRepository solicitudPublicacionRepository;

    @Mock
    private SolicitudPublicacionHorarioRepository solicitudPublicacionHorarioRepository;

    @Mock
    private DeporteRepository deporteRepository;

    @Mock
    private CiudadRepository ciudadRepository;

    @Mock
    private BarrioRepository barrioRepository;

    private SolicitudPublicacionService service;

    @BeforeEach
    void setUp() {
        service = new SolicitudPublicacionService(
                solicitudPublicacionRepository,
                solicitudPublicacionHorarioRepository,
                deporteRepository,
                ciudadRepository,
                barrioRepository
        );
    }

    @Test
    void crearSolicitudConRequestValidoGuardaSolicitudYHorarios() {
        SolicitudPublicacionRequestDTO request = requestValido();
        Ciudad ciudad = ciudadActiva(1L);
        Barrio barrio = barrioActivo(1L, ciudad);

        when(deporteRepository.findById(1L)).thenReturn(Optional.of(deporteActivo(1L)));
        when(ciudadRepository.findById(1L)).thenReturn(Optional.of(ciudad));
        when(barrioRepository.findById(1L)).thenReturn(Optional.of(barrio));
        when(solicitudPublicacionRepository.existsByCodigoSeguimiento(anyString())).thenReturn(false);
        when(solicitudPublicacionRepository.save(any(SolicitudPublicacion.class))).thenAnswer(invocation -> {
            SolicitudPublicacion solicitud = invocation.getArgument(0);
            solicitud.setId(10L);
            return solicitud;
        });
        when(solicitudPublicacionHorarioRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SolicitudPublicacionResponseDTO response = service.crearSolicitud(request);

        assertEquals(10L, response.getId());
        assertNotNull(response.getCodigoSeguimiento());
        assertTrue(response.getCodigoSeguimiento().matches("DEP-\\d{8}-[A-Z0-9]{8}"));
        assertTrue(response.getCodigoSeguimiento().length() <= 40);
        assertEquals("PENDIENTE", response.getEstado());
        assertNotNull(response.getCreatedAt());

        ArgumentCaptor<SolicitudPublicacion> solicitudCaptor =
                ArgumentCaptor.forClass(SolicitudPublicacion.class);
        verify(solicitudPublicacionRepository).save(solicitudCaptor.capture());

        SolicitudPublicacion solicitudGuardada = solicitudCaptor.getValue();
        assertEquals("FORMULARIO_WEB", solicitudGuardada.getOrigen());
        assertEquals("PENDIENTE", solicitudGuardada.getEstado());
        assertFalse(solicitudGuardada.getMostrarPrecio());
        assertEquals("5492235123456", solicitudGuardada.getWhatsappNormalizado());
        assertEquals("contacto@ejemplo.com", solicitudGuardada.getEmail());

        ArgumentCaptor<Iterable<SolicitudPublicacionHorario>> horariosCaptor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(solicitudPublicacionHorarioRepository).saveAll(horariosCaptor.capture());

        List<SolicitudPublicacionHorario> horarios = toList(horariosCaptor.getValue());
        assertEquals(1, horarios.size());
        assertEquals(solicitudGuardada, horarios.get(0).getSolicitudPublicacion());
        assertEquals("LUNES", horarios.get(0).getDiaSemana());
        assertEquals(LocalTime.of(18, 0), horarios.get(0).getHoraInicio());
        assertEquals(LocalTime.of(19, 30), horarios.get(0).getHoraFin());
    }

    @Test
    void crearSolicitudSinHorariosLanzaExcepcionYNoGuarda() {
        SolicitudPublicacionRequestDTO request = requestValido();
        request.setHorarios(List.of());
        configurarReferenciasValidas();

        assertThrows(SolicitudPublicacionInvalidaException.class, () -> service.crearSolicitud(request));

        verify(solicitudPublicacionRepository, never()).save(any());
        verify(solicitudPublicacionHorarioRepository, never()).saveAll(any());
    }

    @Test
    void crearSolicitudConHorarioInvalidoLanzaExcepcionYNoGuarda() {
        SolicitudPublicacionRequestDTO request = requestValido();
        request.setHorarios(List.of(new SolicitudPublicacionHorarioRequestDTO(
                "LUNES",
                LocalTime.of(20, 0),
                LocalTime.of(20, 0),
                null
        )));
        configurarReferenciasValidas();

        assertThrows(SolicitudPublicacionInvalidaException.class, () -> service.crearSolicitud(request));

        verify(solicitudPublicacionRepository, never()).save(any());
        verify(solicitudPublicacionHorarioRepository, never()).saveAll(any());
    }

    @Test
    void crearSolicitudConHorariosDuplicadosLanzaExcepcionYNoGuarda() {
        SolicitudPublicacionRequestDTO request = requestValido();
        request.setHorarios(List.of(
                new SolicitudPublicacionHorarioRequestDTO("LUNES", LocalTime.of(18, 0), LocalTime.of(19, 30), null),
                new SolicitudPublicacionHorarioRequestDTO("LUNES", LocalTime.of(18, 0), LocalTime.of(19, 30), null)
        ));
        configurarReferenciasValidas();

        assertThrows(SolicitudPublicacionInvalidaException.class, () -> service.crearSolicitud(request));

        verify(solicitudPublicacionRepository, never()).save(any());
        verify(solicitudPublicacionHorarioRepository, never()).saveAll(any());
    }

    @Test
    void crearSolicitudConDeporteInexistenteLanzaExcepcionYNoGuarda() {
        SolicitudPublicacionRequestDTO request = requestValido();

        when(deporteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(SolicitudPublicacionInvalidaException.class, () -> service.crearSolicitud(request));

        verify(solicitudPublicacionRepository, never()).save(any());
        verify(solicitudPublicacionHorarioRepository, never()).saveAll(any());
    }

    @Test
    void crearSolicitudConBarrioDeOtraCiudadLanzaExcepcionYNoGuarda() {
        SolicitudPublicacionRequestDTO request = requestValido();
        Ciudad ciudad = ciudadActiva(1L);
        Ciudad otraCiudad = ciudadActiva(2L);
        Barrio barrio = barrioActivo(1L, otraCiudad);

        when(deporteRepository.findById(1L)).thenReturn(Optional.of(deporteActivo(1L)));
        when(ciudadRepository.findById(1L)).thenReturn(Optional.of(ciudad));
        when(barrioRepository.findById(1L)).thenReturn(Optional.of(barrio));

        assertThrows(SolicitudPublicacionInvalidaException.class, () -> service.crearSolicitud(request));

        verify(solicitudPublicacionRepository, never()).save(any());
        verify(solicitudPublicacionHorarioRepository, never()).saveAll(any());
    }

    @Test
    void crearSolicitudSiempreGuardaEstadoPendiente() {
        SolicitudPublicacionRequestDTO request = requestValido();
        configurarReferenciasValidas();
        when(solicitudPublicacionRepository.existsByCodigoSeguimiento(anyString())).thenReturn(false);
        when(solicitudPublicacionRepository.save(any(SolicitudPublicacion.class))).thenAnswer(invocation -> {
            SolicitudPublicacion solicitud = invocation.getArgument(0);
            solicitud.setId(11L);
            return solicitud;
        });
        when(solicitudPublicacionHorarioRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.crearSolicitud(request);

        ArgumentCaptor<SolicitudPublicacion> solicitudCaptor =
                ArgumentCaptor.forClass(SolicitudPublicacion.class);
        verify(solicitudPublicacionRepository).save(solicitudCaptor.capture());
        assertEquals("PENDIENTE", solicitudCaptor.getValue().getEstado());
    }

    @Test
    void crearSolicitudPropagaErrorAlGuardarHorarios() {
        SolicitudPublicacionRequestDTO request = requestValido();
        configurarReferenciasValidas();
        when(solicitudPublicacionRepository.existsByCodigoSeguimiento(anyString())).thenReturn(false);
        when(solicitudPublicacionRepository.save(any(SolicitudPublicacion.class))).thenAnswer(invocation -> {
            SolicitudPublicacion solicitud = invocation.getArgument(0);
            solicitud.setId(12L);
            return solicitud;
        });
        when(solicitudPublicacionHorarioRepository.saveAll(any()))
                .thenThrow(new RuntimeException("fallo simulado"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.crearSolicitud(request));

        assertEquals("fallo simulado", exception.getMessage());
        verify(solicitudPublicacionRepository).save(any(SolicitudPublicacion.class));
        verify(solicitudPublicacionHorarioRepository).saveAll(any());
    }

    private void configurarReferenciasValidas() {
        Ciudad ciudad = ciudadActiva(1L);
        Barrio barrio = barrioActivo(1L, ciudad);

        when(deporteRepository.findById(1L)).thenReturn(Optional.of(deporteActivo(1L)));
        when(ciudadRepository.findById(1L)).thenReturn(Optional.of(ciudad));
        when(barrioRepository.findById(1L)).thenReturn(Optional.of(barrio));
    }

    private SolicitudPublicacionRequestDTO requestValido() {
        return new SolicitudPublicacionRequestDTO(
                "ESCUELA_DEPORTIVA",
                "Escuela de Boxeo Norte",
                "Boxeo recreativo",
                1L,
                null,
                "Clases para principiantes.",
                "PRINCIPIANTE",
                "RECREATIVO",
                "PRESENCIAL",
                18,
                60,
                new BigDecimal("18000.00"),
                null,
                1L,
                null,
                1L,
                null,
                "Escuela Norte",
                "Av. Independencia 1234",
                null,
                "+54 9 223 512-3456",
                "@escuelanorte",
                "CONTACTO@EJEMPLO.COM",
                null,
                true,
                List.of(new SolicitudPublicacionHorarioRequestDTO(
                        "LUNES",
                        LocalTime.of(18, 0),
                        LocalTime.of(19, 30),
                        null
                ))
        );
    }

    private Deporte deporteActivo(Long id) {
        Deporte deporte = new Deporte();
        deporte.setId(id);
        deporte.setActivo(true);
        return deporte;
    }

    private Ciudad ciudadActiva(Long id) {
        Ciudad ciudad = new Ciudad();
        ciudad.setId(id);
        ciudad.setActiva(true);
        return ciudad;
    }

    private Barrio barrioActivo(Long id, Ciudad ciudad) {
        Barrio barrio = new Barrio();
        barrio.setId(id);
        barrio.setActivo(true);
        barrio.setCiudad(ciudad);
        return barrio;
    }

    private List<SolicitudPublicacionHorario> toList(Iterable<SolicitudPublicacionHorario> horarios) {
        assertNotNull(horarios);
        assertInstanceOf(List.class, horarios);
        return (List<SolicitudPublicacionHorario>) horarios;
    }
}
