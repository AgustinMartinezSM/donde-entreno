package com.dondeentreno.api.service;

import com.dondeentreno.api.asistente.AsistenteProperties;
import com.dondeentreno.api.asistente.InterpretacionRemota;
import com.dondeentreno.api.asistente.LimitadorConsultas;
import com.dondeentreno.api.asistente.MotorAsistenteRemoto;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsistenteServiceTest {

    private FiltroService filtroService;
    private ActividadService actividadService;
    private MotorAsistenteRemoto motorRemoto;
    private LimitadorConsultas limitador;
    private AsistenteService asistenteService;

    @BeforeEach
    void prepararService() {
        filtroService = mock(FiltroService.class);
        actividadService = mock(ActividadService.class);
        motorRemoto = mock(MotorAsistenteRemoto.class);
        limitador = mock(LimitadorConsultas.class);

        asistenteService = new AsistenteService(
                filtroService,
                actividadService,
                new ResolutorConsulta(),
                new AsistenteProperties(),
                motorRemoto,
                limitador
        );

        when(filtroService.obtenerOpcionesDeFiltros()).thenReturn(catalogo());
        /* Por defecto, como en produccion hasta encenderlo: Gemini apagado. */
        when(motorRemoto.estaDisponible()).thenReturn(false);
    }

    private void conModeloDisponible(InterpretacionRemota interpretacion) {
        when(motorRemoto.estaDisponible()).thenReturn(true);
        when(limitador.consumirCuotaGemini()).thenReturn(true);
        when(motorRemoto.interpretar(any(), any())).thenReturn(Optional.ofNullable(interpretacion));
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
    void conElModeloApagadoNiSiquieraSeLeConsulta() {
        asistenteService.responder("tengo 50 anios y quiero moverme");

        verify(motorRemoto, never()).interpretar(any(), any());
    }

    @Test
    void usaAlModeloSoloCuandoElMotorLocalNoEntendio() {
        conModeloDisponible(new InterpretacionRemota("Yoga", null, null, null, null));
        when(actividadService.buscarActividadesConFiltros(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(List.of(actividad("Yoga inicial")));

        /* Esta la entiende el motor local: el modelo no deberia tocarse. */
        AsistenteRespuestaDTO local = asistenteService.responder("busco yoga");
        assertThat(local.getFuente()).isEqualTo("local");
        verify(motorRemoto, never()).interpretar(any(), any());

        /* Esta no: aca si entra, y la respuesta la sigue armando el backend. */
        AsistenteRespuestaDTO remota = asistenteService.responder("quiero relajarme un poco");
        assertThat(remota.getFuente()).isEqualTo("gemini");
        assertThat(remota.getTexto()).contains("Yoga inicial");
        assertThat(hrefs(remota)).containsExactly("/explorar?deporteSlug=yoga&page=0");
    }

    /*
      El candado del bloque: lo que el modelo invente no existe en el
      catalogo, no matchea y se descarta solo.
    */
    @Test
    void descartaLosTerminosQueElModeloInventa() {
        conModeloDisponible(new InterpretacionRemota("Quidditch", "Deportes magicos", "Hogwarts", null, null));

        AsistenteRespuestaDTO respuesta = asistenteService.responder("quiero jugar al quidditch");

        assertThat(respuesta.getFuente()).isEqualTo("local");
        assertThat(respuesta.getTexto()).contains("no la tengo del todo clara");
        assertThat(hrefs(respuesta)).containsExactly("/explorar", "/deportes");
    }

    @Test
    void sinCuotaDiariaNoLlamaAlModelo() {
        when(motorRemoto.estaDisponible()).thenReturn(true);
        when(limitador.consumirCuotaGemini()).thenReturn(false);

        AsistenteRespuestaDTO respuesta = asistenteService.responder("quiero relajarme un poco");

        verify(motorRemoto, never()).interpretar(any(), any());
        assertThat(respuesta.getFuente()).isEqualTo("local");
    }

    @Test
    void siElModeloFallaElAsistenteResponderIgual() {
        conModeloDisponible(null);

        AsistenteRespuestaDTO respuesta = asistenteService.responder("quiero relajarme un poco");

        assertThat(respuesta.getFuente()).isEqualTo("local");
        assertThat(respuesta.getTexto()).contains("no la tengo del todo clara");
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
                propiedades,
                motorRemoto,
                limitador
        );

        assertThatThrownBy(() -> acotado.responder("un mensaje bastante mas largo que diez"))
                .isInstanceOf(ConsultaAsistenteInvalidaException.class)
                .hasMessageContaining("10");
    }
}
