package com.dondeentreno.api.asistente;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Lo que el asistente entendió del usuario a lo largo de TODA la charla,
 * no solo del último mensaje.
 *
 * Es la pieza que faltaba y la que explica el peor bug del asistente V1:
 * sin esto, "no quiero básquet" se leía como un mensaje suelto donde
 * aparece la palabra "básquet", y el motor respondía "¡Básquet es una gran
 * elección!". El rechazo se leía como pedido.
 *
 * Se recalcula entero en cada consulta a partir del historial que manda el
 * frontend. No hay estado en el servidor ni tabla en la base: el estado es
 * la conversación, y el servidor sigue siendo sin memoria.
 *
 * @param deportesRechazados nombres canónicos normalizados que el usuario
 *                           descartó. Se aplican como filtro duro EN
 *                           CÓDIGO, no pidiéndole al modelo que se acuerde.
 * @param rechazaCombate     dijo que no quiere pelear, en cualquiera de sus
 *                           formas. Saca todo el grupo de una.
 * @param preferencias       los ejes que fue marcando.
 * @param yaSugeridos        lo que el asistente ya nombró antes. No se
 *                           prohíbe, se posterga: si sigue siendo lo mejor
 *                           puede volver, pero primero se prueba otra cosa.
 * @param mencionaSalud      habló de dolor, lesión o condición médica. Baja
 *                           el impacto de lo que se recomienda y agrega la
 *                           derivación a un profesional.
 */
public record PerfilConversacion(
        Set<String> deportesRechazados,
        boolean rechazaCombate,
        Set<Preferencia> preferencias,
        Set<String> yaSugeridos,
        boolean mencionaSalud
) {

    /** Ejes que el usuario puede marcar hablando. */
    public enum Preferencia {
        /** Quiere gente alrededor: "algo social", "para conocer gente". */
        SOCIAL,
        /** Quiere bajar un cambio: "algo tranqui", "bajar el estrés". */
        TRANQUILO,
        /** Quiere transpirar: "algo intenso", "descargar". */
        INTENSO,
        /** Se aburre con lo repetitivo: "que los ejercicios varíen". */
        VARIEDAD,
        /** Arranca de cero o se cansa rápido: hay que ir de a poco. */
        PROGRESIVO,
        /** Quiere marcador: "competir", "torneos". */
        COMPETITIVO,
        /** Quiere resistencia: "ganar más aire". */
        AIRE
    }

    public static PerfilConversacion vacio() {
        return new PerfilConversacion(
                Set.of(), false, Set.of(), Set.of(), false
        );
    }

    /** ¿Sabemos algo de esta persona, o la consulta viene en blanco? */
    public boolean sinSenales() {
        return preferencias.isEmpty()
                && deportesRechazados.isEmpty()
                && !rechazaCombate
                && !mencionaSalud;
    }

    public boolean quiere(Preferencia preferencia) {
        return preferencias.contains(preferencia);
    }

    /**
     * ¿Este deporte está descartado?
     *
     * Un rechazo de combate saca todo el grupo aunque el usuario nunca haya
     * nombrado ese deporte en particular: quien dice "no me gustan los
     * deportes de pelea" no quiere que le ofrezcan karate.
     */
    public boolean rechaza(DeporteConocido deporte) {
        if (rechazaCombate && deporte.combate()) {
            return true;
        }

        return deportesRechazados.contains(ConocimientoDeportes.claveDe(deporte));
    }

    /** ¿Descartó este nombre suelto (puede no estar en el conocimiento)? */
    public boolean rechazaNombre(String nombre) {
        if (nombre == null) {
            return false;
        }

        String clave = ResolutorConsulta.normalizar(nombre);

        if (deportesRechazados.contains(clave)) {
            return true;
        }

        return rechazaCombate
                && ConocimientoDeportes.porTermino(clave)
                        .map(DeporteConocido::combate)
                        .orElse(false);
    }

    /** Nombres visibles de lo descartado, para contárselo al modelo. */
    public Set<String> nombresRechazados() {
        Set<String> nombres = new LinkedHashSet<>();

        for (DeporteConocido deporte : ConocimientoDeportes.todos()) {
            if (rechaza(deporte)) {
                nombres.add(deporte.nombre());
            }
        }

        return nombres;
    }
}
