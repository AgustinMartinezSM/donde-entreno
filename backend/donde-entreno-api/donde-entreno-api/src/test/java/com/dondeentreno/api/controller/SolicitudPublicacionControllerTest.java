package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.SolicitudPublicacionResponseDTO;
import com.dondeentreno.api.exception.GlobalExceptionHandler;
import com.dondeentreno.api.exception.SolicitudPublicacionInvalidaException;
import com.dondeentreno.api.service.SolicitudPublicacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolicitudPublicacionController.class)
@Import(GlobalExceptionHandler.class)
class SolicitudPublicacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitudPublicacionService solicitudPublicacionService;

    @Test
    void crearSolicitudValidaDevuelveCreated() throws Exception {
        SolicitudPublicacionResponseDTO response = new SolicitudPublicacionResponseDTO(
                10L,
                "DEP-20260619-ABC12345",
                "PENDIENTE",
                OffsetDateTime.parse("2026-06-19T14:30:00-03:00"),
                "La solicitud fue recibida correctamente y quedo pendiente de revision."
        );

        when(solicitudPublicacionService.crearSolicitud(any())).thenReturn(response);

        mockMvc.perform(post("/api/solicitudes-publicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.codigoSeguimiento").value("DEP-20260619-ABC12345"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.mensaje").value(
                        "La solicitud fue recibida correctamente y quedo pendiente de revision."
                ));

        verify(solicitudPublicacionService).crearSolicitud(any());
    }

    @Test
    void crearSolicitudConBeanValidationInvalidaDevuelveBadRequest() throws Exception {
        mockMvc.perform(post("/api/solicitudes-publicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonConErroresDeValidacion()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La solicitud contiene datos invalidos."))
                .andExpect(jsonPath("$.errores.nombreActividad").exists())
                .andExpect(jsonPath("$.errores.horarios").exists());

        verify(solicitudPublicacionService, never()).crearSolicitud(any());
    }

    @Test
    void crearSolicitudConJsonMalFormadoDevuelveBadRequest() throws Exception {
        mockMvc.perform(post("/api/solicitudes-publicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nombreActividad\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "El cuerpo de la solicitud no tiene un formato JSON valido."
                ));

        verify(solicitudPublicacionService, never()).crearSolicitud(any());
    }

    @Test
    void crearSolicitudConReglaDeNegocioInvalidaDevuelveBadRequest() throws Exception {
        when(solicitudPublicacionService.crearSolicitud(any()))
                .thenThrow(new SolicitudPublicacionInvalidaException("Debe informar al menos un WhatsApp o un email."));

        mockMvc.perform(post("/api/solicitudes-publicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Debe informar al menos un WhatsApp o un email."));

        verify(solicitudPublicacionService).crearSolicitud(any());
    }

    private String jsonValido() {
        return """
                {
                  "tipoPublicador": "ESCUELA_DEPORTIVA",
                  "nombrePublicador": "Escuela de Boxeo Norte",
                  "nombreActividad": "Boxeo recreativo",
                  "deporteId": 1,
                  "deporteOtro": null,
                  "descripcion": "Clases para principiantes.",
                  "nivel": "PRINCIPIANTE",
                  "enfoque": "RECREATIVO",
                  "modalidad": "PRESENCIAL",
                  "edadMinima": 18,
                  "edadMaxima": 60,
                  "precioReferencia": 18000.00,
                  "mostrarPrecio": true,
                  "ciudadId": 1,
                  "ciudadOtra": null,
                  "barrioId": 1,
                  "barrioOtro": null,
                  "nombreLugar": "Escuela Norte",
                  "direccion": "Av. Independencia 1234",
                  "referenciaUbicacion": null,
                  "whatsapp": "+54 9 223 512-3456",
                  "instagram": "@escuelanorte",
                  "email": "CONTACTO@EJEMPLO.COM",
                  "observacionesSolicitante": null,
                  "aceptaCondiciones": true,
                  "horarios": [
                    {
                      "diaSemana": "LUNES",
                      "horaInicio": "18:00",
                      "horaFin": "19:30",
                      "observacion": null
                    }
                  ]
                }
                """;
    }

    private String jsonConErroresDeValidacion() {
        return """
                {
                  "tipoPublicador": "ESCUELA_DEPORTIVA",
                  "nombrePublicador": "Escuela de Boxeo Norte",
                  "nombreActividad": "",
                  "deporteId": 1,
                  "deporteOtro": null,
                  "descripcion": "Clases para principiantes.",
                  "nivel": "PRINCIPIANTE",
                  "enfoque": "RECREATIVO",
                  "modalidad": "PRESENCIAL",
                  "edadMinima": 18,
                  "edadMaxima": 60,
                  "precioReferencia": 18000.00,
                  "mostrarPrecio": true,
                  "ciudadId": 1,
                  "ciudadOtra": null,
                  "barrioId": 1,
                  "barrioOtro": null,
                  "nombreLugar": "Escuela Norte",
                  "direccion": "Av. Independencia 1234",
                  "referenciaUbicacion": null,
                  "whatsapp": "+54 9 223 512-3456",
                  "instagram": "@escuelanorte",
                  "email": "CONTACTO@EJEMPLO.COM",
                  "observacionesSolicitante": null,
                  "aceptaCondiciones": true,
                  "horarios": []
                }
                """;
    }
}
