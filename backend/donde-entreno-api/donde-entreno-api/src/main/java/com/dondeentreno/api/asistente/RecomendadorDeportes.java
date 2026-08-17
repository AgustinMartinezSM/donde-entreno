package com.dondeentreno.api.asistente;

import com.dondeentreno.api.asistente.DeporteConocido.Escala;
import com.dondeentreno.api.asistente.PerfilConversacion.Preferencia;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Elige qué deportes proponer, según lo que el usuario fue diciendo.
 *
 * Cumple dos roles y por eso vive solo:
 *
 * 1. Es el asistente completo cuando Gemini está apagado, sin cuota o
 *    caído. Responde peor que el modelo, pero responde bien: sigue
 *    entendiendo "algo social y variado" y sigue respetando los rechazos.
 * 2. Es el filtro de lo que propone el modelo. Los deportes que Gemini
 *    sugiere pasan por {@link #validar}, así la garantía de "no te vuelvo
 *    a ofrecer básquet" se cumple en código y no depende de que el modelo
 *    se haya acordado.
 */
@Component
public class RecomendadorDeportes {

    /** Cuántos deportes como mucho en una respuesta (el pedido: 3 a 6). */
    public static final int MAXIMO_SUGERENCIAS = 5;

    /**
     * Penalización por haberlo nombrado antes.
     *
     * No es una prohibición: si después de restarle esto sigue siendo lo
     * mejor para el perfil, vuelve a aparecer, y está bien que así sea. Lo
     * que evita es la respuesta idéntica dos turnos seguidos.
     */
    private static final int PENALIZACION_YA_SUGERIDO = -4;

    /** Empujoncito a lo que sí se puede mostrar, sin que decida solo. */
    private static final int BONUS_TIENE_ACTIVIDADES = 2;

    /**
     * Arma la recomendación desde cero para este perfil.
     *
     * @param perfil   lo entendido de la conversación.
     * @param catalogo qué hay publicado hoy.
     * @param maximo   tope de sugerencias.
     */
    public List<DeporteSugerido> recomendar(
            PerfilConversacion perfil,
            DisponibilidadCatalogo catalogo,
            int maximo
    ) {
        record Candidato(DeporteConocido deporte, int puntaje) {
        }

        List<Candidato> candidatos = new ArrayList<>();

        for (DeporteConocido deporte : ConocimientoDeportes.todos()) {
            if (perfil.rechaza(deporte)) {
                continue;
            }

            candidatos.add(new Candidato(deporte, puntuar(deporte, perfil, catalogo)));
        }

        /*
          Orden estable: por puntaje descendente y, ante empate, por el
          orden en que están declarados en el conocimiento. La misma
          conversación devuelve siempre lo mismo.
        */
        List<DeporteConocido> ordenados = candidatos.stream()
                .sorted(Comparator
                        .comparingInt(Candidato::puntaje).reversed()
                        .thenComparingInt(candidato ->
                                ConocimientoDeportes.todos().indexOf(candidato.deporte())))
                .map(Candidato::deporte)
                .limit(Math.max(1, maximo))
                .toList();

        return ordenados.stream()
                .map(deporte -> aSugerido(deporte, deporte.explicacion(), catalogo))
                .toList();
    }

    /**
     * Convierte los nombres que propuso el modelo en sugerencias reales.
     *
     * Acá se cae todo lo que no corresponde: un deporte inventado no
     * matchea contra el conocimiento ni contra el catálogo y desaparece; un
     * deporte rechazado se descarta aunque el modelo insista. Si después de
     * filtrar no queda nada, se devuelve vacío y el que decide es el
     * recomendador determinístico.
     *
     * @param propuestos pares nombre/motivo tal como vinieron del modelo.
     */
    public List<DeporteSugerido> validar(
            List<NombreYMotivo> propuestos,
            PerfilConversacion perfil,
            DisponibilidadCatalogo catalogo,
            int maximo
    ) {
        return validarConDetalle(propuestos, perfil, catalogo, maximo).validos();
    }

    /**
     * Lo mismo que {@link #validar}, contando además qué se descartó y por
     * qué.
     *
     * Existe para el log diagnóstico: cuando en producción la respuesta
     * del modelo caía entera, "propuso deportes inventados" y "propuso
     * deportes que la persona ya rechazó" eran indistinguibles, porque el
     * filtrado devolvía solo los sobrevivientes. Las listas traen los
     * nombres tal cual vinieron del modelo: quien los loguee debe
     * sanearlos antes.
     */
    public ResultadoValidacion validarConDetalle(
            List<NombreYMotivo> propuestos,
            PerfilConversacion perfil,
            DisponibilidadCatalogo catalogo,
            int maximo
    ) {
        List<DeporteSugerido> validos = new ArrayList<>();
        List<String> porCatalogo = new ArrayList<>();
        List<String> porRechazo = new ArrayList<>();
        int duplicados = 0;
        int invalidos = 0;

        if (propuestos == null || propuestos.isEmpty()) {
            return new ResultadoValidacion(List.of(), List.of(), List.of(), 0, 0);
        }

        Set<String> yaPuestos = new LinkedHashSet<>();

        for (NombreYMotivo propuesto : propuestos) {
            if (validos.size() >= Math.max(1, maximo)) {
                break;
            }

            if (propuesto == null || propuesto.nombre() == null || propuesto.nombre().isBlank()) {
                invalidos += 1;
                continue;
            }

            Optional<DeporteConocido> conocido =
                    ConocimientoDeportes.porTermino(propuesto.nombre());

            /*
              Si el modelo nombró un deporte que no está en el conocimiento
              pero SÍ en el catálogo real, vale igual: la base manda sobre
              nuestra lista de descripciones.
            */
            if (conocido.isEmpty()) {
                Optional<DisponibilidadCatalogo.EntradaCatalogo> delCatalogo =
                        catalogo.buscarPorTexto(propuesto.nombre());

                if (delCatalogo.isEmpty()) {
                    porCatalogo.add(propuesto.nombre());
                    continue;
                }

                if (perfil.rechazaNombre(propuesto.nombre())) {
                    porRechazo.add(propuesto.nombre());
                    continue;
                }

                DisponibilidadCatalogo.EntradaCatalogo entrada = delCatalogo.get();

                if (!yaPuestos.add(ResolutorConsulta.normalizar(entrada.nombre()))) {
                    duplicados += 1;
                    continue;
                }

                validos.add(new DeporteSugerido(
                        entrada.nombre(),
                        motivoLimpio(propuesto.motivo(), ""),
                        entrada.slug(),
                        entrada.publicadas()
                ));
                continue;
            }

            DeporteConocido deporte = conocido.get();

            if (perfil.rechaza(deporte)) {
                porRechazo.add(deporte.nombre());
                continue;
            }

            if (!yaPuestos.add(ConocimientoDeportes.claveDe(deporte))) {
                duplicados += 1;
                continue;
            }

            validos.add(aSugerido(
                    deporte,
                    motivoLimpio(propuesto.motivo(), deporte.explicacion()),
                    catalogo
            ));
        }

        return new ResultadoValidacion(
                List.copyOf(validos),
                List.copyOf(porCatalogo),
                List.copyOf(porRechazo),
                duplicados,
                invalidos
        );
    }

    /**
     * El desenlace de una validación, sobreviviente por sobreviviente y
     * caído por caído.
     *
     * @param validos               lo que se puede sugerir (idéntico a lo
     *                              que devuelve {@link #validar}).
     * @param descartadosPorCatalogo nombres que no matchearon ni el
     *                              conocimiento ni el catálogo real.
     * @param descartadosPorRechazo nombres que la persona ya descartó en
     *                              la conversación.
     * @param duplicados            propuestas repetidas dentro de la misma
     *                              respuesta.
     * @param invalidos             entradas sin nombre.
     */
    public record ResultadoValidacion(
            List<DeporteSugerido> validos,
            List<String> descartadosPorCatalogo,
            List<String> descartadosPorRechazo,
            int duplicados,
            int invalidos
    ) {
    }

    /**
     * Completa una lista corta con las mejores opciones que falten.
     *
     * El modelo a veces devuelve dos deportes cuando pedimos cuatro, o
     * tres de los cuales dos estaban rechazados. Antes que dar una
     * respuesta pobre, se rellena con el recomendador, sin repetir.
     */
    public List<DeporteSugerido> completar(
            List<DeporteSugerido> parciales,
            PerfilConversacion perfil,
            DisponibilidadCatalogo catalogo,
            int minimo,
            int maximo
    ) {
        if (parciales.size() >= minimo) {
            return parciales;
        }

        Set<String> yaPuestos = parciales.stream()
                .map(sugerido -> ResolutorConsulta.normalizar(sugerido.nombre()))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<DeporteSugerido> completada = new ArrayList<>(parciales);

        for (DeporteSugerido extra : recomendar(perfil, catalogo, maximo + parciales.size())) {
            if (completada.size() >= maximo) {
                break;
            }

            if (yaPuestos.add(ResolutorConsulta.normalizar(extra.nombre()))) {
                completada.add(extra);
            }
        }

        return List.copyOf(completada);
    }

    /** Par nombre/motivo, que es todo lo que aceptamos del modelo. */
    public record NombreYMotivo(String nombre, String motivo) {
    }

    private DeporteSugerido aSugerido(
            DeporteConocido deporte,
            String motivo,
            DisponibilidadCatalogo catalogo
    ) {
        Optional<DisponibilidadCatalogo.EntradaCatalogo> entrada = catalogo.buscar(deporte);

        return new DeporteSugerido(
                /* El nombre real del catálogo manda: es el que ve en la app. */
                entrada.map(DisponibilidadCatalogo.EntradaCatalogo::nombre).orElse(deporte.nombre()),
                motivo,
                entrada.map(DisponibilidadCatalogo.EntradaCatalogo::slug).orElse(null),
                entrada.map(DisponibilidadCatalogo.EntradaCatalogo::publicadas).orElse(0)
        );
    }

    private String motivoLimpio(String delModelo, String porDefecto) {
        String limpio = SanitizadorTexto.limpiarFragmento(delModelo);

        return limpio.isBlank() ? porDefecto : limpio;
    }

    /**
     * Puntúa un deporte contra el perfil.
     *
     * Los pesos están elegidos para que una preferencia explícita pese más
     * que el orden por defecto: quien pide "algo social" tiene que ver
     * cambiar la respuesta, no un reordenamiento cosmético.
     */
    private int puntuar(
            DeporteConocido deporte,
            PerfilConversacion perfil,
            DisponibilidadCatalogo catalogo
    ) {
        int puntaje = deporte.puntajeBase();

        if (perfil.quiere(Preferencia.SOCIAL)) {
            puntaje += segunEscala(deporte.social(), 5, 1, -4);
        }

        if (perfil.quiere(Preferencia.TRANQUILO)) {
            puntaje += segunEscala(deporte.intensidad(), -4, 1, 5);
            puntaje += segunEscala(deporte.impacto(), -2, 0, 2);
        }

        if (perfil.quiere(Preferencia.INTENSO)) {
            puntaje += segunEscala(deporte.intensidad(), 5, 1, -4);
        }

        if (perfil.quiere(Preferencia.VARIEDAD)) {
            puntaje += deporte.variedad() ? 4 : -3;
        }

        if (perfil.quiere(Preferencia.PROGRESIVO)) {
            /* Lo que castiga articulaciones es lo primero que hace abandonar. */
            puntaje += segunEscala(deporte.impacto(), -4, 1, 4);
            puntaje += segunEscala(deporte.intensidad(), -2, 1, 2);
        }

        if (perfil.quiere(Preferencia.COMPETITIVO)) {
            puntaje += deporte.competitivo() ? 4 : -2;
        }

        if (perfil.quiere(Preferencia.AIRE)) {
            puntaje += deporte.aire() ? 4 : -2;
        }

        if (perfil.mencionaSalud()) {
            /*
              Con una lesión de por medio no se sugiere nada de alto
              impacto ni de contacto, aunque el resto del perfil lo pida.
            */
            puntaje += segunEscala(deporte.impacto(), -8, 0, 5);

            if (deporte.combate()) {
                puntaje -= 8;
            }
        }

        if (perfil.yaSugeridos().contains(ConocimientoDeportes.claveDe(deporte))) {
            puntaje += PENALIZACION_YA_SUGERIDO;
        }

        if (catalogo.buscar(deporte).map(entrada -> entrada.publicadas() > 0).orElse(false)) {
            puntaje += BONUS_TIENE_ACTIVIDADES;
        }

        return puntaje;
    }

    private int segunEscala(Escala escala, int siAlta, int siMedia, int siBaja) {
        return switch (escala) {
            case ALTA -> siAlta;
            case MEDIA -> siMedia;
            case BAJA -> siBaja;
        };
    }
}
