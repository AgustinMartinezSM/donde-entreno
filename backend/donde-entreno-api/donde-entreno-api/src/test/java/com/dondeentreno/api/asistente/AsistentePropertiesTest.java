package com.dondeentreno.api.asistente;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El motivo por el que el modelo no está disponible se escribe en los logs
 * de producción, así que lo que importa es que diga qué falta sin decir
 * cuánto vale.
 */
class AsistentePropertiesTest {

    /*
      A propósito no imita el formato de una key de Google: una cadena que
      empiece con "AIza" queda marcada por el escaneo de secretos de
      GitHub, y un falso positivo en cada push es peor que el realismo.
    */
    private static final String KEY_SECRETA = "clave-de-prueba-que-no-debe-aparecer-en-un-log";

    private AsistenteProperties propiedades(boolean encendido, String key, String modelo) {
        AsistenteProperties propiedades = new AsistenteProperties();
        propiedades.setGeminiEnabled(encendido);
        propiedades.setGeminiApiKey(key);
        propiedades.setGeminiModel(modelo);
        return propiedades;
    }

    @Test
    void apagadoLoDiceSinMirarLasCredenciales() {
        AsistenteProperties propiedades =
                propiedades(false, KEY_SECRETA, "gemini-flash-latest");

        assertThat(propiedades.geminiDisponible()).isFalse();
        assertThat(propiedades.motivoGeminiNoDisponible())
                .isEqualTo("apagado (gemini-enabled=false)");
    }

    @Test
    void encendidoSinKeyNombraLaVariableQueFalta() {
        AsistenteProperties propiedades = propiedades(true, "", "gemini-flash-latest");

        assertThat(propiedades.motivoGeminiNoDisponible())
                .isEqualTo("encendido pero sin gemini-api-key");
    }

    @Test
    void encendidoSinModeloNombraLaVariableQueFalta() {
        AsistenteProperties propiedades = propiedades(true, KEY_SECRETA, "  ");

        assertThat(propiedades.motivoGeminiNoDisponible())
                .isEqualTo("encendido pero sin gemini-model");
    }

    @Test
    void faltandoLasDosLasNombraALasDos() {
        AsistenteProperties propiedades = propiedades(true, "", "");

        assertThat(propiedades.motivoGeminiNoDisponible())
                .isEqualTo("encendido pero sin gemini-api-key ni gemini-model");
    }

    @Test
    void configuradoCompletoSeConsideraDisponible() {
        AsistenteProperties propiedades =
                propiedades(true, KEY_SECRETA, "gemini-flash-latest");

        assertThat(propiedades.geminiDisponible()).isTrue();
        assertThat(propiedades.motivoGeminiNoDisponible()).isEqualTo("disponible");
    }

    /**
     * La razón de ser de este archivo: el motivo va a un log que se lee
     * desde el panel de Render, así que no puede arrastrar la key ni un
     * pedazo de ella.
     */
    @Test
    void elMotivoJamasIncluyeLaApiKey() {
        for (AsistenteProperties propiedades : new AsistenteProperties[] {
                propiedades(false, KEY_SECRETA, "gemini-flash-latest"),
                propiedades(true, KEY_SECRETA, ""),
                propiedades(true, KEY_SECRETA, "gemini-flash-latest"),
        }) {
            String motivo = propiedades.motivoGeminiNoDisponible();

            assertThat(motivo).doesNotContain(KEY_SECRETA);
            /* Ni entera ni por pedazos. */
            assertThat(motivo).doesNotContain("clave-de-prueba");
        }
    }
}
