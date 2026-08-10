package com.dondeentreno.api.asistente;

import com.dondeentreno.api.asistente.PerfilConversacion.Preferencia;
import com.dondeentreno.api.dto.AsistenteMensajeDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la memoria del asistente.
 *
 * Es la pieza que hace cumplir "si dijiste que no, no te lo vuelvo a
 * ofrecer", y por eso tiene que ser determinística y estar cubierta: la
 * garantía no puede depender de que un modelo se acuerde.
 *
 * Los falsos positivos importan tanto como los aciertos. Descartar por
 * error un deporte que la persona estaba considerando es peor que no
 * detectar un rechazo, porque el usuario siempre puede repetirlo.
 */
class AnalizadorConversacionTest {

    private final AnalizadorConversacion analizador = new AnalizadorConversacion();

    private PerfilConversacion perfilDe(String mensaje) {
        return analizador.analizar(mensaje, List.of());
    }

    private boolean rechaza(PerfilConversacion perfil, String nombre) {
        return perfil.rechazaNombre(nombre);
    }

    /* ------------------------- rechazos ------------------------- */

    @Test
    void detectaElRechazoDirectoDeUnDeporte() {
        PerfilConversacion perfil = perfilDe("no quiero básquet");

        assertThat(rechaza(perfil, "Básquet")).isTrue();
        assertThat(rechaza(perfil, "Vóley")).isFalse();
    }

    @Test
    void entiendeLasDistintasFormasDeDecirQueNo() {
        assertThat(rechaza(perfilDe("odio el running"), "Running")).isTrue();
        assertThat(rechaza(perfilDe("me aburre el gym"), "Musculación")).isTrue();
        assertThat(rechaza(perfilDe("nada de natación"), "Natación")).isTrue();
        assertThat(rechaza(perfilDe("el boxeo me da miedo"), "Boxeo")).isTrue();
        assertThat(rechaza(perfilDe("dejé el fútbol hace años"), "Fútbol")).isTrue();
    }

    /* La negación no cruza la coma ni el "pero". */
    @Test
    void elRechazoNoSeDerramaSobreLaOtraMitadDeLaFrase() {
        PerfilConversacion perfil = perfilDe("no me gusta el básquet pero el vóley sí");

        assertThat(rechaza(perfil, "Básquet")).isTrue();
        assertThat(rechaza(perfil, "Vóley")).isFalse();
    }

    /*
      El falso positivo mas caro: "no se si yoga o pilates" descartaba
      justo los dos deportes que la persona estaba considerando.
    */
    @Test
    void noSeSiEsIndecisionYNoRechazaNada() {
        PerfilConversacion perfil = perfilDe("no sé si yoga o pilates");

        assertThat(rechaza(perfil, "Yoga")).isFalse();
        assertThat(rechaza(perfil, "Pilates")).isFalse();
    }

    @Test
    void nuncaHiceAlgoEsInteresNoRechazo() {
        PerfilConversacion perfil = perfilDe("nunca hice yoga, me gustaría probar");

        assertThat(rechaza(perfil, "Yoga")).isFalse();
        assertThat(perfil.quiere(Preferencia.PROGRESIVO)).isTrue();
    }

    @Test
    void unaConsultaSinNegacionNoRechazaNada() {
        assertThat(perfilDe("quiero hacer básquet").deportesRechazados()).isEmpty();
        assertThat(perfilDe("no sé qué deporte elegir").deportesRechazados()).isEmpty();
    }

    /* --------------------- rechazo de grupo --------------------- */

    @Test
    void rechazarLaPeleaSacaTodoElGrupoDeCombate() {
        PerfilConversacion perfil = perfilDe("no me gustan los deportes de pelea");

        assertThat(perfil.rechazaCombate()).isTrue();
        assertThat(rechaza(perfil, "Boxeo")).isTrue();
        assertThat(rechaza(perfil, "Karate")).isTrue();
        assertThat(rechaza(perfil, "MMA")).isTrue();
        assertThat(rechaza(perfil, "Vóley")).isFalse();
    }

    @Test
    void sinContactoFisicoTambienSacaElCombate() {
        assertThat(perfilDe("quiero algo sin contacto físico").rechazaCombate()).isTrue();
        assertThat(perfilDe("nada de artes marciales").rechazaCombate()).isTrue();
    }

    /* --------------------- preferencias --------------------- */

    @Test
    void detectaLosEjesQueLaPersonaMarca() {
        assertThat(perfilDe("quiero algo social").quiere(Preferencia.SOCIAL)).isTrue();
        assertThat(perfilDe("busco algo tranqui").quiere(Preferencia.TRANQUILO)).isTrue();
        assertThat(perfilDe("quiero algo bien intenso").quiere(Preferencia.INTENSO)).isTrue();
        assertThat(perfilDe("que los ejercicios varíen").quiere(Preferencia.VARIEDAD)).isTrue();
        assertThat(perfilDe("me canso rápido").quiere(Preferencia.PROGRESIVO)).isTrue();
        assertThat(perfilDe("quiero competir en torneos").quiere(Preferencia.COMPETITIVO)).isTrue();
        assertThat(perfilDe("para ganar más aire").quiere(Preferencia.AIRE)).isTrue();
    }

    @Test
    void bajarElEstresCuentaComoTranquilo() {
        assertThat(perfilDe("quiero bajar el estrés").quiere(Preferencia.TRANQUILO)).isTrue();
    }

    /* "al aire libre" habla de estar afuera, no de resistencia. */
    @Test
    void alAireLibreNoEsPedirCardio() {
        assertThat(perfilDe("quiero algo al aire libre").quiere(Preferencia.AIRE)).isFalse();
    }

    @Test
    void meAburreElGymEsVariedadYTambienRechazo() {
        PerfilConversacion perfil = perfilDe("me aburre el gym");

        assertThat(perfil.quiere(Preferencia.VARIEDAD)).isTrue();
        assertThat(rechaza(perfil, "Musculación")).isTrue();
    }

    /* Salió del smoke con datos reales: faltaba la primera persona. */
    @Test
    void tambienEntiendeElRechazoEnPrimeraPersona() {
        assertThat(rechaza(perfilDe("me aburro con el gym"), "Musculación")).isTrue();
        assertThat(rechaza(perfilDe("me canso con el running"), "Running")).isTrue();
    }

    /* --------------------- salud --------------------- */

    @Test
    void detectaCuandoHayUnTemaDeSalud() {
        assertThat(perfilDe("me duele la espalda").mencionaSalud()).isTrue();
        assertThat(perfilDe("tengo una lesión en la rodilla").mencionaSalud()).isTrue();
        assertThat(perfilDe("estoy embarazada").mencionaSalud()).isTrue();
        assertThat(perfilDe("quiero algo social").mencionaSalud()).isFalse();
    }

    /* --------------------- historial --------------------- */

    @Test
    void acumulaPreferenciasYRechazosDeTodaLaConversacion() {
        PerfilConversacion perfil = analizador.analizar(
                "y que sea variado",
                List.of(
                        new AsistenteMensajeDTO("usuario", "quiero algo social"),
                        new AsistenteMensajeDTO("asistente", "Te tiro Básquet y Funcional"),
                        new AsistenteMensajeDTO("usuario", "no quiero básquet")
                )
        );

        assertThat(perfil.quiere(Preferencia.SOCIAL)).isTrue();
        assertThat(perfil.quiere(Preferencia.VARIEDAD)).isTrue();
        assertThat(rechaza(perfil, "Básquet")).isTrue();
    }

    /* Lo que dijo el asistente no cuenta como preferencia del usuario. */
    @Test
    void loQueDijoElAsistenteNoContaminaElPerfil() {
        PerfilConversacion perfil = analizador.analizar(
                "dale",
                List.of(new AsistenteMensajeDTO(
                        "asistente",
                        "No te recomiendo boxeo si buscás algo tranquilo"
                ))
        );

        assertThat(perfil.rechazaCombate()).isFalse();
        assertThat(perfil.quiere(Preferencia.TRANQUILO)).isFalse();
    }

    @Test
    void registraLoQueElAsistenteYaSugirioParaNoRepetirse() {
        PerfilConversacion perfil = analizador.analizar(
                "otra idea?",
                List.of(new AsistenteMensajeDTO(
                        "asistente",
                        "1. Pádel: social. 2. Funcional: circuitos variados."
                ))
        );

        assertThat(perfil.yaSugeridos()).contains("padel", "funcional");
    }

    /* --------------------- pedido de recomendación --------------------- */

    @Test
    void reconoceCuandoLePidenUnaRecomendacion() {
        assertThat(analizador.pideRecomendacion("algún deporte que recomiendes?")).isTrue();
        assertThat(analizador.pideRecomendacion("no sé qué hacer")).isTrue();
        assertThat(analizador.pideRecomendacion("quiero empezar algo")).isTrue();
        assertThat(analizador.pideRecomendacion("asdkjh qwe")).isFalse();
        assertThat(analizador.pideRecomendacion("")).isFalse();
    }

    @Test
    void unPerfilVacioSeReconoceComoTal() {
        assertThat(perfilDe("asdkjh qwe").sinSenales()).isTrue();
        assertThat(perfilDe("quiero algo social").sinSenales()).isFalse();
    }
}
