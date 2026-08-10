package com.dondeentreno.api.asistente;

import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.dto.DeporteDTO;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Foto de qué hay realmente publicado en DondeEntreno, en el momento de
 * responder.
 *
 * Es el puente entre el conocimiento deportivo (que sabe qué es el pádel
 * aunque nadie lo haya cargado) y la base (que sabe qué se puede mostrar).
 * Sin este cruce, el asistente o miente o se calla; con él puede decir las
 * tres cosas que corresponden: "hay 3 de esto", "el deporte existe pero
 * todavía no hay actividades", "esto va como idea, no lo tenemos".
 *
 * Se arma con UNA sola búsqueda sin filtros y se agrupa en memoria. El
 * catálogo publicado es chico (unidades, no miles), así que sale más
 * barato que pedir un conteo por deporte. Si algún día crece, acá va una
 * consulta agrupada en la base.
 */
public record DisponibilidadCatalogo(Map<String, EntradaCatalogo> porNombre) {

    /**
     * @param slug       slug real, el que va en los enlaces.
     * @param nombre     nombre real del catálogo (gana sobre el nuestro).
     * @param publicadas actividades publicadas hoy de ese deporte.
     */
    public record EntradaCatalogo(String slug, String nombre, int publicadas) {
    }

    public static DisponibilidadCatalogo vacia() {
        return new DisponibilidadCatalogo(Map.of());
    }

    /**
     * Cruza el catálogo de deportes con las actividades publicadas.
     *
     * @param deportes   los que existen en la base (FiltroService).
     * @param publicadas todas las actividades publicadas, sin filtrar.
     */
    public static DisponibilidadCatalogo desde(
            List<DeporteDTO> deportes,
            List<ActividadDTO> publicadas
    ) {
        Map<String, Integer> conteoPorSlug = new HashMap<>();

        if (publicadas != null) {
            for (ActividadDTO actividad : publicadas) {
                if (actividad.getDeporteSlug() == null) {
                    continue;
                }

                conteoPorSlug.merge(actividad.getDeporteSlug(), 1, Integer::sum);
            }
        }

        Map<String, EntradaCatalogo> indice = new LinkedHashMap<>();

        if (deportes != null) {
            for (DeporteDTO deporte : deportes) {
                if (deporte.getNombre() == null || deporte.getSlug() == null) {
                    continue;
                }

                EntradaCatalogo entrada = new EntradaCatalogo(
                        deporte.getSlug(),
                        deporte.getNombre(),
                        conteoPorSlug.getOrDefault(deporte.getSlug(), 0)
                );

                /*
                  Se indexa por nombre Y por slug con guiones convertidos a
                  espacios, porque el conocimiento nombra "Cross Training" y
                  el slug es "cross-training". Las dos formas tienen que
                  encontrar la misma entrada.
                */
                indice.putIfAbsent(ResolutorConsulta.normalizar(deporte.getNombre()), entrada);
                indice.putIfAbsent(
                        ResolutorConsulta.normalizar(deporte.getSlug().replace('-', ' ')),
                        entrada
                );
            }
        }

        return new DisponibilidadCatalogo(Map.copyOf(indice));
    }

    /**
     * Busca un deporte del conocimiento dentro del catálogo real.
     *
     * Prueba el nombre canónico y después los alias, para que "bici"
     * encuentre "Ciclismo" si está cargado. Vacío significa exactamente
     * "este deporte no está en DondeEntreno".
     */
    public Optional<EntradaCatalogo> buscar(DeporteConocido deporte) {
        Optional<EntradaCatalogo> porNombreCanonico =
                buscarPorTexto(deporte.nombre());

        if (porNombreCanonico.isPresent()) {
            return porNombreCanonico;
        }

        for (String alias : deporte.alias()) {
            Optional<EntradaCatalogo> porAlias = buscarPorTexto(alias);

            if (porAlias.isPresent()) {
                return porAlias;
            }
        }

        return Optional.empty();
    }

    public Optional<EntradaCatalogo> buscarPorTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(porNombre.get(ResolutorConsulta.normalizar(texto)));
    }

    /** Cuántas actividades publicadas hay de ese slug. */
    public int publicadasDe(String slug) {
        if (slug == null) {
            return 0;
        }

        return porNombre.values().stream()
                .filter(entrada -> slug.equals(entrada.slug()))
                .findFirst()
                .map(EntradaCatalogo::publicadas)
                .orElse(0);
    }
}
