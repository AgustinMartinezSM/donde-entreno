package com.dondeentreno.api.asistente;

import com.dondeentreno.api.dto.BarrioDTO;
import com.dondeentreno.api.dto.CategoriaDeportivaDTO;
import com.dondeentreno.api.dto.CiudadDTO;
import com.dondeentreno.api.dto.DeporteDTO;
import com.dondeentreno.api.dto.FiltroOpcionesDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResolutorConsultaTest {

    private final ResolutorConsulta resolutor = new ResolutorConsulta();

    private FiltroOpcionesDTO catalogo() {
        DeporteDTO yoga = new DeporteDTO(
                1L, "Yoga", "yoga", null, null, 1,
                6L, "Bienestar y salud", "bienestar-y-salud"
        );
        DeporteDTO muayThai = new DeporteDTO(
                2L, "Muay Thai", "muay-thai", null, null, 2,
                1L, "Deportes de combate", "deportes-de-combate"
        );
        DeporteDTO karate = new DeporteDTO(
                3L, "Karate", "karate", null, null, 3,
                2L, "Artes marciales", "artes-marciales"
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
                List.of(yoga, muayThai, karate),
                List.of(marDelPlata),
                List.of(new BarrioDTO(7L, "Constitución", 1L, "Mar del Plata")),
                List.of(),
                List.of(),
                List.of()
        );
    }

    @Test
    void resuelveElDeporteNombradoDentroDeUnaFrase() {
        FiltrosResueltos filtros = resolutor.resolver(
                "hola, busco clases de yoga cerca",
                catalogo()
        );

        assertThat(filtros.deporteSlug()).isEqualTo("yoga");
        assertThat(filtros.hayAlgo()).isTrue();
    }

    @Test
    void resuelveUnDeporteDeVariasPalabrasAunqueElSlugTengaGuiones() {
        FiltrosResueltos filtros = resolutor.resolver("quiero hacer muay thai", catalogo());

        assertThat(filtros.deporteSlug()).isEqualTo("muay-thai");
    }

    @Test
    void elDeporteConcretoLeGanaALaCategoria() {
        FiltrosResueltos filtros = resolutor.resolver(
                "artes marciales, karate por ejemplo",
                catalogo()
        );

        assertThat(filtros.deporteSlug()).isEqualTo("karate");
        assertThat(filtros.categoriaSlug()).isNull();
    }

    @Test
    void resuelveCategoriaCuandoNoSeNombraNingunDeporte() {
        FiltrosResueltos filtros = resolutor.resolver("me interesan las artes marciales", catalogo());

        assertThat(filtros.deporteSlug()).isNull();
        assertThat(filtros.categoriaSlug()).isEqualTo("artes-marciales");
    }

    @Test
    void resuelveBarrioNivelYModalidadJuntoConElDeporte() {
        FiltrosResueltos filtros = resolutor.resolver(
                "yoga para principiantes presencial en Constitucion",
                catalogo()
        );

        assertThat(filtros.deporteSlug()).isEqualTo("yoga");
        assertThat(filtros.barrioId()).isEqualTo(7L);
        assertThat(filtros.nivel()).isEqualTo("PRINCIPIANTE");
        assertThat(filtros.modalidad()).isEqualTo("PRESENCIAL");
    }

    @Test
    void noResuelveNadaCuandoElMensajeNoNombraNadaDelCatalogo() {
        FiltrosResueltos filtros = resolutor.resolver(
                "tengo 50 anios y quiero moverme un poco",
                catalogo()
        );

        assertThat(filtros.hayAlgo()).isFalse();
    }

    /*
      El motor del frontend tenía justo este bug: "mi" pegaba dentro de
      "submission" y resolvía Jiu Jitsu. Acá el match es por frase con
      borde de palabra, así que no puede pasar.
    */
    @Test
    void noResuelvePorSubcadenaSueltaDentroDeOtraPalabra() {
        FiltrosResueltos filtros = resolutor.resolver(
                "algo para hacer con mi hijo",
                catalogo()
        );

        assertThat(filtros.hayAlgo()).isFalse();
    }

    @Test
    void toleraTextoVacioYCatalogoNulo() {
        assertThat(resolutor.resolver("", catalogo()).hayAlgo()).isFalse();
        assertThat(resolutor.resolver("yoga", null).hayAlgo()).isFalse();
    }
}
