package com.dondeentreno.api.service;

import com.dondeentreno.api.asistente.AnalizadorConversacion;
import com.dondeentreno.api.asistente.AsistenteProperties;
import com.dondeentreno.api.asistente.LimitadorConsultas;
import com.dondeentreno.api.asistente.MotorAsistenteRemoto;
import com.dondeentreno.api.asistente.RecomendadorDeportes;
import com.dondeentreno.api.asistente.RedactorRespuesta;
import com.dondeentreno.api.asistente.ResolutorConsulta;
import com.dondeentreno.api.asistente.RespuestaModelo;
import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.dto.AsistenteEnlaceDTO;
import com.dondeentreno.api.dto.AsistenteMensajeDTO;
import com.dondeentreno.api.dto.AsistenteRespuestaDTO;
import com.dondeentreno.api.dto.BarrioDTO;
import com.dondeentreno.api.dto.CategoriaDeportivaDTO;
import com.dondeentreno.api.dto.CiudadDTO;
import com.dondeentreno.api.dto.DeporteDTO;
import com.dondeentreno.api.dto.FiltroOpcionesDTO;
import com.dondeentreno.api.exception.ConsultaAsistenteInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsistenteServiceTest {

    private FiltroService filtroService;
    private ActividadService actividadService;
    private MotorAsistenteRemoto motorRemoto;
    private LimitadorConsultas limitador;
    private AsistenteService asistenteService;

    @BeforeEach
    void prepararService() {
        filtroService = mock(FiltroService.class);
        actividadService = mock(ActividadService.class);
        motorRemoto = mock(MotorAsistenteRemoto.class);
        limitador = mock(LimitadorConsultas.class);

        asistenteService = construir(new AsistenteProperties());

        when(filtroService.obtenerOpcionesDeFiltros()).thenReturn(catalogo());
        /* Por defecto, como en produccion hasta encenderlo: Gemini apagado. */
        when(motorRemoto.estaDisponible()).thenReturn(false);
    }

    private AsistenteService construir(AsistenteProperties propiedades) {
        return new AsistenteService(
                filtroService,
                actividadService,
                new ResolutorConsulta(),
                propiedades,
                motorRemoto,
                limitador,
                new AnalizadorConversacion(),
                new RecomendadorDeportes(),
                new RedactorRespuesta()
        );
    }

    private void conModeloDisponible(RespuestaModelo respuesta) {
        when(motorRemoto.estaDisponible()).thenReturn(true);
        when(limitador.consumirCuotaGemini()).thenReturn(true);
        when(motorRemoto.conversar(any())).thenReturn(Optional.ofNullable(respuesta));
    }

    private FiltroOpcionesDTO catalogo() {
        CategoriaDeportivaDTO artesMarciales = new CategoriaDeportivaDTO();
        artesMarciales.setId(2L);
        artesMarciales.setNombre("Artes marciales");
        artesMarciales.setSlug("artes-marciales");

        CiudadDTO marDelPlata = new CiudadDTO();
        marDelPlata.setId(1L);
        marDelPlata.setNombre("Mar del Plata");
        marDelPlata.setSlug("mar-del-plata");

        return new FiltroOpcionesDTO(
                List.of(artesMarciales),
                List.of(
                        deporte(1L, "Yoga", "yoga", 6L, "Bienestar y salud", "bienestar-y-salud"),
                        deporte(2L, "Funcional", "funcional", 3L, "Fitness", "fitness-y-entrenamiento"),
                        deporte(3L, "Básquet", "basquet", 4L, "Deportes de equipo", "deportes-de-equipo"),
                        deporte(4L, "Natación", "natacion", 5L, "Acuáticas", "actividades-acuaticas"),
                        deporte(5L, "Boxeo", "boxeo", 1L, "Deportes de combate", "deportes-de-combate")
                ),
                List.of(marDelPlata),
                List.of(new BarrioDTO(7L, "Constitución", 1L, "Mar del Plata")),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private DeporteDTO deporte(
            Long id, String nombre, String slug,
            Long categoriaId, String categoriaNombre, String categoriaSlug
    ) {
        return new DeporteDTO(
                id, nombre, slug, null, null, id.intValue(),
                categoriaId, categoriaNombre, categoriaSlug
        );
    }

    private ActividadDTO actividad(String titulo) {
        ActividadDTO actividad = new ActividadDTO();
        actividad.setTitulo(titulo);
        return actividad;
    }

    private ActividadDTO actividad(String titulo, String deporteSlug) {
        ActividadDTO actividad = actividad(titulo);
        actividad.setDeporteSlug(deporteSlug);
        return actividad;
    }

    /** Lo que devuelve la busqueda "todo lo publicado" que arma el catalogo. */
    private void conActividadesPublicadas(ActividadDTO... actividades) {
        when(actividadService.buscarActividadesConFiltros(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of(actividades));
    }

    private List<String> hrefs(AsistenteRespuestaDTO respuesta) {
        return respuesta.getEnlaces().stream().map(AsistenteEnlaceDTO::getHref).toList();
    }

    private List<AsistenteMensajeDTO> charla(String... alternados) {
        return java.util.stream.IntStream.range(0, alternados.length)
                .mapToObj(indice -> new AsistenteMensajeDTO(
                        indice % 2 == 0 ? "usuario" : "asistente",
                        alternados[indice]
                ))
                .toList();
    }

    /* =============================================================
       Camino de busqueda: la persona nombro algo concreto.
       ============================================================= */

    @Test
    void informaElTotalRealYEnlazaConLosFiltrosEntendidos() {
        when(actividadService.buscarActividadesConFiltros(
                isNull(), eq("yoga"), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of(actividad("Yoga inicial"), actividad("Yoga en el parque")));

        AsistenteRespuestaDTO respuesta = asistenteService.responder("busco yoga");

        assertThat(respuesta.getTexto()).contains("2 actividades");
        assertThat(respuesta.getTexto()).contains("Yoga inicial");
        assertThat(hrefs(respuesta)).containsExactly("/explorar?deporteSlug=yoga&page=0");
        assertThat(respuesta.getFuente()).isEqualTo("local");
    }

    /* Una respuesta que ya cierra no lleva botones de charla encima. */
    @Test
    void unaBusquedaConResultadosNoAgregaOpcionesRapidas() {
        when(actividadService.buscarActividadesConFiltros(
                isNull(), eq("yoga"), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of(actividad("Yoga inicial")));

        assertThat(asistenteService.responder("busco yoga").getOpcionesRapidas()).isEmpty();
    }

    @Test
    void cuandoNoHayNadaEnElBarrioAmpliaLaZonaYLoDice() {
        when(actividadService.buscarActividadesConFiltros(
                isNull(), eq("yoga"), isNull(), isNull(), eq(7L),
                isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of());

        when(actividadService.buscarActividadesConFiltros(
                isNull(), eq("yoga"), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of(actividad("Yoga inicial")));

        AsistenteRespuestaDTO respuesta = asistenteService.responder("yoga en Constitucion");

        assertThat(respuesta.getTexto()).contains("Constitución");
        assertThat(respuesta.getTexto()).contains("no encontré");
        assertThat(respuesta.getTexto()).contains("1 actividad");
        /* El enlace ampliado ya no lleva el barrio que no tenia nada. */
        assertThat(hrefs(respuesta)).containsExactly("/explorar?deporteSlug=yoga&page=0");
    }

    /*
      V1 respondia "no hay" y cerraba. Ahora aprovecha para ofrecer algo
      que si exista, pero sin dejar de decir la verdad sobre lo buscado.
    */
    @Test
    void siNoHayDelDeporteBuscadoOfreceAlternativasQueSiTenganActividades() {
        conActividadesPublicadas(actividad("Funcional en la playa", "funcional"));

        when(actividadService.buscarActividadesConFiltros(
                isNull(), eq("yoga"), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of());

        AsistenteRespuestaDTO respuesta = asistenteService.responder("busco yoga");

        assertThat(respuesta.getTexto()).contains("Todavía no hay actividades");
        assertThat(respuesta.getTexto()).contains("Funcional");
        assertThat(hrefs(respuesta)).contains("/explorar?deporteSlug=funcional&page=0");
    }

    @Test
    void siNoHayNadaPublicadoLoDiceSinInventar() {
        when(actividadService.buscarActividadesConFiltros(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(List.of());

        AsistenteRespuestaDTO respuesta = asistenteService.responder("busco yoga");

        assertThat(respuesta.getTexto()).contains("Todavía no hay actividades");
        assertThat(hrefs(respuesta)).containsExactly("/deportes", "/explorar");
    }

    @Test
    void unaCategoriaSolaLlevaAlCatalogoDeEsaCategoriaSinInventarConteo() {
        AsistenteRespuestaDTO respuesta = asistenteService.responder("me interesan las artes marciales");

        assertThat(hrefs(respuesta)).containsExactly("/deportes?categoria=artes-marciales");
        assertThat(respuesta.getTexto()).doesNotContain("actividades de");
    }

    /* =============================================================
       Memoria: lo que la persona rechaza no vuelve.
       ============================================================= */

    /*
      El peor bug del asistente V1, y la razon de ser del bloque: "no
      quiero basquet" tiene la palabra "basquet" adentro, asi que el
      resolutor la encontraba y el asistente contestaba con actividades de
      basquet. Un rechazo leido como pedido.
    */
    @Test
    void unRechazoNoSeLeeComoUnaBusquedaDeEseDeporte() {
        AsistenteRespuestaDTO respuesta = asistenteService.responder("no quiero básquet");

        assertThat(respuesta.getTexto()).doesNotContain("Básquet");
        verify(actividadService, never()).buscarActividadesConFiltros(
                any(), eq("basquet"), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void noVuelveARecomendarUnDeporteRechazadoEnUnTurnoAnterior() {
        AsistenteRespuestaDTO respuesta = asistenteService.responder(
                "y algo social?",
                charla(
                        "algún deporte que recomiendes?",
                        "Te tiro algunas: 1. Básquet 2. Funcional 3. Natación",
                        "no quiero básquet"
                )
        );

        assertThat(respuesta.getTexto()).doesNotContain("Básquet");
    }

    /* Un rechazo de grupo saca todo el grupo, no solo lo nombrado. */
    @Test
    void rechazarLaPeleaSacaTodosLosDeportesDeCombate() {
        AsistenteRespuestaDTO respuesta =
                asistenteService.responder("no me gustan los deportes de pelea");

        assertThat(respuesta.getTexto())
                .doesNotContain("Boxeo")
                .doesNotContain("Kickboxing")
                .doesNotContain("Muay Thai")
                .doesNotContain("Karate")
                .doesNotContain("Jiu Jitsu")
                .doesNotContain("MMA");
        assertThat(respuesta.getTexto()).contains("saco todo lo que sea contacto o pelea");
    }

    @Test
    void noRepiteLaMismaListaDeDeportesTurnoATurno() {
        AsistenteRespuestaDTO primera = asistenteService.responder("algún deporte que recomiendes?");

        AsistenteRespuestaDTO segunda = asistenteService.responder(
                "dame otras opciones",
                charla("algún deporte que recomiendes?", primera.getTexto())
        );

        assertThat(segunda.getTexto()).isNotEqualTo(primera.getTexto());
    }

    /* =============================================================
       Consejo general contra actividades reales.
       ============================================================= */

    @Test
    void recomiendaDeportesQueNoEstanEnDondeEntrenoYLoAclara() {
        AsistenteRespuestaDTO respuesta = asistenteService.responder("quiero algo social");

        /* Padel no esta en el catalogo de este test y aun asi se recomienda. */
        assertThat(respuesta.getTexto()).contains("Pádel");
        assertThat(respuesta.getTexto()).contains("recomendación general");
    }

    @Test
    void separaLoQueSiHayPublicadoDeLoQueEsSoloConsejo() {
        conActividadesPublicadas(
                actividad("Funcional en la playa", "funcional"),
                actividad("Funcional matutino", "funcional")
        );

        AsistenteRespuestaDTO respuesta = asistenteService.responder("quiero algo social");

        assertThat(respuesta.getTexto()).contains("En DondeEntreno ya hay actividades de Funcional");
        assertThat(respuesta.getTexto()).contains("recomendación general");
        assertThat(hrefs(respuesta)).containsExactly("/explorar?deporteSlug=funcional&page=0");
    }

    /* Sin actividades publicadas no se ofrece un enlace a una busqueda vacia. */
    @Test
    void noEnlazaDeportesSinActividadesPublicadas() {
        AsistenteRespuestaDTO respuesta = asistenteService.responder("quiero algo tranqui");

        assertThat(hrefs(respuesta)).containsExactly("/explorar", "/deportes");
    }

    @Test
    void ofreceOpcionesProgresivasCuandoDiceQueSeCansaRapido() {
        AsistenteRespuestaDTO respuesta = asistenteService.responder("me canso rápido");

        assertThat(respuesta.getTexto()).contains("de a poco");
        /* Nada de alto impacto en la primera recomendacion. */
        assertThat(respuesta.getTexto()).doesNotContain("Running");
        assertThat(respuesta.getTexto()).doesNotContain("Cross Training");
    }

    @Test
    void anteUnTemaDeSaludDerivaAUnProfesionalYBajaElImpacto() {
        AsistenteRespuestaDTO respuesta =
                asistenteService.responder("me duele la rodilla, qué puedo hacer?");

        assertThat(respuesta.getTexto()).contains("profesional de la salud");
        assertThat(respuesta.getTexto()).doesNotContain("Running");
        assertThat(respuesta.getTexto()).doesNotContain("Boxeo");
        /*
          Y no se le encima el arranque genérico: el párrafo de derivación ya
          hace de apertura y cierra presentando la lista.
        */
        assertThat(respuesta.getTexto()).doesNotContain("Depende de qué estés buscando");
    }

    /* "Social" no puede traer deportes de combate entre los primeros. */
    @Test
    void unaConsultaSocialNoDevuelveDeportesDeCombate() {
        conActividadesPublicadas(actividad("Jiu Jitsu para todos", "jiu-jitsu"));

        AsistenteRespuestaDTO respuesta =
                asistenteService.responder("quiero algo social y que varíe, me aburro con el gym");

        assertThat(respuesta.getTexto())
                .doesNotContain("Jiu Jitsu")
                .doesNotContain("Boxeo")
                .doesNotContain("Musculación");
    }

    @Test
    void admiteQueNoEntendioCuandoNoHayNiSenalesNiPedido() {
        AsistenteRespuestaDTO respuesta = asistenteService.responder("asdkjh qwe zxc");

        assertThat(respuesta.getTexto()).contains("no la tengo del todo clara");
        assertThat(hrefs(respuesta)).containsExactly("/explorar", "/deportes");
    }

    /* =============================================================
       El modelo: entra donde el motor local no llega, y no manda.
       ============================================================= */

    @Test
    void conElModeloApagadoNiSiquieraSeLeConsulta() {
        asistenteService.responder("quiero algo social");

        verify(motorRemoto, never()).conversar(any());
    }

    @Test
    void usaAlModeloSoloCuandoElMotorLocalNoEntendio() {
        conModeloDisponible(modelo("Te tiro estas", "Funcional", "Natación"));
        when(actividadService.buscarActividadesConFiltros(
                isNull(), eq("yoga"), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of(actividad("Yoga inicial")));

        /* Esta la entiende el motor local: el modelo no deberia tocarse. */
        assertThat(asistenteService.responder("busco yoga").getFuente()).isEqualTo("local");
        verify(motorRemoto, never()).conversar(any());

        /* Esta no: aca si entra. */
        AsistenteRespuestaDTO remota = asistenteService.responder("tengo 50 años y quiero moverme un poco");
        assertThat(remota.getFuente()).isEqualTo("gemini");
        assertThat(remota.getTexto()).contains("Te tiro estas");
        assertThat(remota.getTexto()).contains("Funcional");
    }

    /* El candado: lo que el modelo invente no existe y se descarta solo. */
    @Test
    void descartaLosDeportesQueElModeloInventa() {
        conModeloDisponible(modelo("Mirá estas opciones", "Quidditch", "Parkour lunar"));

        AsistenteRespuestaDTO respuesta =
                asistenteService.responder("recomendame algo, quiero algo social");

        assertThat(respuesta.getTexto())
                .doesNotContain("Quidditch")
                .doesNotContain("Parkour");
        /*
          Los deportes en pantalla son nuestros. La prosa del modelo se
          conserva (por eso la fuente es gemini y no local, como era
          antes de aceptar el consejo puro): descartar lo inventado no
          obliga a descartar el saludo.
        */
        assertThat(respuesta.getFuente()).isEqualTo("gemini");
        assertThat(respuesta.getTexto()).contains("Mirá estas opciones");
        assertThat(respuesta.getTexto()).contains("Pádel");
    }

    /*
      El modelo no puede insistir con lo rechazado ni aunque lo devuelva:
      el filtro esta en codigo, no en el prompt.
    */
    @Test
    void filtraLosDeportesRechazadosAunqueElModeloLosProponga() {
        conModeloDisponible(modelo("Probá con esto", "Básquet", "Funcional", "Natación"));

        AsistenteRespuestaDTO respuesta = asistenteService.responder(
                "algo más",
                charla("recomendame algo", "Te tiro Básquet y Funcional", "no quiero básquet")
        );

        assertThat(respuesta.getTexto()).doesNotContain("Básquet");
        assertThat(respuesta.getTexto()).contains("Funcional");
    }

    /* Lo que el modelo escriba se limpia antes de llegar a la pantalla. */
    @Test
    void leSacaEnlacesPreciosYTelefonosAlTextoDelModelo() {
        conModeloDisponible(new RespuestaModelo(
                "consejo_deportivo",
                "Mirá esto en https://otro-sitio.com. Sale 15000 pesos por mes. Escribime al 2235123456.",
                List.of(new RespuestaModelo.DeportePropuesto("Funcional", "circuitos variados")),
                null,
                "¿Te sirve?"
        ));

        AsistenteRespuestaDTO respuesta =
                asistenteService.responder("recomendame algo copado para arrancar");

        assertThat(respuesta.getTexto())
                .doesNotContain("http")
                .doesNotContain("otro-sitio")
                .doesNotContain("15000")
                .doesNotContain("2235123456");
        assertThat(respuesta.getTexto()).contains("Funcional");
    }

    @Test
    void elModeloRecibeLoRechazadoYElHistorialRecortado() {
        conModeloDisponible(modelo("Va", "Funcional"));

        AsistenteProperties propiedades = new AsistenteProperties();
        propiedades.setMaxMensajesHistorial(2);

        construir(propiedades).responder(
                "otra idea?",
                charla("hola", "hola!", "no quiero básquet", "listo", "algo social")
        );

        var captor = forClass(com.dondeentreno.api.asistente.ConsultaRemota.class);
        verify(motorRemoto).conversar(captor.capture());

        assertThat(captor.getValue().rechazados()).contains("Básquet");
        assertThat(captor.getValue().historial()).hasSize(2);
        assertThat(captor.getValue().vocabulario()).doesNotContain("Básquet");
    }

    @Test
    void sinCuotaDiariaNoLlamaAlModelo() {
        when(motorRemoto.estaDisponible()).thenReturn(true);
        when(limitador.consumirCuotaGemini()).thenReturn(false);

        AsistenteRespuestaDTO respuesta = asistenteService.responder("quiero algo social");

        verify(motorRemoto, never()).conversar(any());
        assertThat(respuesta.getFuente()).isEqualTo("local");
    }

    @Test
    void siElModeloFallaElAsistenteRespondeIgual() {
        conModeloDisponible(null);

        AsistenteRespuestaDTO respuesta = asistenteService.responder("quiero algo social y variado");

        assertThat(respuesta.getFuente()).isEqualTo("local");
        assertThat(respuesta.getTexto()).contains("Pádel");
    }

    /*
      El caso que dejó el asistente en modo local una semana: el modelo
      contesta consejo válido pero con "deportes" vacío. Su prosa se
      acepta y los deportes los pone el recomendador determinístico.
    */
    @Test
    void aceptaElConsejoDelModeloAunqueVengaSinDeportes() {
        conModeloDisponible(new RespuestaModelo(
                "consejo_deportivo",
                "Buenísimo que quieras arrancar, te tiro ideas para elegir.",
                List.of(),
                null,
                "¿Cuál te tienta?"
        ));

        AsistenteRespuestaDTO respuesta =
                asistenteService.responder("quiero arrancar algo, ayudame a elegir");

        assertThat(respuesta.getFuente()).isEqualTo("gemini");
        assertThat(respuesta.getTexto()).contains("Buenísimo que quieras arrancar");
        /* Los deportes son nuestros, no un texto sin opciones. */
        assertThat(respuesta.getTexto()).contains("Pádel");
    }

    /*
      Con el extractor, la enumeración del mensaje ES la propuesta del
      modelo: si todo lo que enumeró estaba rechazado, corre el mismo
      candado que el campo "deportes" (test siguiente) y se descarta
      completo. Antes este caso recortaba la enumeración y aceptaba la
      apertura como consejo puro; eso mostraba con tono amable a un
      modelo que venía insistiendo con lo rechazado.
    */
    @Test
    void siLaEnumeracionSoloTraiaLoRechazadoSeDescartaComoSiFueraElCampo() {
        conModeloDisponible(new RespuestaModelo(
                "consejo_deportivo",
                "Te tiro opciones: 1. Básquet: picado y aros 2. Boxeo: bolsa y guantes",
                List.of(),
                null,
                null
        ));

        AsistenteRespuestaDTO respuesta = asistenteService.responder(
                "y entonces qué hago?",
                charla("recomendame algo", "ok", "no quiero básquet ni nada de pelea")
        );

        assertThat(respuesta.getFuente()).isEqualTo("local");
        /* Nada de la prosa del modelo sobrevive al descarte. */
        assertThat(respuesta.getTexto()).doesNotContain("picado y aros");
        assertThat(respuesta.getTexto()).doesNotContain("Te tiro opciones");
        assertThat(respuesta.getTexto()).doesNotContain("Básquet");
        assertThat(respuesta.getTexto()).doesNotContain("Boxeo");
    }

    /*
      El hábito real del modelo en producción (visto el 2026-08-18):
      campo "deportes" vacío y la lista entera como enumeración adentro
      del mensaje. Su elección no se tira: se extrae y pasa por el mismo
      validar() que el campo, y la lista final la re-arma el backend con
      la apertura del modelo sin su enumeración.
    */
    @Test
    void extraeLosDeportesQueElModeloEnumeroDentroDelMensaje() {
        conModeloDisponible(new RespuestaModelo(
                "consejo_deportivo",
                "Para arrancar tranqui: 1. Yoga: para soltar el cuerpo 2. Natación: bajo impacto",
                List.of(),
                null,
                null
        ));

        AsistenteRespuestaDTO respuesta = asistenteService.responder(
                "y entonces qué hago?",
                charla("quiero moverme un poco", "ok")
        );

        assertThat(respuesta.getFuente()).isEqualTo("gemini");
        /* La elección del modelo, honrada. */
        assertThat(respuesta.getTexto()).contains("Yoga");
        assertThat(respuesta.getTexto()).contains("Natación");
        /* Su apertura queda; la lista la enumera el backend. */
        assertThat(respuesta.getTexto()).contains("Para arrancar tranqui");
    }

    /*
      Extraer de la prosa no relaja ninguna garantía: lo rechazado se cae
      igual que si hubiera venido en el campo, y lo válido sobrevive.
    */
    @Test
    void deLaEnumeracionSoloSobreviveLoQueNoEstabaRechazado() {
        conModeloDisponible(new RespuestaModelo(
                "consejo_deportivo",
                "Mirá: 1. Boxeo: para descargar 2. Yoga: para aflojar",
                List.of(),
                null,
                null
        ));

        AsistenteRespuestaDTO respuesta = asistenteService.responder(
                "y entonces qué hago?",
                charla("recomendame algo", "ok", "nada de pelea")
        );

        assertThat(respuesta.getFuente()).isEqualTo("gemini");
        assertThat(respuesta.getTexto()).contains("Yoga");
        assertThat(respuesta.getTexto()).doesNotContain("Boxeo");
    }

    /* La lista pura con viñetas y sin apertura también es una elección. */
    @Test
    void extraeLaListaPuraConVinetasDesdeElPrimerCaracter() {
        conModeloDisponible(new RespuestaModelo(
                "consejo_deportivo",
                "• Yoga: para el estrés • Funcional: circuitos cortos",
                List.of(),
                null,
                null
        ));

        AsistenteRespuestaDTO respuesta = asistenteService.responder(
                "y entonces qué hago?",
                charla("quiero moverme un poco", "ok")
        );

        assertThat(respuesta.getFuente()).isEqualTo("gemini");
        assertThat(respuesta.getTexto()).contains("Yoga");
        assertThat(respuesta.getTexto()).contains("Funcional");
    }

    /*
      Una enumeración sin deportes reales ("dormí bien") no matchea nada
      del catálogo: no hay elección que honrar y el caso degrada al
      consejo puro de siempre — apertura del modelo + deportes del
      recomendador determinístico.
    */
    @Test
    void unaEnumeracionSinDeportesRealesDegradaAlConsejoPuro() {
        conModeloDisponible(new RespuestaModelo(
                "consejo_deportivo",
                "Arranquemos por lo básico: 1. Dormí bien 2. Tomá agua",
                List.of(),
                null,
                null
        ));

        AsistenteRespuestaDTO respuesta = asistenteService.responder(
                "y entonces qué hago?",
                charla("quiero arrancar algo", "ok")
        );

        assertThat(respuesta.getFuente()).isEqualTo("gemini");
        assertThat(respuesta.getTexto()).contains("Arranquemos por lo básico");
        assertThat(respuesta.getTexto()).doesNotContain("Dormí bien");
    }

    /*
      Si TODO lo que propuso estaba rechazado, su prosa tampoco se
      muestra: es justamente la más propensa a elogiar lo rechazado, y el
      sanitizador no filtra nombres de deportes. Se conserva el descarte
      completo que había antes del consejo puro.
    */
    @Test
    void siTodoLoPropuestoEstabaRechazadoLaProsaTampocoSeMuestra() {
        conModeloDisponible(modelo(
                "El boxeo te va a encantar para descargar la bronca",
                "Boxeo", "Muay Thai"
        ));

        AsistenteRespuestaDTO respuesta = asistenteService.responder(
                "y entonces qué hago?",
                charla("recomendame algo", "ok", "nada de pelea ni combate")
        );

        assertThat(respuesta.getFuente()).isEqualTo("local");
        assertThat(respuesta.getTexto()).doesNotContain("te va a encantar");
        assertThat(respuesta.getTexto()).doesNotContain("Boxeo");
    }

    /*
      El hábito de repetir la lista adentro del mensaje no distingue
      caminos: también con deportes válidos la apertura se recorta, o la
      pantalla mostraba la lista dos veces.
    */
    @Test
    void tambienConDeportesValidosLaAperturaSeQuedaSinLaEnumeracion() {
        conModeloDisponible(modelo(
                "Te tiro ideas: 1. Funcional: circuitos 2. Vóley: en grupo",
                "Funcional", "Vóley"
        ));

        AsistenteRespuestaDTO respuesta =
                asistenteService.responder("recomendame algo, quiero algo social");

        assertThat(respuesta.getFuente()).isEqualTo("gemini");
        assertThat(respuesta.getTexto()).contains("Te tiro ideas");
        /* La enumeración embebida no pasa; la lista la arma el backend. */
        assertThat(respuesta.getTexto()).doesNotContain("1. Funcional: circuitos");
    }

    /* La variante rioplatense "1- " también es una enumeración. */
    @Test
    void laEnumeracionConGuionTambienSeRecorta() {
        conModeloDisponible(new RespuestaModelo(
                "consejo_deportivo",
                "Mirá estas: 1- Funcional para variar 2- Vóley en grupo",
                List.of(),
                null,
                null
        ));

        AsistenteRespuestaDTO respuesta =
                asistenteService.responder("quiero algo social y variado");

        assertThat(respuesta.getFuente()).isEqualTo("gemini");
        assertThat(respuesta.getTexto()).contains("Mirá estas");
        assertThat(respuesta.getTexto()).doesNotContain("para variar");
    }

    /* Consejo vacío y sin deportes: recién ahí decide el motor local. */
    @Test
    void sinProsaNiDeportesElModeloNoAportaNadaYDecideElLocal() {
        conModeloDisponible(new RespuestaModelo(
                "consejo_deportivo",
                "   ",
                List.of(),
                null,
                "¿seguimos?"
        ));

        AsistenteRespuestaDTO respuesta = asistenteService.responder("quiero algo social");

        assertThat(respuesta.getFuente()).isEqualTo("local");
        assertThat(respuesta.getTexto()).isNotBlank();
    }

    /* =============================================================
       Validaciones de entrada.
       ============================================================= */

    @Test
    void rechazaConsultaVacia() {
        assertThatThrownBy(() -> asistenteService.responder("   "))
                .isInstanceOf(ConsultaAsistenteInvalidaException.class);
    }

    @Test
    void rechazaConsultaMasLargaQueElMaximoConfigurado() {
        AsistenteProperties propiedades = new AsistenteProperties();
        propiedades.setMaxInputChars(10);

        assertThatThrownBy(() -> construir(propiedades)
                .responder("un mensaje bastante mas largo que diez"))
                .isInstanceOf(ConsultaAsistenteInvalidaException.class)
                .hasMessageContaining("10");
    }

    @Test
    void unHistorialConBasuraNoRompeNada() {
        List<AsistenteMensajeDTO> sucio = new java.util.ArrayList<>();
        sucio.add(null);
        sucio.add(new AsistenteMensajeDTO(null, "sin autor"));
        sucio.add(new AsistenteMensajeDTO("usuario", "   "));
        sucio.add(new AsistenteMensajeDTO("sistema", "no soy de los dos"));

        AsistenteRespuestaDTO respuesta = asistenteService.responder("quiero algo social", sucio);

        assertThat(respuesta.getTexto()).isNotBlank();
    }

    private RespuestaModelo modelo(String mensaje, String... deportes) {
        return new RespuestaModelo(
                "consejo_deportivo",
                mensaje,
                java.util.Arrays.stream(deportes)
                        .map(nombre -> new RespuestaModelo.DeportePropuesto(nombre, "motivo de " + nombre))
                        .toList(),
                null,
                "¿Seguimos afinando?"
        );
    }
}
