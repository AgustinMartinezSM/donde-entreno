package com.dondeentreno.api.asistente;

import com.dondeentreno.api.dto.AsistenteMensajeDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del cliente de la Interactions API.
 *
 * Dos partes frágiles: el parseo (el texto no viene en la raíz sino dentro
 * del paso "model_output", que además puede venir después de pasos de
 * "thought") y la forma del cuerpo, que ya nos costó un 400 en cada
 * llamada.
 */
class AsistenteGeminiTest {

    private final AsistenteGemini gemini = new AsistenteGemini(
            new AsistenteProperties(),
            RestClient.builder(),
            new ObjectMapper()
    );

    private ConsultaRemota consulta() {
        return new ConsultaRemota(
                "quiero algo social",
                List.of(),
                "Deportes: Yoga",
                "Yoga, Funcional, Pádel",
                Set.of(),
                Set.of()
        );
    }

    @Test
    void extraeLaRespuestaDelPasoModelOutput() throws Exception {
        String respuesta = """
                {
                  "id": "v1_abc",
                  "status": "completed",
                  "steps": [
                    {
                      "type": "model_output",
                      "content": [
                        {"type": "text", "text": "{\\"mensaje\\":\\"Va esto\\",\\"deportes\\":[{\\"nombre\\":\\"Pádel\\",\\"motivo\\":\\"social\\"}]}"}
                      ]
                    }
                  ]
                }
                """;

        Optional<RespuestaModelo> interpretada = gemini.extraerRespuesta(respuesta);

        assertThat(interpretada).isPresent();
        assertThat(interpretada.get().mensaje()).isEqualTo("Va esto");
        assertThat(interpretada.get().deportesODefecto()).hasSize(1);
        assertThat(interpretada.get().deportesODefecto().get(0).nombre()).isEqualTo("Pádel");
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
                        {"type": "text", "text": "{\\"mensaje\\":\\"Pilates va bien\\"}"}
                      ]
                    }
                  ]
                }
                """;

        assertThat(gemini.extraerRespuesta(respuesta))
                .isPresent()
                .get()
                .extracting(RespuestaModelo::mensaje)
                .isEqualTo("Pilates va bien");
    }

    @Test
    void toleraQueElJsonVengaEnvueltoEnCercosDeCodigo() throws Exception {
        String respuesta = """
                {
                  "steps": [
                    {
                      "type": "model_output",
                      "content": [
                        {"type": "text", "text": "```json\\n{\\"mensaje\\":\\"Dale\\"}\\n```"}
                      ]
                    }
                  ]
                }
                """;

        assertThat(gemini.extraerRespuesta(respuesta)).isPresent();
    }

    @Test
    void ignoraCamposDesconocidosQueAgregueLaApi() throws Exception {
        String respuesta = """
                {
                  "steps": [
                    {
                      "type": "model_output",
                      "content": [
                        {"type": "text", "text": "{\\"mensaje\\":\\"Ok\\",\\"confianza\\":0.9}"}
                      ]
                    }
                  ]
                }
                """;

        assertThat(gemini.extraerRespuesta(respuesta)).isPresent();
    }

    @Test
    void devuelveVacioCuandoNoHayNadaAprovechable() throws Exception {
        assertThat(gemini.extraerRespuesta("{\"steps\":[]}")).isEmpty();
        assertThat(gemini.extraerRespuesta("")).isEmpty();
        assertThat(gemini.extraerRespuesta(null)).isEmpty();
    }

    /* Un JSON valido pero vacio equivale a no haber llamado. */
    @Test
    void unaRespuestaSinContenidoSeTrataComoFalla() throws Exception {
        String respuesta = """
                {
                  "steps": [
                    {
                      "type": "model_output",
                      "content": [{"type": "text", "text": "{\\"mensaje\\":\\"\\"}"}]
                    }
                  ]
                }
                """;

        assertThat(gemini.extraerRespuesta(respuesta)).isEmpty();
    }

    /*
      La forma de response_format ya nos rompio una vez: lo mandamos como
      objeto con type "json_schema" cuando la API espera una LISTA de
      formatos con mime_type y schema. Daba 400 y el asistente caia al
      motor local sin que se notara desde afuera. Este test lo fija.
    */
    @Test
    void mandaResponseFormatComoListaConMimeTypeYEsquema() {
        Map<String, Object> cuerpo = gemini.armarCuerpo(consulta(), true);

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
        Map<String, Object> conEsquema = gemini.armarCuerpo(consulta(), true);
        Map<String, Object> sinEsquema = gemini.armarCuerpo(consulta(), false);

        assertThat(sinEsquema).doesNotContainKey("response_format");
        assertThat(sinEsquema.get("system_instruction"))
                .isEqualTo(conEsquema.get("system_instruction"));
        assertThat(sinEsquema.get("input")).isEqualTo(conEsquema.get("input"));
    }

    /*
      Las prohibiciones tienen su equivalente en codigo, pero tambien
      tienen que estar dichas: si alguien reescribe la instruccion y las
      saca, el modelo empieza a intentar cosas que despues hay que filtrar.
    */
    @Test
    void laInstruccionDeSistemaProhibeInventarYAfirmarDisponibilidad() {
        String instruccion = (String) gemini.armarCuerpo(consulta(), true).get("system_instruction");

        assertThat(instruccion)
                .contains("PROHIBIDO")
                .contains("No escribas ningun enlace")
                .contains("profesional de la salud");
        assertThat(instruccion.toLowerCase()).contains("precios");
    }

    /*
      El historial va como texto dentro de input y el mensaje nuevo va
      ultimo y anunciado: es lo que separa contexto de pedido.
    */
    @Test
    void elHistorialYElMensajeNuevoViajanEnLaEntrada() {
        ConsultaRemota conCharla = new ConsultaRemota(
                "y algo más tranqui?",
                List.of(
                        new AsistenteMensajeDTO("usuario", "recomendame algo"),
                        new AsistenteMensajeDTO("asistente", "Te tiro Funcional y Pádel")
                ),
                "Deportes: Yoga",
                "Yoga, Funcional",
                Set.of("Básquet"),
                Set.of("Yoga")
        );

        String entrada = gemini.armarEntrada(conCharla);

        assertThat(entrada).contains("CONVERSACION HASTA ACA");
        assertThat(entrada).contains("Persona: recomendame algo");
        assertThat(entrada).contains("Vos: Te tiro Funcional y Pádel");
        assertThat(entrada).contains("YA RECHAZADOS POR LA PERSONA, no los propongas: Básquet");
        assertThat(entrada).endsWith("y algo más tranqui?");
    }

    @Test
    void sinCredencialesNoEstaDisponibleYNoIntentaLlamar() {
        assertThat(gemini.estaDisponible()).isFalse();
        assertThat(gemini.conversar(consulta())).isEmpty();
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
