package com.dondeentreno.api.asistente;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Limpia el texto que escribió el modelo antes de que llegue a la pantalla.
 *
 * Contexto de por qué existe: hasta el asistente V1, Gemini no podía poner
 * una sola palabra en pantalla — devolvía cinco términos y el backend
 * escribía todo. Esa propiedad era una defensa completa contra inyección
 * de instrucciones, y el V2 la resigna a cambio de que el asistente suene
 * humano.
 *
 * Lo que la reemplaza son dos cosas, y esta clase es la primera:
 *
 * 1. Del texto del modelo se borra todo lo que pueda ser un dato inventado
 *    con consecuencias: enlaces, mails, teléfonos, precios y horarios. Un
 *    usuario puede aceptar un consejo flojo; no puede aceptar un precio o
 *    un teléfono que no existe.
 * 2. (Fuera de acá) el modelo no arma enlaces, ni conteos, ni afirma qué
 *    hay publicado. Eso lo escribe el backend con datos de la búsqueda
 *    real, en RedactorRespuesta.
 *
 * Lo que NO puede hacer esta clase es detectar un club inventado: contra
 * eso juegan la instrucción de sistema y, sobre todo, que la línea de
 * "esto es lo que tenemos" nunca la escriba el modelo.
 */
public final class SanitizadorTexto {

    /** Tope del párrafo de consejo. El pedido es respuestas cortas. */
    public static final int MAX_MENSAJE = 700;

    /** Tope de un motivo o una pregunta de seguimiento. */
    public static final int MAX_FRAGMENTO = 160;

    /** [texto](url) -> texto. El enlace se cae, la palabra queda. */
    private static final Pattern ENLACE_MARKDOWN = Pattern.compile("\\[([^\\]]{0,120})\\]\\([^)]{0,300}\\)");

    private static final Pattern URL = Pattern.compile(
            "(?i)\\b(?:https?://|www\\.)\\S+|\\b[a-z0-9-]+\\.(?:com|ar|net|org|io|app|co)(?:/\\S*)?\\b"
    );

    private static final Pattern EMAIL = Pattern.compile("(?i)\\b[\\w.+-]+@[\\w-]+\\.[\\w.]+\\b");

    /** Siete dígitos seguidos o más, con o sin separadores: es un teléfono. */
    private static final Pattern TELEFONO = Pattern.compile("\\+?\\d[\\d\\s().-]{6,}\\d");

    /** Marcas de asterisco y guion bajo: la UI muestra texto plano. */
    private static final Pattern MARCAS_MARKDOWN = Pattern.compile("[*_`#]+");

    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    /**
     * Frases sueltas que el modelo no tiene derecho a afirmar.
     *
     * Un precio o un horario inventado es el peor error posible del
     * asistente: manda a una persona a un lugar con una expectativa falsa.
     * La oración entera que los contenga se descarta.
     */
    private static final Pattern PRECIO = Pattern.compile(
            "(?i)(\\$\\s?\\d|\\b\\d[\\d.]*\\s?(pesos|mangos|lucas|usd|dolares)\\b|\\bcuesta\\s+\\d|\\bsale\\s+\\d)"
    );

    /*
      Solo hora del reloj. "2 horas por semana" es un consejo válido y no
      se toca; "18:30" o "20 hs" es un horario de clase que el modelo no
      tiene forma de saber.
    */
    private static final Pattern HORARIO = Pattern.compile(
            "(?i)\\b\\d{1,2}[:.]\\d{2}\\b|\\b\\d{1,2}\\s?hs\\b"
    );

    private SanitizadorTexto() {
    }

    /** Párrafo de consejo del modelo. */
    public static String limpiarMensaje(String valor) {
        return limpiar(valor, MAX_MENSAJE, true);
    }

    /** Motivo de un deporte o pregunta de seguimiento: corto y sin punto final. */
    public static String limpiarFragmento(String valor) {
        return limpiar(valor, MAX_FRAGMENTO, true);
    }

    /**
     * Historial que va DENTRO del prompt.
     *
     * Se limita el largo y se sacan saltos de línea para que un mensaje
     * largo no empuje la instrucción de sistema fuera de foco, pero no se
     * borran precios ni URLs: es texto que el usuario ya escribió y que el
     * modelo tiene que poder leer para entender la charla. Nunca vuelve a
     * la pantalla sin pasar por las otras dos funciones.
     */
    public static String limpiarParaPrompt(String valor, int maximo) {
        if (valor == null) {
            return "";
        }

        String plano = ESPACIOS.matcher(valor).replaceAll(" ").trim();

        return plano.length() <= maximo ? plano : plano.substring(0, maximo).trim();
    }

    private static String limpiar(String valor, int maximo, boolean quitarDatosInventables) {
        if (valor == null || valor.isBlank()) {
            return "";
        }

        String texto = ENLACE_MARKDOWN.matcher(valor).replaceAll("$1");
        /*
          El mail va ANTES que la URL: si no, el dominio de
          "hola@club.com" lo come el patrón de URL y queda "hola@"
          suelto, que ya no matchea como mail.
        */
        texto = EMAIL.matcher(texto).replaceAll(" ");
        texto = URL.matcher(texto).replaceAll(" ");
        texto = TELEFONO.matcher(texto).replaceAll(" ");
        texto = MARCAS_MARKDOWN.matcher(texto).replaceAll("");

        if (quitarDatosInventables) {
            texto = sinOracionesConDatosInventados(texto);
        }

        texto = ESPACIOS.matcher(texto).replaceAll(" ").trim();

        /* Espacios que quedaron colgando delante de la puntuación. */
        texto = texto.replaceAll("\\s+([.,;:!?])", "$1").trim();

        if (texto.length() <= maximo) {
            return texto;
        }

        /* Se corta en el último final de oración para no dejar la frase colgada. */
        String recortado = texto.substring(0, maximo);
        int ultimoCorte = Math.max(
                recortado.lastIndexOf('.'),
                Math.max(recortado.lastIndexOf('!'), recortado.lastIndexOf('?'))
        );

        return ultimoCorte > maximo / 2
                ? recortado.substring(0, ultimoCorte + 1).trim()
                : recortado.trim();
    }

    private static String sinOracionesConDatosInventados(String texto) {
        String[] oraciones = texto.split("(?<=[.!?])\\s+");
        List<String> conservadas = new ArrayList<>();

        for (String oracion : oraciones) {
            if (PRECIO.matcher(oracion).find() || HORARIO.matcher(oracion).find()) {
                continue;
            }

            conservadas.add(oracion);
        }

        return String.join(" ", conservadas);
    }
}
