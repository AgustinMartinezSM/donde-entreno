package com.dondeentreno.api.asistente;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del filtro que se le aplica al texto del modelo.
 *
 * Desde el asistente V2 el modelo escribe lo que ve el usuario, y esta
 * clase es la primera de las dos defensas que reemplazan a la vieja
 * (donde el modelo directamente no podía escribir). La segunda es que
 * ninguna afirmación sobre qué hay publicado la escribe él.
 */
class SanitizadorTextoTest {

    @Test
    void borraLasUrlsPeroDejaElTexto() {
        assertThat(SanitizadorTexto.limpiarMensaje("Mirá en https://otro-sitio.com/clases y contame"))
                .doesNotContain("http")
                .doesNotContain("otro-sitio")
                .contains("Mirá en")
                .contains("y contame");

        assertThat(SanitizadorTexto.limpiarMensaje("Entrá a www.ejemplo.com.ar ya"))
                .doesNotContain("ejemplo");
    }

    @Test
    void desarmaLosEnlacesDeMarkdownDejandoSoloLaPalabra() {
        assertThat(SanitizadorTexto.limpiarMensaje("Probá [este club](https://club.com) hoy"))
                .contains("este club")
                .doesNotContain("http")
                .doesNotContain("[");
    }

    @Test
    void borraMailsYTelefonos() {
        assertThat(SanitizadorTexto.limpiarMensaje("Escribile a hola@club.com así arreglan"))
                .doesNotContain("@");

        assertThat(SanitizadorTexto.limpiarMensaje("Llamá al 223 512 3456 y listo"))
                .doesNotContain("3456");
    }

    /*
      Un precio o un horario inventado manda a una persona a un lugar con
      una expectativa falsa: se descarta la oracion entera.
    */
    @Test
    void descartaLaOracionQueInventaUnPrecio() {
        String limpio = SanitizadorTexto.limpiarMensaje(
                "El pádel es social y divertido. Sale 15000 pesos por mes. Te va a gustar."
        );

        assertThat(limpio)
                .contains("social y divertido")
                .contains("Te va a gustar")
                .doesNotContain("15000");
    }

    @Test
    void descartaLaOracionQueInventaUnHorario() {
        String limpio = SanitizadorTexto.limpiarMensaje(
                "El funcional es variado. Las clases son a las 18:30. Probalo."
        );

        assertThat(limpio)
                .contains("variado")
                .contains("Probalo")
                .doesNotContain("18:30");
    }

    /* "2 horas por semana" es un consejo valido y no se toca. */
    @Test
    void noConfundeUnaDuracionConUnHorarioDeClase() {
        assertThat(SanitizadorTexto.limpiarMensaje("Con 2 horas por semana ya vas a notar la diferencia."))
                .contains("2 horas por semana");
    }

    @Test
    void saleLasMarcasDeMarkdownPorqueLaUiMuestraTextoPlano() {
        assertThat(SanitizadorTexto.limpiarMensaje("**Pádel** es _social_"))
                .isEqualTo("Pádel es social");
    }

    @Test
    void recortaEnElFinalDeOracionCuandoSePasaDeLargo() {
        String largo = "Una oración corta. ".repeat(120);
        String limpio = SanitizadorTexto.limpiarMensaje(largo);

        assertThat(limpio.length()).isLessThanOrEqualTo(SanitizadorTexto.MAX_MENSAJE);
        assertThat(limpio).endsWith(".");
    }

    @Test
    void unFragmentoSeRecortaMasCorto() {
        String limpio = SanitizadorTexto.limpiarFragmento("palabra ".repeat(80));

        assertThat(limpio.length()).isLessThanOrEqualTo(SanitizadorTexto.MAX_FRAGMENTO);
    }

    @Test
    void toleraNullYVacio() {
        assertThat(SanitizadorTexto.limpiarMensaje(null)).isEmpty();
        assertThat(SanitizadorTexto.limpiarMensaje("   ")).isEmpty();
        assertThat(SanitizadorTexto.limpiarFragmento(null)).isEmpty();
    }

    /*
      El historial va DENTRO del prompt: ahi no se borran precios ni URLs,
      porque es texto que la persona escribio y el modelo tiene que poder
      leerlo para entender la charla. Solo se acota el largo.
    */
    @Test
    void loQueVaAlPromptSoloSeAcotaDeLargo() {
        String original = "Me dijeron que en https://algo.com sale 5000 pesos";

        assertThat(SanitizadorTexto.limpiarParaPrompt(original, 400)).isEqualTo(original);
        assertThat(SanitizadorTexto.limpiarParaPrompt("a".repeat(500), 100)).hasSize(100);
        assertThat(SanitizadorTexto.limpiarParaPrompt(null, 100)).isEmpty();
    }
}
