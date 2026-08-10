package com.dondeentreno.api.asistente;

/**
 * Filtros que el asistente logró entender de un mensaje en lenguaje
 * natural, ya validados contra el catálogo real.
 *
 * Invariante del bloque: acá nunca entra un slug inventado. Todo valor
 * distinto de null salió de la base, así que los enlaces que se arman con
 * esto siempre llevan a algo que existe.
 */
public record FiltrosResueltos(
        String deporteSlug,
        String deporteNombre,
        String categoriaSlug,
        String categoriaNombre,
        Long barrioId,
        String barrioNombre,
        String ciudadSlug,
        String ciudadNombre,
        String nivel,
        String modalidad
) {

    public static FiltrosResueltos vacio() {
        return new FiltrosResueltos(
                null, null, null, null, null, null, null, null, null, null
        );
    }

    /** ¿Se entendió algo concreto, o la consulta quedó sin resolver? */
    public boolean hayAlgo() {
        return deporteSlug != null
                || categoriaSlug != null
                || barrioId != null
                || ciudadSlug != null
                || nivel != null
                || modalidad != null;
    }

    /** Mismos filtros pero sin el barrio, para reintentar más amplio. */
    public FiltrosResueltos sinBarrio() {
        return new FiltrosResueltos(
                deporteSlug,
                deporteNombre,
                categoriaSlug,
                categoriaNombre,
                null,
                null,
                ciudadSlug,
                ciudadNombre,
                nivel,
                modalidad
        );
    }
}
