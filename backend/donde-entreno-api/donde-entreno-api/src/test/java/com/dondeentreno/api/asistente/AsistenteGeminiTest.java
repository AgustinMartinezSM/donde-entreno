package com.dondeentreno.api.asistente;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del parseo de la respuesta de la Interactions API.
 *
 * Es la parte más frágil del cliente: el texto no viene en la raíz sino
 * dentro del paso "model_output" de un array de steps que además puede
 * traer pasos de "thought" adelante.
 */
class AsistenteGeminiTest {

    private final AsistenteGemini gemini = new AsistenteGemini(
            new AsistenteProperties(),
            RestClient.builder(),
            new ObjectMapper()
    );

    @Test
    void extraeLosTerminosDelPasoModelOutput() throws Exception {
        String respuesta = """
                {
                  "id": "v1_abc",
                  "status": "completed",
                  "steps": [
                    {
                      "type": "model_output",
                      "content": [
                        {"type": "text", "text": "{\\"deporte\\":\\"Yoga\\",\\"barrio\\":\\"Centro\\"}"}
                      ]
                    }
                  ]
                }
                """;

        Optional<InterpretacionRemota> interpretacion = gemini.extraerInterpretacion(respuesta);

        assertThat(interpretacion).isPresent();
        assertThat(interpretacion.get().deporte()).isEqualTo("Yoga");
        assertThat(interpretacion.get().barrio()).isEqualTo("Centro");
        assertThat(interpretacion.get().comoFrase()).isEqualTo("Yoga Centro");
    }

    /* Con thinking activado, el primer paso no es la respuesta. */
    @Test
    void salteaLosPasosDePensamientoAntesDelModelOutput() throws Exception {
        String respuesta = """
                {
                  "steps": [
                    {"type": "thought", "signature": "abc"},
                    {
                      "type": "model_output",
                      "content": [
                        {"type": "text", "text": "{\\"deporte\\":\\"Pilates\\"}"}
                      ]
                    }
                  ]
                }
                """;

        Optional<InterpretacionRemota> interpretacion = gemini.extraerInterpretacion(respuesta);

        assertThat(interpretacion).isPresent();
        assertThat(interpretacion.get().deporte()).isEqualTo("Pilates");
    }

    @Test
    void toleraQueElJsonVengaEnvueltoEnCercosDeCodigo() throws Exception {
        String respuesta = """
                {
                  "steps": [
                    {
                      "type": "model_output",
                      "content": [
                        {"type": "text", "text": "```json\\n{\\"deporte\\":\\"Boxeo\\"}\\n```"}
                      ]
                    }
                  ]
                }
                """;

        Optional<InterpretacionRemota> interpretacion = gemini.extraerInterpretacion(respuesta);

        assertThat(interpretacion).isPresent();
        assertThat(interpretacion.get().deporte()).isEqualTo("Boxeo");
    }

    @Test
    void ignoraCamposDesconocidosQueAgregueLaApi() throws Exception {
        String respuesta = """
                {
                  "steps": [
                    {
                      "type": "model_output",
                      "content": [
                        {"type": "text", "text": "{\\"deporte\\":\\"Tenis\\",\\"confianza\\":0.9}"}
                      ]
                    }
                  ]
                }
                """;

        assertThat(gemini.extraerInterpretacion(respuesta)).isPresent();
    }

    @Test
    void devuelveVacioCuandoNoHayPasoDeSalida() throws Exception {
        assertThat(gemini.extraerInterpretacion("{\"steps\":[]}")).isEmpty();
        assertThat(gemini.extraerInterpretacion("")).isEmpty();
        assertThat(gemini.extraerInterpretacion(null)).isEmpty();
    }

    /*
      La forma de response_format ya nos rompió una vez: lo mandamos como
      objeto con type "json_schema" cuando la API espera una LISTA de
      formatos con mime_type y schema. Daba 400 y el asistente caía al
      motor local sin que se notara desde afuera. Este test lo fija.
    */
    @Test
    void mandaResponseFormatComoListaConMimeTypeYEsquema() {
        Map<String, Object> cuerpo = gemini.armarCuerpo("busco yoga", "Deportes: Yoga", true);

        assertThat(cuerpo.get("response_format")).isInstanceOf(List.class);

        List<?> formatos = (List<?>) cuerpo.get("response_format");
        assertThat(formatos).hasSize(1);

        Map<?, ?> formato = (Map<?, ?>) formatos.get(0);
        assertThat(formato.get("type")).isEqualTo("text");
        assertThat(formato.get("mime_type")).isEqualTo("application/json");
        assertThat(formato.get("schema")).isNotNull();
    }

    @Test
    void elReintentoVaSinEsquemaPeroConLaMismaInstruccion() {
        Map<String, Object> conEsquema = gemini.armarCuerpo("busco yoga", "Deportes: Yoga", true);
        Map<String, Object> sinEsquema = gemini.armarCuerpo("busco yoga", "Deportes: Yoga", false);

        assertThat(sinEsquema).doesNotContainKey("response_format");
        assertThat(sinEsquema.get("system_instruction"))
                .isEqualTo(conEsquema.get("system_instruction"));
        assertThat(sinEsquema.get("input")).isEqualTo("busco yoga");
    }

    @Test
    void elCatalogoViajaEnLaInstruccionDeSistema() {
        Map<String, Object> cuerpo = gemini.armarCuerpo("hola", "Deportes: Yoga, Boxeo", true);

        assertThat((String) cuerpo.get("system_instruction")).contains("Deportes: Yoga, Boxeo");
    }

    @Test
    void sinCredencialesNoEstaDisponibleYNoIntentaLlamar() {
        assertThat(gemini.estaDisponible()).isFalse();
        assertThat(gemini.interpretar("busco yoga", "Deportes: Yoga")).isEmpty();
    }

    @Test
    void encendidoPeroSinKeyNiModeloSigueSinEstarDisponible() {
        AsistenteProperties propiedades = new AsistenteProperties();
        propiedades.setGeminiEnabled(true);

        AsistenteGemini soloEncendido = new AsistenteGemini(
                propiedades,
                RestClient.builder(),
                new ObjectMapper()
        );

        assertThat(soloEncendido.estaDisponible()).isFalse();
    }
}
