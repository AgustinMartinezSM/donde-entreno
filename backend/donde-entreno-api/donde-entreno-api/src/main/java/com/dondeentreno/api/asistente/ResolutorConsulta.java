package com.dondeentreno.api.asistente;

import com.dondeentreno.api.dto.BarrioDTO;
import com.dondeentreno.api.dto.CategoriaDeportivaDTO;
import com.dondeentreno.api.dto.CiudadDTO;
import com.dondeentreno.api.dto.DeporteDTO;
import com.dondeentreno.api.dto.FiltroOpcionesDTO;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;

/**
 * Traduce un mensaje en lenguaje natural a filtros del catálogo REAL.
 *
 * Es la pieza que hace que el asistente no invente: no hay catálogo
 * espejo ni lista hardcodeada de deportes. Todo se resuelve contra lo que
 * devuelve FiltroService, que sale de la base. Si algo no está en la
 * base, no se puede resolver, y punto.
 *
 * Cómo decide: normaliza (sin tildes ni mayúsculas), y busca cada nombre
 * del catálogo como FRASE COMPLETA con borde de palabra dentro del
 * mensaje. Gana la coincidencia más larga. Nada de subcadenas sueltas:
 * en el motor del frontend ese atajo hacía que "mi" resolviera Jiu Jitsu
 * por estar dentro de "submission".
 */
@Component
public class ResolutorConsulta {

    /**
     * Largo mínimo de un nombre del catálogo para buscarlo dentro de una
     * frase. Con menos, cualquier sigla corta ("MMA" es el límite) pega
     * demasiado seguido por casualidad.
     */
    private static final int LARGO_MINIMO_NOMBRE = 3;

    /** Sinónimos de nivel, apuntando a los valores que acepta la búsqueda. */
    private static final Map<String, String> NIVELES = Map.ofEntries(
            Map.entry("principiante", "PRINCIPIANTE"),
            Map.entry("principiantes", "PRINCIPIANTE"),
            Map.entry("inicial", "PRINCIPIANTE"),
            Map.entry("desde cero", "PRINCIPIANTE"),
            Map.entry("empezar de cero", "PRINCIPIANTE"),
            Map.entry("nunca hice", "PRINCIPIANTE"),
            Map.entry("intermedio", "INTERMEDIO"),
            Map.entry("avanzado", "AVANZADO"),
            Map.entry("experimentado", "AVANZADO")
    );

    /** Sinónimos de modalidad. */
    private static final Map<String, String> MODALIDADES = Map.of(
            "presencial", "PRESENCIAL",
            "online", "ONLINE",
            "virtual", "ONLINE",
            "a distancia", "ONLINE",
            "por videollamada", "ONLINE",
            "mixta", "MIXTA",
            "hibrida", "MIXTA"
    );

    /**
     * Resuelve los filtros de un mensaje contra el catálogo recibido.
     *
     * @param texto mensaje del usuario, tal como lo escribió.
     * @param opciones catálogo real (deportes, categorías, ciudades, barrios).
     * @return filtros entendidos; todos los campos pueden venir en null.
     */
    public FiltrosResueltos resolver(String texto, FiltroOpcionesDTO opciones) {
        String normalizado = normalizar(texto);

        if (normalizado.isBlank() || opciones == null) {
            return FiltrosResueltos.vacio();
        }

        DeporteDTO deporte = mejorCoincidencia(
                opciones.getDeportes(),
                normalizado,
                deporteDTO -> List.of(
                        deporteDTO.getNombre(),
                        desdeSlug(deporteDTO.getSlug())
                )
        );

        CategoriaDeportivaDTO categoria = mejorCoincidencia(
                opciones.getCategorias(),
                normalizado,
                categoriaDTO -> List.of(
                        categoriaDTO.getNombre(),
                        desdeSlug(categoriaDTO.getSlug())
                )
        );

        BarrioDTO barrio = mejorCoincidencia(
                opciones.getBarrios(),
                normalizado,
                barrioDTO -> List.of(barrioDTO.getNombre())
        );

        CiudadDTO ciudad = mejorCoincidencia(
                opciones.getCiudades(),
                normalizado,
                ciudadDTO -> List.of(
                        ciudadDTO.getNombre(),
                        desdeSlug(ciudadDTO.getSlug())
                )
        );

        /*
          El deporte le gana a la categoría: si alguien dice "karate", que
          es más específico, no tiene sentido mandarlo al listado entero de
          artes marciales.
        */
        boolean usarCategoria = deporte == null && categoria != null;

        return new FiltrosResueltos(
                deporte != null ? deporte.getSlug() : null,
                deporte != null ? deporte.getNombre() : null,
                usarCategoria ? categoria.getSlug() : null,
                usarCategoria ? categoria.getNombre() : null,
                barrio != null ? barrio.getId() : null,
                barrio != null ? barrio.getNombre() : null,
                ciudad != null ? ciudad.getSlug() : null,
                ciudad != null ? ciudad.getNombre() : null,
                buscarEnDiccionario(normalizado, NIVELES),
                buscarEnDiccionario(normalizado, MODALIDADES)
        );
    }

    /**
     * Normaliza igual que el buscador del frontend: sin tildes, en
     * minúsculas, sin signos y con espacios simples.
     */
    public static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }

        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Busca cada nombre candidato dentro del texto como frase completa y
     * devuelve el elemento cuyo nombre más largo haya coincidido.
     */
    private <T> T mejorCoincidencia(
            List<T> elementos,
            String textoNormalizado,
            java.util.function.Function<T, List<String>> nombresDe
    ) {
        if (elementos == null) {
            return null;
        }

        T mejor = null;
        int largoDelMejor = 0;

        for (T elemento : elementos) {
            for (String nombre : nombresDe.apply(elemento)) {
                String candidato = normalizar(nombre);

                if (candidato.length() < LARGO_MINIMO_NOMBRE) {
                    continue;
                }

                if (contieneFrase(textoNormalizado, candidato)
                        && candidato.length() > largoDelMejor) {
                    mejor = elemento;
                    largoDelMejor = candidato.length();
                }
            }
        }

        return mejor;
    }

    private String buscarEnDiccionario(String textoNormalizado, Map<String, String> diccionario) {
        String encontrado = null;
        int largoDelMejor = 0;

        for (Map.Entry<String, String> entrada : diccionario.entrySet()) {
            String clave = entrada.getKey();

            if (contieneFrase(textoNormalizado, clave) && clave.length() > largoDelMejor) {
                encontrado = entrada.getValue();
                largoDelMejor = clave.length();
            }
        }

        return encontrado;
    }

    /** Coincidencia con borde de palabra a ambos lados, sin regex. */
    private boolean contieneFrase(String textoNormalizado, String frase) {
        return (" " + textoNormalizado + " ").contains(" " + frase + " ");
    }

    /** "muay-thai" -> "muay thai", para poder buscarlo dentro de una frase. */
    private String desdeSlug(String slug) {
        return slug == null ? "" : slug.replace('-', ' ');
    }
}
