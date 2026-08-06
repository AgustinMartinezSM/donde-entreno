package com.dondeentreno.api.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;

/**
 * Registra la funcion SQL {@code unaccent(texto)} de PostgreSQL como
 * funcion de Hibernate, para poder invocarla desde JPQL sin pasar la
 * query a nativa.
 *
 * La usa la busqueda publica por texto ({@code ActividadRepository})
 * para que las coincidencias sean insensibles a tildes
 * ("futbol" encuentra "Futbol", "natacion" encuentra "Natacion").
 * Requiere la extension {@code unaccent} en la base
 * (migracion {@code database/scripts/16_prepare_busqueda_unaccent.sql}).
 *
 * No es un bean de Spring: Hibernate lo descubre por ServiceLoader via
 * {@code META-INF/services/org.hibernate.boot.model.FunctionContributor}.
 * El registro es solo metadata; no ejecuta SQL hasta que una query real
 * llama a {@code unaccent(...)} (los tests unitarios mockean el repo y
 * no la disparan; los IT corren contra PostgreSQL real con la extension).
 *
 * Ver docs/plan-busqueda-sin-tildes.md
 */
public class UnaccentFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry().registerPattern(
                "unaccent",
                "unaccent(?1)",
                functionContributions.getTypeConfiguration()
                        .getBasicTypeForJavaType(String.class)
        );
    }
}
