package com.dondeentreno.api.asistente;

import com.dondeentreno.api.dto.AsistenteMensajeDTO;
import com.dondeentreno.api.asistente.PerfilConversacion.Preferencia;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lee la conversación entera y arma el perfil del usuario.
 *
 * Todo acá es determinístico y sin red. Es a propósito: la garantía de
 * "si dijiste que no querés básquet, no te vuelvo a ofrecer básquet" no
 * puede depender de que un modelo se acuerde. Se calcula con reglas, se
 * aplica como filtro, y se puede testear.
 *
 * Cómo detecta un rechazo: parte el mensaje en cláusulas (por punto, coma
 * y "pero"), y dentro de cada una busca una marca de negación. Todo
 * deporte nombrado DESPUÉS de esa marca, en la misma cláusula, queda
 * descartado. Así "no me gusta el básquet pero sí el vóley" descarta uno
 * solo.
 */
@Component
public class AnalizadorConversacion {

    /**
     * Marcas de rechazo. Incluye las de gusto ("me aburre", "me da miedo")
     * porque en la práctica funcionan igual: lo que viene después no lo
     * quiere.
     *
     * Quedaron afuera a propósito tres que parecían obvias: "nunca" y
     * "jamás" ("nunca hice yoga" suele ser interés, no rechazo) y "menos"
     * ("por lo menos correr" no rechaza correr).
     */
    private static final List<String> MARCAS_DE_RECHAZO = List.of(
            "no", "nada", "sin", "tampoco",
            "odio", "detesto", "evito", "evitar",
            /* "me aburro con el gym" es tan común como "me aburre el gym". */
            "aburre", "aburren", "aburro", "aburrio", "aburria",
            "aburrido", "aburrida",
            "miedo", "cansa", "cansan", "canso", "harto", "harta",
            "cansado", "cansada",
            "dejar", "deje", "abandone"
    );

    /**
     * Marcas que pueden llevar el deporte ANTES o después.
     *
     * "odio el running" lo pone después, "el boxeo me da miedo" lo pone
     * antes. Con las negaciones puras no pasa: nadie dice "el boxeo no",
     * y mirar hacia atrás con un "no" suelto rompía frases como
     * "hago yoga y no quiero cambiar".
     */
    private static final List<String> MARCAS_QUE_MIRAN_TODA_LA_CLAUSULA = List.of(
            "odio", "detesto", "evito", "evitar",
            "aburre", "aburren", "aburro", "aburrio", "aburria",
            "aburrido", "aburrida",
            "miedo", "cansa", "cansan", "canso", "harto", "harta",
            "cansado", "cansada",
            "dejar", "deje", "abandone"
    );

    /**
     * Arranques que llevan un "no" adelante pero no rechazan nada.
     *
     * Sin esto, "no sé si yoga o pilates" descartaba los dos deportes que
     * la persona estaba considerando, que es el falso positivo más caro
     * posible: el asistente deja de ofrecer justo lo que le interesaba.
     */
    private static final List<String> NEGACIONES_QUE_NO_RECHAZAN = List.of(
            "no se", "no sabria", "no sabria decir", "no tengo idea",
            "ni idea", "no me decido", "no importa", "no estoy seguro",
            "no estoy segura", "no conozco"
    );

    /** Separadores de cláusula: la negación no cruza estos límites. */
    private static final String[] CORTES_DE_CLAUSULA = {".", ",", ";", " pero ", " aunque ", " igual "};

    /*
      Frases que marcan cada eje. Se buscan como frase completa con borde
      de palabra sobre el texto normalizado, así que "social" no matchea
      dentro de otra palabra.
    */
    private static final Map<Preferencia, List<String>> FRASES_POR_PREFERENCIA = Map.of(
            Preferencia.SOCIAL, List.of(
                    "social", "sociales", "conocer gente", "con gente", "en grupo",
                    "grupal", "grupales", "hacer amigos", "en equipo", "acompanado",
                    "acompanada", "con otra gente", "para socializar"
            ),
            Preferencia.TRANQUILO, List.of(
                    "tranqui", "tranquilo", "tranquila", "suave", "relajado", "relajada",
                    "relajarme", "relajar", "bajo impacto", "sin exigirme", "sin matarme",
                    "estres", "estresado", "estresada", "ansiedad", "despejar",
                    "bajar un cambio", "algo calmo", "distenderme"
            ),
            Preferencia.INTENSO, List.of(
                    "intenso", "intensa", "exigente", "transpirar", "descargar",
                    "descargarme", "a full", "fuerte", "quemar", "quemar calorias",
                    "alta intensidad", "al maximo", "matarme"
            ),
            Preferencia.VARIEDAD, List.of(
                    "varien", "varie", "variado", "variada", "variedad", "que cambien",
                    "aburre", "aburrido", "aburrida", "siempre lo mismo", "repetitivo",
                    "repetitiva", "rutinario", "monotono", "distinto cada"
            ),
            Preferencia.PROGRESIVO, List.of(
                    "me canso", "me canso rapido", "sin estado", "fuera de estado",
                    "nunca hice", "desde cero", "recien empiezo", "de a poco",
                    "principiante", "sedentario", "sedentaria", "hace anos que no",
                    "estoy oxidado", "estoy oxidada", "sin aire", "me agito"
            ),
            Preferencia.COMPETITIVO, List.of(
                    "competitivo", "competitiva", "competir", "competencia",
                    "torneo", "torneos", "liga", "campeonato", "federado"
            ),
            Preferencia.AIRE, List.of(
                    "aire", "mas aire", "resistencia", "aguante", "cardio",
                    "pulmon", "fondo fisico", "capacidad aerobica"
            )
    );

    /**
     * Señales de que hay una cuestión de salud de por medio.
     *
     * No convierte al asistente en médico: hace lo contrario. Cuando
     * aparece alguna, la respuesta agrega la derivación a un profesional y
     * el recomendador se limita a lo de bajo impacto.
     */
    private static final List<String> SENALES_DE_SALUD = List.of(
            "lesion", "lesionado", "lesionada", "lesiones", "me duele", "dolor",
            "operado", "operada", "cirugia", "hernia", "artrosis", "artritis",
            "rehabilitacion", "kinesiologo", "kinesiologa", "medico", "medica",
            "rodilla", "rodillas", "espalda", "cervicales", "lumbar", "menisco",
            "embarazada", "presion alta", "hipertension", "diabetes", "asma",
            "problema cardiaco", "corazon"
    );

    /**
     * Arma el perfil con el historial más el mensaje que se está
     * respondiendo.
     *
     * @param mensajeActual lo que el usuario acaba de escribir.
     * @param historial     mensajes previos, en orden. Puede venir vacío.
     */
    public PerfilConversacion analizar(String mensajeActual, List<AsistenteMensajeDTO> historial) {
        List<String> delUsuario = new ArrayList<>();
        List<String> delAsistente = new ArrayList<>();

        if (historial != null) {
            for (AsistenteMensajeDTO mensaje : historial) {
                if (mensaje == null || mensaje.getTexto() == null || mensaje.getTexto().isBlank()) {
                    continue;
                }

                if (mensaje.esDelUsuario()) {
                    delUsuario.add(mensaje.getTexto());
                } else if (mensaje.esDelAsistente()) {
                    delAsistente.add(mensaje.getTexto());
                }
            }
        }

        if (mensajeActual != null && !mensajeActual.isBlank()) {
            delUsuario.add(mensajeActual);
        }

        Set<String> rechazados = new LinkedHashSet<>();
        Set<Preferencia> preferencias = new LinkedHashSet<>();
        boolean rechazaCombate = false;
        boolean salud = false;

        for (String texto : delUsuario) {
            RechazosDeUnMensaje rechazos = detectarRechazos(texto);
            rechazados.addAll(rechazos.deportes());
            rechazaCombate = rechazaCombate || rechazos.combate();

            preferencias.addAll(detectarPreferencias(texto));
            salud = salud || mencionaSalud(texto);
        }

        return new PerfilConversacion(
                Set.copyOf(rechazados),
                rechazaCombate,
                Set.copyOf(preferencias),
                Set.copyOf(detectarYaSugeridos(delAsistente)),
                salud
        );
    }

    /**
     * ¿Este mensaje está pidiendo que le recomendemos algo?
     *
     * Sirve para no responder con una recomendación a cualquier cosa. Si
     * la persona escribió algo que no entendemos y tampoco parece un
     * pedido, es más honesto decir que no entendimos que tirarle cinco
     * deportes al azar.
     */
    public boolean pideRecomendacion(String mensaje) {
        String normalizado = ResolutorConsulta.normalizar(mensaje);

        if (normalizado.isBlank()) {
            return false;
        }

        return PEDIDOS_DE_RECOMENDACION.stream().anyMatch(normalizado::contains);
    }

    /**
     * Se buscan como subcadena y no como frase completa a propósito:
     * "recomend" cubre recomendás, recomendame y recomendación de una
     * sola vez.
     *
     * Van las dos raíces porque el verbo diptonga: "¿qué me recomendás?"
     * y "algún deporte que recomiendes?" no comparten prefijo, y la
     * segunda es justamente la forma más común de la pregunta.
     */
    private static final List<String> PEDIDOS_DE_RECOMENDACION = List.of(
            "recomend", "recomien", "sugeri", "sugerencia", "que deporte", "que actividad",
            "que puedo hacer", "que hago", "que me conviene", "no se que",
            "ayudame a elegir", "quiero empezar", "quiero arrancar",
            "quiero hacer", "quiero probar", "quiero entrenar", "quiero moverme",
            "algo para", "algo que", "opciones", "ideas", "elegir",
            "por donde empiezo", "me sirve", "me convendria"
    );

    /** Lo que el asistente ya nombró, para no repetirse turno a turno. */
    private Set<String> detectarYaSugeridos(List<String> mensajesDelAsistente) {
        Set<String> nombrados = new LinkedHashSet<>();

        for (String texto : mensajesDelAsistente) {
            for (DeporteConocido deporte : ConocimientoDeportes.mencionadosEn(texto)) {
                nombrados.add(ConocimientoDeportes.claveDe(deporte));
            }
        }

        return nombrados;
    }

    private record RechazosDeUnMensaje(Set<String> deportes, boolean combate) {
    }

    /**
     * Busca en cada cláusula una marca de negación y descarta lo que viene
     * después.
     *
     * Deliberadamente conservador: si no hay marca, no se rechaza nada. El
     * costo de un falso positivo (dejar de recomendar algo que sí querían)
     * es peor que el de un falso negativo, porque el usuario siempre puede
     * volver a pedirlo.
     */
    private RechazosDeUnMensaje detectarRechazos(String texto) {
        Set<String> deportes = new LinkedHashSet<>();
        boolean combate = false;

        for (String clausula : partirEnClausulas(texto)) {
            String resto = parteRechazada(clausula);

            if (resto == null) {
                continue;
            }

            for (DeporteConocido deporte : ConocimientoDeportes.mencionadosEn(resto)) {
                deportes.add(ConocimientoDeportes.claveDe(deporte));
            }

            if (ConocimientoDeportes.mencionaCombate(resto)) {
                combate = true;
            }
        }

        return new RechazosDeUnMensaje(deportes, combate);
    }

    private List<String> partirEnClausulas(String texto) {
        List<String> partes = new ArrayList<>();
        partes.add(texto == null ? "" : texto.toLowerCase());

        for (String corte : CORTES_DE_CLAUSULA) {
            List<String> siguientes = new ArrayList<>();

            for (String parte : partes) {
                siguientes.addAll(List.of(parte.split(java.util.regex.Pattern.quote(corte))));
            }

            partes = siguientes;
        }

        return partes;
    }

    /**
     * Devuelve el trozo de la cláusula donde buscar lo rechazado, o null
     * si esta cláusula no rechaza nada.
     *
     * Con una negación pura solo cuenta lo que viene después; con un
     * verbo de rechazo cuenta toda la cláusula, porque el deporte puede
     * ir de cualquiera de los dos lados.
     */
    private String parteRechazada(String clausula) {
        String normalizada = ResolutorConsulta.normalizar(clausula);

        if (normalizada.isBlank()) {
            return null;
        }

        String conBordes = " " + normalizada + " ";

        if (NEGACIONES_QUE_NO_RECHAZAN.stream()
                .anyMatch(frase -> conBordes.contains(" " + frase + " "))) {
            return null;
        }

        String[] palabras = normalizada.split(" ");

        for (int indice = 0; indice < palabras.length; indice += 1) {
            String palabra = palabras[indice];

            if (MARCAS_QUE_MIRAN_TODA_LA_CLAUSULA.contains(palabra)) {
                return normalizada;
            }

            if (MARCAS_DE_RECHAZO.contains(palabra)) {
                return String.join(" ", List.of(palabras).subList(indice + 1, palabras.length));
            }
        }

        return null;
    }

    private Set<Preferencia> detectarPreferencias(String texto) {
        String conBordes = " " + ResolutorConsulta.normalizar(texto) + " ";
        Set<Preferencia> encontradas = new LinkedHashSet<>();

        for (Map.Entry<Preferencia, List<String>> entrada : FRASES_POR_PREFERENCIA.entrySet()) {
            boolean marca = entrada.getValue().stream()
                    .anyMatch(frase -> conBordes.contains(" " + frase + " "));

            if (marca) {
                encontradas.add(entrada.getKey());
            }
        }

        /*
          "aire libre" no habla de resistencia, habla de estar afuera. Sin
          esta excepción, "quiero algo al aire libre" pedía cardio.
        */
        if (conBordes.contains(" aire libre ")
                && FRASES_POR_PREFERENCIA.get(Preferencia.AIRE).stream()
                        .filter(frase -> !frase.equals("aire"))
                        .noneMatch(frase -> conBordes.contains(" " + frase + " "))) {
            encontradas.remove(Preferencia.AIRE);
        }

        return encontradas;
    }

    private boolean mencionaSalud(String texto) {
        String conBordes = " " + ResolutorConsulta.normalizar(texto) + " ";

        return SENALES_DE_SALUD.stream()
                .anyMatch(senal -> conBordes.contains(" " + senal + " "));
    }
}
