package com.dondeentreno.api.asistente;

import com.dondeentreno.api.asistente.PerfilConversacion.Preferencia;
import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.dto.DeporteDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del recomendador determinístico.
 *
 * Es el asistente completo cuando Gemini está apagado, y el filtro de lo
 * que Gemini propone cuando está encendido. Si esto falla, falla el
 * bloque entero en los dos modos.
 */
class RecomendadorDeportesTest {

    private final RecomendadorDeportes recomendador = new RecomendadorDeportes();

    private DisponibilidadCatalogo catalogoCon(String... nombresConActividades) {
        List<DeporteDTO> deportes = List.of(
                deporte(1L, "Yoga", "yoga"),
                deporte(2L, "Funcional", "funcional"),
                deporte(3L, "Básquet", "basquet"),
                deporte(4L, "Natación", "natacion"),
                deporte(5L, "Boxeo", "boxeo")
        );

        List<ActividadDTO> publicadas = java.util.Arrays.stream(nombresConActividades)
                .map(slug -> {
                    ActividadDTO actividad = new ActividadDTO();
                    actividad.setTitulo("Actividad de " + slug);
                    actividad.setDeporteSlug(slug);
                    return actividad;
                })
                .toList();

        return DisponibilidadCatalogo.desde(deportes, publicadas);
    }

    private DeporteDTO deporte(Long id, String nombre, String slug) {
        return new DeporteDTO(id, nombre, slug, null, null, id.intValue(), 1L, "Cat", "cat");
    }

    private PerfilConversacion perfil(Preferencia... preferencias) {
        return new PerfilConversacion(Set.of(), false, Set.of(preferencias), Set.of(), false);
    }

    private List<String> nombres(List<DeporteSugerido> sugeridos) {
        return sugeridos.stream().map(DeporteSugerido::nombre).toList();
    }

    /* --------------------- recomendación --------------------- */

    /*
      Sin ninguna senal, la respuesta por defecto tiene que ser la que
      engancha a mas gente, no la primera del catalogo por casualidad.
    */
    @Test
    void sinSenalesDevuelveLaRecomendacionPorDefecto() {
        List<DeporteSugerido> sugeridos =
                recomendador.recomendar(PerfilConversacion.vacio(), catalogoCon(), 5);

        assertThat(nombres(sugeridos)).hasSize(5);
        assertThat(nombres(sugeridos)).contains("Pádel", "Funcional");
        /* Nada de combate como primera sugerencia a alguien que no pidio nada. */
        assertThat(nombres(sugeridos)).doesNotContain("MMA", "Muay Thai");
    }

    @Test
    void priorizaLoSocialCuandoPidenAlgoSocial() {
        List<String> sugeridos = nombres(
                recomendador.recomendar(perfil(Preferencia.SOCIAL), catalogoCon(), 5)
        );

        assertThat(sugeridos).contains("Pádel");
        assertThat(sugeridos).doesNotContain("Musculación", "Natación");
    }

    @Test
    void priorizaLoQueVariaCuandoSeAburrenDeLaRutina() {
        List<String> sugeridos = nombres(
                recomendador.recomendar(
                        perfil(Preferencia.VARIEDAD, Preferencia.SOCIAL),
                        catalogoCon(),
                        5
                )
        );

        assertThat(sugeridos).contains("Funcional");
        assertThat(sugeridos).doesNotContain("Musculación", "Stretching");
    }

    @Test
    void evitaElAltoImpactoCuandoSeCansanRapido() {
        List<String> sugeridos = nombres(
                recomendador.recomendar(perfil(Preferencia.PROGRESIVO), catalogoCon(), 5)
        );

        assertThat(sugeridos).doesNotContain("Running", "Cross Training", "Squash");
    }

    @Test
    void anteUnTemaDeSaludSoloOfreceBajoImpactoYNadaDeCombate() {
        PerfilConversacion conSalud =
                new PerfilConversacion(Set.of(), false, Set.of(), Set.of(), true);

        List<String> sugeridos = nombres(recomendador.recomendar(conSalud, catalogoCon(), 5));

        assertThat(sugeridos).doesNotContain("Running", "Boxeo", "Cross Training", "Fútbol");
        assertThat(sugeridos).contains("Natación");
    }

    /* --------------------- rechazos --------------------- */

    @Test
    void nuncaDevuelveUnDeporteRechazado() {
        PerfilConversacion conRechazo = new PerfilConversacion(
                Set.of("padel", "funcional"), false, Set.of(Preferencia.SOCIAL), Set.of(), false
        );

        assertThat(nombres(recomendador.recomendar(conRechazo, catalogoCon(), 6)))
                .doesNotContain("Pádel", "Funcional");
    }

    @Test
    void nuncaDevuelveCombateSiRechazaronLaPelea() {
        PerfilConversacion sinPelea =
                new PerfilConversacion(Set.of(), true, Set.of(Preferencia.INTENSO), Set.of(), false);

        assertThat(nombres(recomendador.recomendar(sinPelea, catalogoCon(), 8)))
                .doesNotContain("Boxeo", "Kickboxing", "Muay Thai", "MMA", "Karate", "Judo");
    }

    /* Lo ya dicho se posterga, no se prohibe: puede volver si sigue siendo lo mejor. */
    @Test
    void poneUltimoLoQueYaHabiaSugerido() {
        PerfilConversacion conMemoria = new PerfilConversacion(
                Set.of(), false, Set.of(Preferencia.SOCIAL), Set.of("padel", "funcional"), false
        );

        List<String> sugeridos = nombres(recomendador.recomendar(conMemoria, catalogoCon(), 3));

        assertThat(sugeridos).doesNotContain("Pádel", "Funcional");
    }

    /* --------------------- cruce con el catálogo --------------------- */

    @Test
    void marcaCualesTienenActividadesPublicadasYCualesNo() {
        List<DeporteSugerido> sugeridos = recomendador.recomendar(
                perfil(Preferencia.SOCIAL),
                catalogoCon("funcional", "funcional"),
                5
        );

        DeporteSugerido funcional = sugeridos.stream()
                .filter(sugerido -> sugerido.nombre().equals("Funcional"))
                .findFirst()
                .orElseThrow();

        assertThat(funcional.tieneActividades()).isTrue();
        assertThat(funcional.publicadas()).isEqualTo(2);
        assertThat(funcional.slug()).isEqualTo("funcional");

        DeporteSugerido padel = sugeridos.stream()
                .filter(sugerido -> sugerido.nombre().equals("Pádel"))
                .findFirst()
                .orElseThrow();

        /* Padel no esta en el catalogo: se recomienda igual, pero sin enlace. */
        assertThat(padel.slug()).isNull();
        assertThat(padel.esSoloRecomendacion()).isTrue();
    }

    @Test
    void todosLosSugeridosTraenSuExplicacion() {
        assertThat(recomendador.recomendar(PerfilConversacion.vacio(), catalogoCon(), 5))
                .allSatisfy(sugerido -> assertThat(sugerido.motivo()).isNotBlank());
    }

    /* --------------------- validación de lo que dice el modelo --------------------- */

    @Test
    void descartaLosNombresQueNoExisten() {
        List<DeporteSugerido> validos = recomendador.validar(
                List.of(
                        new RecomendadorDeportes.NombreYMotivo("Quidditch", "volar en escoba"),
                        new RecomendadorDeportes.NombreYMotivo("Funcional", "circuitos")
                ),
                PerfilConversacion.vacio(),
                catalogoCon("funcional"),
                5
        );

        assertThat(nombres(validos)).containsExactly("Funcional");
    }

    @Test
    void descartaLoRechazadoAunqueElModeloInsista() {
        PerfilConversacion conRechazo =
                new PerfilConversacion(Set.of("basquet"), true, Set.of(), Set.of(), false);

        List<DeporteSugerido> validos = recomendador.validar(
                List.of(
                        new RecomendadorDeportes.NombreYMotivo("Básquet", "es divertido"),
                        new RecomendadorDeportes.NombreYMotivo("Boxeo", "descargás"),
                        new RecomendadorDeportes.NombreYMotivo("Vóley", "social")
                ),
                conRechazo,
                catalogoCon(),
                5
        );

        assertThat(nombres(validos)).containsExactly("Vóley");
    }

    @Test
    void aceptaAliasYNoRepiteElMismoDeporteDosVeces() {
        List<DeporteSugerido> validos = recomendador.validar(
                List.of(
                        new RecomendadorDeportes.NombreYMotivo("paddle", "social"),
                        new RecomendadorDeportes.NombreYMotivo("Pádel", "otra vez"),
                        new RecomendadorDeportes.NombreYMotivo("crossfit", "intenso")
                ),
                PerfilConversacion.vacio(),
                catalogoCon(),
                5
        );

        assertThat(nombres(validos)).containsExactly("Pádel", "Cross Training");
    }

    @Test
    void usaLaExplicacionPropiaCuandoElModeloNoDaMotivo() {
        List<DeporteSugerido> validos = recomendador.validar(
                List.of(new RecomendadorDeportes.NombreYMotivo("Yoga", "   ")),
                PerfilConversacion.vacio(),
                catalogoCon(),
                5
        );

        assertThat(validos.get(0).motivo()).isNotBlank();
    }

    @Test
    void completaLaListaCuandoElModeloDevolvioMuyPoco() {
        List<DeporteSugerido> parciales = recomendador.validar(
                List.of(new RecomendadorDeportes.NombreYMotivo("Yoga", "tranquilo")),
                PerfilConversacion.vacio(),
                catalogoCon(),
                5
        );

        List<DeporteSugerido> completa = recomendador.completar(
                parciales, PerfilConversacion.vacio(), catalogoCon(), 3, 5
        );

        assertThat(completa).hasSizeGreaterThanOrEqualTo(3);
        assertThat(nombres(completa).stream().distinct().toList()).hasSameSizeAs(completa);
        assertThat(nombres(completa).get(0)).isEqualTo("Yoga");
    }

    @Test
    void validarConNadaDevuelveVacioEnVezDeInventar() {
        assertThat(recomendador.validar(List.of(), PerfilConversacion.vacio(), catalogoCon(), 5))
                .isEmpty();
        assertThat(recomendador.validar(null, PerfilConversacion.vacio(), catalogoCon(), 5))
                .isEmpty();
    }

    /* --------------------- detalle de la validación --------------------- */

    /*
      El detalle existe para el log diagnóstico de producción: cuando todo
      lo que propone el modelo se cae, hay que poder decir si fue por
      inventado, por rechazado o por repetido, sin adivinar.
    */
    @Test
    void elDetalleClasificaCadaDescartePorSuCausa() {
        PerfilConversacion conRechazo =
                new PerfilConversacion(Set.of("basquet"), false, Set.of(), Set.of(), false);

        RecomendadorDeportes.ResultadoValidacion resultado = recomendador.validarConDetalle(
                List.of(
                        new RecomendadorDeportes.NombreYMotivo("Yoga", "tranquilo"),
                        new RecomendadorDeportes.NombreYMotivo("Básquet", "en equipo"),
                        new RecomendadorDeportes.NombreYMotivo("Quidditch", "en escoba"),
                        new RecomendadorDeportes.NombreYMotivo("Yoga", "de nuevo"),
                        new RecomendadorDeportes.NombreYMotivo("   ", "sin nombre")
                ),
                conRechazo,
                catalogoCon(),
                5
        );

        assertThat(nombres(resultado.validos())).containsExactly("Yoga");
        /* El rechazado sale con su nombre canónico, no como lo tipeó el modelo. */
        assertThat(resultado.descartadosPorRechazo()).containsExactly("Básquet");
        /* El inventado sale tal cual vino: es el dato diagnóstico. */
        assertThat(resultado.descartadosPorCatalogo()).containsExactly("Quidditch");
        assertThat(resultado.duplicados()).isEqualTo(1);
        assertThat(resultado.invalidos()).isEqualTo(1);
    }

    /*
      validar() delega en el detalle: si esto falla, el refactor cambió el
      comportamiento y no solo agregó información.
    */
    @Test
    void validarDevuelveExactamenteLosValidosDelDetalle() {
        List<RecomendadorDeportes.NombreYMotivo> propuestos = List.of(
                new RecomendadorDeportes.NombreYMotivo("paddle", "social"),
                new RecomendadorDeportes.NombreYMotivo("Pádel", "otra vez"),
                new RecomendadorDeportes.NombreYMotivo("Quidditch", "inventado"),
                new RecomendadorDeportes.NombreYMotivo("crossfit", "intenso")
        );
        PerfilConversacion perfil =
                new PerfilConversacion(Set.of(), true, Set.of(), Set.of(), false);

        assertThat(recomendador.validar(propuestos, perfil, catalogoCon(), 5))
                .isEqualTo(recomendador
                        .validarConDetalle(propuestos, perfil, catalogoCon(), 5)
                        .validos());
    }

    @Test
    void elDetalleDeUnaListaVaciaNoTieneNada() {
        RecomendadorDeportes.ResultadoValidacion resultado =
                recomendador.validarConDetalle(null, PerfilConversacion.vacio(), catalogoCon(), 5);

        assertThat(resultado.validos()).isEmpty();
        assertThat(resultado.descartadosPorCatalogo()).isEmpty();
        assertThat(resultado.descartadosPorRechazo()).isEmpty();
        assertThat(resultado.duplicados()).isZero();
        assertThat(resultado.invalidos()).isZero();
    }

    /*
      Con el combate rechazado en bloque, el descarte tiene que nombrar el
      deporte que cayó: "rechazosActivos=8" solo no alcanza para saber si
      el modelo insistió con la pelea.
    */
    @Test
    void elDetalleNombraLosDeportesDeCombateQueCayeronPorElRechazoEnBloque() {
        PerfilConversacion sinPelea =
                new PerfilConversacion(Set.of(), true, Set.of(), Set.of(), false);

        RecomendadorDeportes.ResultadoValidacion resultado = recomendador.validarConDetalle(
                List.of(
                        new RecomendadorDeportes.NombreYMotivo("Boxeo", "descarga"),
                        new RecomendadorDeportes.NombreYMotivo("Muay Thai", "intenso")
                ),
                sinPelea,
                catalogoCon(),
                5
        );

        assertThat(resultado.validos()).isEmpty();
        assertThat(resultado.descartadosPorRechazo()).containsExactly("Boxeo", "Muay Thai");
    }
}
