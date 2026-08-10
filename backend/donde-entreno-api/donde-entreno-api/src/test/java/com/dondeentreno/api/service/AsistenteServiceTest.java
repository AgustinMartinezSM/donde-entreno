package com.dondeentreno.api.service;

import com.dondeentreno.api.asistente.AsistenteProperties;
import com.dondeentreno.api.asistente.ResolutorConsulta;
import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.dto.AsistenteEnlaceDTO;
import com.dondeentreno.api.dto.AsistenteRespuestaDTO;
import com.dondeentreno.api.dto.BarrioDTO;
import com.dondeentreno.api.dto.CategoriaDeportivaDTO;
import com.dondeentreno.api.dto.CiudadDTO;
import com.dondeentreno.api.dto.DeporteDTO;
import com.dondeentreno.api.dto.FiltroOpcionesDTO;
import com.dondeentreno.api.exception.ConsultaAsistenteInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AsistenteServiceTest {

    private FiltroService filtroService;
    private ActividadService actividadService;
    private AsistenteService asistenteService;

    @BeforeEach
    void prepararService() {
        filtroService = mock(FiltroService.class);
        actividadService = mock(ActividadService.class);

        asistenteService = new AsistenteService(
                filtroService,
                actividadService,
                new ResolutorConsulta(),
                new AsistenteProperties()
        );

        when(filtroService.obtenerOpcionesDeFiltros()).thenReturn(catalogo());
    }

    private FiltroOpcionesDTO catalogo() {
        DeporteDTO yoga = new DeporteDTO(
                1L, "Yoga", "yoga", null, null, 1,
                6L, "Bienestar y salud", "bienestar-y-salud"
        );

        CategoriaDeportivaDTO artesMarciales = new CategoriaDeportivaDTO();
        artesMarciales.setId(2L);
        artesMarciales.setNombre("Artes marciales");
        artesMarciales.setSlug("artes-marciales");

        CiudadDTO marDelPlata = new CiudadDTO();
        marDelPlata.setId(1L);
        marDelPlata.setNombre("Mar del Plata");
        marDelPlata.setSlug("mar-del-plata");

        return new FiltroOpcionesDTO(
                List.of(artesMarciales),
                List.of(yoga),
                List.of(marDelPlata),
                List.of(new BarrioDTO(7L, "Constitución", 1L, "Mar del Plata")),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private ActividadDTO actividad(String titulo) {
        ActividadDTO actividad = new ActividadDTO();
        actividad.setTitulo(titulo);
        return actividad;
    }

    private List<String> hrefs(AsistenteRespuestaDTO respuesta) {
        return respuesta.getEnlaces().stream().map(AsistenteEnlaceDTO::getHref).toList();
    }

    @Test
    void informaElTotalRealYEnlazaConLosFiltrosEntendidos() {
        when(actividadService.buscarActividadesConFiltros(
                isNull(), eq("yoga"), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of(actividad("Yoga inicial"), actividad("Yoga en el parque")));

        AsistenteRespuestaDTO respuesta = asistenteService.responder("busco yoga");

        assertThat(respuesta.getTexto()).contains("2 actividades");
        assertThat(respuesta.getTexto()).contains("Yoga inicial");
        assertThat(hrefs(respuesta)).containsExactly("/explorar?deporteSlug=yoga&page=0");
        assertThat(respuesta.getFuente()).isEqualTo("local");
    }

    @Test
    void cuandoNoHayNadaEnElBarrioAmpliaLaZonaYLoDice() {
        when(actividadService.buscarActividadesConFiltros(
                isNull(), eq("yoga"), isNull(), isNull(), eq(7L),
                isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of());

        when(actividadService.buscarActividadesConFiltros(
                isNull(), eq("yoga"), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of(actividad("Yoga inicial")));

        AsistenteRespuestaDTO respuesta = asistenteService.responder("yoga en Constitucion");

        assertThat(respuesta.getTexto()).contains("Constitución");
        assertThat(respuesta.getTexto()).contains("no encontré");
        assertThat(respuesta.getTexto()).contains("1 actividad");
        /* El enlace ampliado ya no lleva el barrio que no tenía nada. */
        assertThat(hrefs(respuesta)).containsExactly("/explorar?deporteSlug=yoga&page=0");
    }

    @Test
    void siNoHayResultadosEnNingunLadoLoDiceSinInventar() {
        when(actividadService.buscarActividadesConFiltros(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(List.of());

        AsistenteRespuestaDTO respuesta = asistenteService.responder("busco yoga");

        assertThat(respuesta.getTexto()).contains("no hay actividades");
        assertThat(hrefs(respuesta)).containsExactly("/deportes", "/explorar");
    }

    @Test
    void unaCategoriaSolaLlevaAlCatalogoDeEsaCategoriaSinInventarConteo() {
        AsistenteRespuestaDTO respuesta = asistenteService.responder("me interesan las artes marciales");

        assertThat(hrefs(respuesta)).containsExactly("/deportes?categoria=artes-marciales");
        assertThat(respuesta.getTexto()).doesNotContain("actividades de");
    }

    @Test
    void admiteQueNoEntendioEnVezDeInventarUnDeporte() {
        AsistenteRespuestaDTO respuesta = asistenteService.responder("tengo 50 anios y quiero moverme");

        assertThat(respuesta.getTexto()).contains("no la tengo del todo clara");
        assertThat(hrefs(respuesta)).containsExactly("/explorar", "/deportes");
    }

    @Test
    void rechazaConsultaVacia() {
        assertThatThrownBy(() -> asistenteService.responder("   "))
                .isInstanceOf(ConsultaAsistenteInvalidaException.class);
    }

    @Test
    void rechazaConsultaMasLargaQueElMaximoConfigurado() {
        AsistenteProperties propiedades = new AsistenteProperties();
        propiedades.setMaxInputChars(10);

        AsistenteService acotado = new AsistenteService(
                filtroService,
                actividadService,
                new ResolutorConsulta(),
                propiedades
        );

        assertThatThrownBy(() -> acotado.responder("un mensaje bastante mas largo que diez"))
                .isInstanceOf(ConsultaAsistenteInvalidaException.class)
                .hasMessageContaining("10");
    }
}
