package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.mapper.ActividadMapper;
import com.dondeentreno.api.dto.ActividadDetalleDTO;
import com.dondeentreno.api.dto.HorarioActividadDTO;
import com.dondeentreno.api.dto.ImagenDTO;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.dto.BusquedaCercaniaDTO;
import com.dondeentreno.api.dto.PaginaResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import java.util.List;

/**
 * Service de Actividad.
 *
 * Esta capa contiene la lógica relacionada con las actividades
 * deportivas publicadas en DondeEntreno.
 */
@Service
public class ActividadService {

    /**
     * Constante para no repetir el texto "PUBLICADA" en todos lados.
     *
     * Solo vamos a mostrar públicamente actividades activas
     * y con estado_publicacion = PUBLICADA.
     */
    private static final String ESTADO_PUBLICADA = "PUBLICADA";

    /**
     * Valores permitidos para los filtros que llegan desde la URL.
     *
     * Si el cliente manda un valor fuera de estas listas,
     * respondemos 400 en vez de descartar el filtro en silencio.
     */
    private static final List<String> NIVELES_PERMITIDOS =
            List.of("PRINCIPIANTE", "INTERMEDIO", "AVANZADO", "TODOS");

    private static final List<String> MODALIDADES_PERMITIDAS =
            List.of("PRESENCIAL", "ONLINE", "MIXTA");

    private static final List<String> ORDENAMIENTOS_PERMITIDOS =
            List.of("recientes", "precio_asc", "precio_desc", "titulo_asc");

    /**
     * Tope defensivo para el texto de búsqueda.
     *
     * Un texto libre desmedido no aporta a la búsqueda (los campos
     * indexados no superan este largo) y solo cargaría la query pública.
     * Se recorta en silencio en vez de rechazar, para no romper el flujo
     * si el usuario pega un texto largo.
     */
    private static final int MAX_LONGITUD_TEXTO_BUSQUEDA = 120;

    private final ActividadRepository actividadRepository;
    private final HorarioActividadService horarioActividadService;
    private final ImagenService imagenService;
    private final SocialProofService socialProofService;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente los servicios y repositories
     * necesarios y los entrega a esta clase.
     */
    public ActividadService(
            ActividadRepository actividadRepository,
            HorarioActividadService horarioActividadService,
            ImagenService imagenService,
            SocialProofService socialProofService
    ) {
        this.actividadRepository = actividadRepository;
        this.horarioActividadService = horarioActividadService;
        this.imagenService = imagenService;
        this.socialProofService = socialProofService;
    }

    /**
     * Obtiene todas las actividades activas y publicadas.
     *
     * @return lista de actividades públicas en formato DTO.
     */
    public List<ActividadDTO> obtenerActividadesPublicadas() {
        List<Actividad> actividades =
                actividadRepository.findByActivaTrueAndEstadoPublicacionOrderByCreatedAtDesc(ESTADO_PUBLICADA);

        return mapearConImagenPrincipal(actividades);
    }

    /**
     * Obtiene actividades publicadas filtradas por deporte ID.
     *
     * @param deporteId ID del deporte.
     * @return lista de actividades públicas de ese deporte.
     */
    public List<ActividadDTO> obtenerActividadesPorDeporte(Long deporteId) {
        List<Actividad> actividades =
                actividadRepository.findByActivaTrueAndEstadoPublicacionAndDeporte_IdOrderByCreatedAtDesc(
                        ESTADO_PUBLICADA,
                        deporteId
                );

        return mapearConImagenPrincipal(actividades);
    }

    /**
     * Obtiene actividades publicadas filtradas por slug de deporte.
     *
     * @param deporteSlug slug del deporte.
     * @return lista de actividades públicas de ese deporte.
     */
    public List<ActividadDTO> obtenerActividadesPorDeporteSlug(String deporteSlug) {
        List<Actividad> actividades =
                actividadRepository.findByActivaTrueAndEstadoPublicacionAndDeporte_SlugOrderByCreatedAtDesc(
                        ESTADO_PUBLICADA,
                        deporteSlug
                );

        return mapearConImagenPrincipal(actividades);
    }

    /**
     * Obtiene actividades publicadas filtradas por ciudad.
     *
     * @param ciudadId ID de la ciudad.
     * @return lista de actividades públicas de esa ciudad.
     */
    public List<ActividadDTO> obtenerActividadesPorCiudad(Long ciudadId) {
        List<Actividad> actividades =
                actividadRepository.findByActivaTrueAndEstadoPublicacionAndUbicacion_Ciudad_IdOrderByCreatedAtDesc(
                        ESTADO_PUBLICADA,
                        ciudadId
                );

        return mapearConImagenPrincipal(actividades);
    }

    /**
     * Obtiene actividades publicadas filtradas por barrio.
     *
     * @param barrioId ID del barrio.
     * @return lista de actividades públicas de ese barrio.
     */
    public List<ActividadDTO> obtenerActividadesPorBarrio(Long barrioId) {
        List<Actividad> actividades =
                actividadRepository.findByActivaTrueAndEstadoPublicacionAndUbicacion_Barrio_IdOrderByCreatedAtDesc(
                        ESTADO_PUBLICADA,
                        barrioId
                );

        return mapearConImagenPrincipal(actividades);
    }

    /**
     * Obtiene actividades publicadas filtradas por nivel.
     *
     * @param nivel nivel de la actividad.
     * @return lista de actividades públicas de ese nivel.
     */
    public List<ActividadDTO> obtenerActividadesPorNivel(String nivel) {
        List<Actividad> actividades =
                actividadRepository.findByActivaTrueAndEstadoPublicacionAndNivelOrderByCreatedAtDesc(
                        ESTADO_PUBLICADA,
                        nivel
                );

        return mapearConImagenPrincipal(actividades);
    }

    /**
     * Obtiene actividades publicadas filtradas por modalidad.
     *
     * @param modalidad modalidad de la actividad.
     * @return lista de actividades públicas de esa modalidad.
     */
    public List<ActividadDTO> obtenerActividadesPorModalidad(String modalidad) {
        List<Actividad> actividades =
                actividadRepository.findByActivaTrueAndEstadoPublicacionAndModalidadOrderByCreatedAtDesc(
                        ESTADO_PUBLICADA,
                        modalidad
                );

        return mapearConImagenPrincipal(actividades);
    }

    /**
     * Obtiene el detalle público de una actividad por su slug.
     *
     * Solo devuelve actividades activas y publicadas.
     *
     * @param slug slug único de la actividad.
     * @return DTO de la actividad encontrada.
     * @throws RecursoNoEncontradoException si no existe una actividad publicada con ese slug.
     */
    public ActividadDTO obtenerActividadPublicadaPorSlug(String slug) {
        Actividad actividad = actividadRepository
                .findBySlugAndActivaTrueAndEstadoPublicacion(slug, ESTADO_PUBLICADA)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada"));

        return mapearConImagenPrincipal(List.of(actividad)).get(0);
    }

    /**
     * Busca actividades publicadas usando filtros combinados.
     *
     * Todos los filtros son opcionales.
     * Si un filtro viene en null, no se aplica.
     * Si nivel o modalidad traen un valor no permitido,
     * se lanza FiltroInvalidoException (la API responde 400).
     *
     * @param deporteId ID del deporte.
     * @param deporteSlug slug del deporte.
     * @param ciudadId ID de la ciudad.
     * @param ciudadSlug slug de la ciudad.
     * @param barrioId ID del barrio.
     * @param perfilPublicadorId ID del perfil publicador.
     * @param nivel nivel de la actividad.
     * @param modalidad modalidad de la actividad.
     * @param texto texto libre de búsqueda.
     * @return lista de actividades publicadas en formato DTO.
     */
    public List<ActividadDTO> buscarActividadesConFiltros(
            Long deporteId,
            String deporteSlug,
            Long ciudadId,
            String ciudadSlug,
            Long barrioId,
            Long perfilPublicadorId,
            String nivel,
            String modalidad,
            String texto
    ) {
        List<Actividad> actividades =
                actividadRepository.buscarActividadesPublicadasConFiltros(
                        ESTADO_PUBLICADA,
                        deporteId,
                        limpiarTexto(deporteSlug),
                        ciudadId,
                        normalizarSlug(ciudadSlug),
                        barrioId,
                        perfilPublicadorId,
                        validarNivel(limpiarTexto(nivel)),
                        validarModalidad(limpiarTexto(modalidad)),
                        prepararTextoBusqueda(texto)
                );

        return mapearConImagenPrincipal(actividades);
    }

    /**
     * Mapea entidades a DTO y les asigna la imagen PRINCIPAL visible
     * en público (un query batch por lote, sin N+1).
     */
    /**
     * Las actividades DESTACADAS del publicador (Fase 5), en el orden
     * que él eligió. Endpoint propio y no un `orden=destacadas` del
     * buscador: el perfil las muestra ARRIBA de su listado normal, no
     * en vez de él, así que son dos consultas distintas.
     */
    public List<ActividadDTO> obtenerDestacadasDePerfil(Long perfilPublicadorId) {
        List<Actividad> actividades = actividadRepository
                .findByPerfilPublicador_IdAndDestacadaOrdenIsNotNullAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNullOrderByDestacadaOrdenAsc(
                        perfilPublicadorId,
                        ESTADO_PUBLICADA
                );

        return mapearConImagenPrincipal(actividades);
    }

    private List<ActividadDTO> mapearConImagenPrincipal(List<Actividad> actividades) {
        List<ActividadDTO> dtos = actividades.stream()
                .map(ActividadMapper::toDTO)
                .toList();

        imagenService.asignarImagenPrincipal(dtos);

        return dtos;
    }

    /**
     * Convierte textos vacíos en null.
     *
     * Esto evita que una URL como:
     * ?nivel=
     *
     * intente filtrar por un string vacío.
     *
     * @param texto texto recibido desde la URL.
     * @return texto limpio o null.
     */
    private String limpiarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        return texto;
    }

    private String normalizarSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }

        return slug.trim().toLowerCase();
    }

    /**
     * Prepara el texto de búsqueda.
     *
     * A diferencia de limpiarTexto, este metodo devuelve un string vacío
     * cuando no viene texto.
     *
     * Esto evita errores en PostgreSQL/JPA cuando el parámetro texto llega null
     * y se usa dentro de LOWER, LIKE o CONCAT en la query.
     *
     * @param texto texto recibido desde la URL.
     * @return texto limpio o string vacío.
     */
    private String prepararTextoBusqueda(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }

        String acotado = texto.length() > MAX_LONGITUD_TEXTO_BUSQUEDA
                ? texto.substring(0, MAX_LONGITUD_TEXTO_BUSQUEDA)
                : texto;

        return escaparComodinesLike(acotado);
    }

    /**
     * Escapa los comodines de LIKE en el texto del usuario para que se
     * busquen de forma literal: "50%" busca "50%", no "50" seguido de
     * cualquier cosa. Las queries usan ESCAPE '\'.
     *
     * Se escapa la barra invertida primero (es el propio carácter de
     * escape) y luego % y _. Corre DESPUÉS del recorte de longitud para
     * no cortar una secuencia de escape por la mitad.
     */
    private String escaparComodinesLike(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * Obtiene el detalle completo de una actividad por slug.
     *
     * Incluye:
     * - Datos principales de la actividad
     * - Horarios activos
     * - Imágenes activas
     *
     * @param slug slug único de la actividad.
     * @return detalle completo de la actividad.
     */
    public ActividadDetalleDTO obtenerDetalleCompletoPorSlug(String slug) {
        ActividadDTO actividad = obtenerActividadPublicadaPorSlug(slug);

        List<HorarioActividadDTO> horarios =
                horarioActividadService.obtenerHorariosPorActividadSlug(slug);

        List<ImagenDTO> imagenes =
                imagenService.obtenerImagenesPorActividadSlug(slug);

        ActividadDetalleDTO detalle = new ActividadDetalleDTO(
                actividad,
                horarios,
                imagenes
        );

        /* Señales agregadas de confianza (etapa A): aditivo al DTO. */
        detalle.setSocialProof(socialProofService.deActividad(actividad.getId()));

        return detalle;
    }

    /**
     * Busca actividades publicadas usando filtros combinados,
     * búsqueda por texto, paginación y ordenamiento.
     *
     * También valida parámetros recibidos desde la URL:
     * si nivel, modalidad u orden traen un valor no permitido,
     * se lanza FiltroInvalidoException (la API responde 400).
     *
     * @param deporteId ID del deporte.
     * @param deporteSlug slug del deporte.
     * @param ciudadId ID de la ciudad.
     * @param ciudadSlug slug de la ciudad.
     * @param barrioId ID del barrio.
     * @param perfilPublicadorId ID del perfil publicador.
     * @param nivel nivel de la actividad.
     * @param modalidad modalidad de la actividad.
     * @param texto texto libre de búsqueda.
     * @param page número de página. Arranca en 0.
     * @param size cantidad de elementos por página.
     * @param orden criterio de ordenamiento.
     * @return página de actividades publicadas.
     */
    /**
     * Modo "cerca mío" (Fase 7): las actividades publicadas ordenadas
     * por distancia al punto que mandó el usuario.
     *
     * La distancia se calcula EN MEMORIA sobre el resultado ya
     * filtrado, no en la base: con el volumen actual (7 actividades)
     * un índice geográfico o PostGIS sería ceremonia. Umbral anotado
     * para revisarlo: ~500 actividades.
     *
     * La ubicación del usuario llega por parámetro y NO se guarda en
     * ningún lado: es lo que ya promete /privacidad.
     */
    public BusquedaCercaniaDTO buscarCerca(
            double latitud,
            double longitud,
            int radioKm,
            String ciudadSlug,
            String deporteSlug,
            int limite
    ) {
        int radioSeguro = Math.min(Math.max(radioKm, 1), 50);
        int limiteSeguro = Math.min(Math.max(limite, 1), 50);

        /*
          Se traen las publicadas de la ciudad (filtro barato que ya
          existe) y recién ahí se mide: sin esto habría que recorrer
          todo el catálogo.
        */
        Page<Actividad> candidatas =
                actividadRepository.buscarActividadesPublicadasConFiltrosPaginado(
                        ESTADO_PUBLICADA,
                        null,
                        limpiarTexto(deporteSlug),
                        null,
                        normalizarSlug(ciudadSlug),
                        null,
                        null,
                        null,
                        null,
                        /*
                          "" y NO null: la condición del texto en el query
                          es `:texto = '' OR ... LIKE ...`, así que con null
                          NINGUNA fila pasa el filtro y el modo cercanía
                          devolvía siempre vacío. Por eso existe
                          prepararTextoBusqueda — su javadoc lo advierte.
                        */
                        prepararTextoBusqueda(null),
                        PageRequest.of(0, 200)
                );

        List<ActividadDTO> conDistancia = new java.util.ArrayList<>();
        long sinCoordenadas = 0;

        for (Actividad actividad : candidatas.getContent()) {
            var ubicacion = actividad.getUbicacion();
            BigDecimal lat = ubicacion != null ? ubicacion.getLatitud() : null;
            BigDecimal lng = ubicacion != null ? ubicacion.getLongitud() : null;

            double distancia = CalculadoraDistancia.kilometros(latitud, longitud, lat, lng);

            if (distancia < 0) {
                /* Sin punto cargado: no se puede ordenar, queda fuera. */
                sinCoordenadas++;
                continue;
            }

            if (distancia > radioSeguro) {
                continue;
            }

            ActividadDTO dto = ActividadMapper.toDTO(actividad);
            dto.setDistanciaKm(distancia);
            conDistancia.add(dto);
        }

        conDistancia.sort(
                java.util.Comparator.comparingDouble(ActividadDTO::getDistanciaKm)
        );

        long totalEnRadio = conDistancia.size();
        List<ActividadDTO> recortadas = conDistancia.size() > limiteSeguro
                ? conDistancia.subList(0, limiteSeguro)
                : conDistancia;

        imagenService.asignarImagenPrincipal(recortadas);

        return new BusquedaCercaniaDTO(
                recortadas,
                radioSeguro,
                sinCoordenadas,
                totalEnRadio
        );
    }

    /**
     * Zonas con actividad real (Fase 7): cuántas actividades hay por
     * barrio. Un query agrupado, sin coordenadas de por medio.
     */
    public List<com.dondeentreno.api.dto.ZonaBarrioDTO> obtenerZonasPorBarrio(
            String ciudadSlug
    ) {
        return actividadRepository
                .contarPublicadasPorBarrio(ESTADO_PUBLICADA, normalizarSlug(ciudadSlug))
                .stream()
                .filter(fila -> fila[0] != null)
                .map(fila -> new com.dondeentreno.api.dto.ZonaBarrioDTO(
                        (Long) fila[0],
                        (String) fila[1],
                        ((Number) fila[2]).longValue()
                ))
                .toList();
    }

    public PaginaResponseDTO<ActividadDTO> buscarActividadesConFiltrosPaginado(
            Long deporteId,
            String deporteSlug,
            Long ciudadId,
            String ciudadSlug,
            Long barrioId,
            Long perfilPublicadorId,
            String nivel,
            String modalidad,
            String texto,
            int page,
            int size,
            String orden
    ) {
        String nivelValidado = validarNivel(limpiarTexto(nivel));
        String modalidadValidada = validarModalidad(limpiarTexto(modalidad));

        int paginaSegura = validarPagina(page);
        int tamanioSeguro = validarTamanioPagina(size);

        Pageable pageable = PageRequest.of(
                paginaSegura,
                tamanioSeguro,
                obtenerOrdenamiento(orden)
        );

        Page<Actividad> paginaActividades =
                actividadRepository.buscarActividadesPublicadasConFiltrosPaginado(
                        ESTADO_PUBLICADA,
                        deporteId,
                        limpiarTexto(deporteSlug),
                        ciudadId,
                        normalizarSlug(ciudadSlug),
                        barrioId,
                        perfilPublicadorId,
                        nivelValidado,
                        modalidadValidada,
                        prepararTextoBusqueda(texto),
                        pageable
                );

        List<ActividadDTO> contenido =
                mapearConImagenPrincipal(paginaActividades.getContent());

        return new PaginaResponseDTO<>(
                contenido,
                paginaActividades.getNumber(),
                paginaActividades.getSize(),
                paginaActividades.getTotalElements(),
                paginaActividades.getTotalPages(),
                paginaActividades.isLast()
        );
    }

    /**
     * Define el criterio de ordenamiento para el listado de actividades.
     *
     * Valores permitidos:
     * - recientes: actividades más nuevas primero.
     * - precio_asc: precio menor a mayor.
     * - precio_desc: precio mayor a menor.
     * - titulo_asc: título alfabético.
     *
     * Si viene un valor desconocido, lanzamos FiltroInvalidoException
     * para que la API responda 400 en vez de ordenar por "recientes"
     * en silencio.
     *
     * @param orden criterio recibido desde la URL.
     * @return objeto Sort para Spring Data.
     * @throws FiltroInvalidoException si el criterio no está permitido.
     */
    private Sort obtenerOrdenamiento(String orden) {
        if (orden == null || orden.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String ordenNormalizado = orden.trim().toLowerCase();

        return switch (ordenNormalizado) {
            case "precio_asc" -> Sort.by(Sort.Direction.ASC, "precioReferencia");
            case "precio_desc" -> Sort.by(Sort.Direction.DESC, "precioReferencia");
            case "titulo_asc" -> Sort.by(Sort.Direction.ASC, "titulo");
            case "recientes" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> throw new FiltroInvalidoException(construirMensajeFiltroInvalido(
                    "orden",
                    orden,
                    ORDENAMIENTOS_PERMITIDOS
            ));
        };
    }

    /**
     * Valida el nivel recibido desde la URL.
     *
     * Si viene null, no aplica filtro.
     * Si viene un valor inválido, lanzamos FiltroInvalidoException
     * para que la API responda 400 en vez de descartar el filtro
     * en silencio y devolver resultados sin filtrar.
     *
     * @param nivel nivel recibido.
     * @return nivel válido normalizado o null.
     * @throws FiltroInvalidoException si el nivel no está permitido.
     */
    private String validarNivel(String nivel) {
        if (nivel == null) {
            return null;
        }

        String nivelNormalizado = nivel.trim().toUpperCase();

        if (!NIVELES_PERMITIDOS.contains(nivelNormalizado)) {
            throw new FiltroInvalidoException(construirMensajeFiltroInvalido(
                    "nivel",
                    nivel,
                    NIVELES_PERMITIDOS
            ));
        }

        return nivelNormalizado;
    }

    /**
     * Valida la modalidad recibida desde la URL.
     *
     * Si viene null, no aplica filtro.
     * Si viene un valor inválido, lanzamos FiltroInvalidoException
     * para que la API responda 400 en vez de descartar el filtro
     * en silencio y devolver resultados sin filtrar.
     *
     * @param modalidad modalidad recibida.
     * @return modalidad válida normalizada o null.
     * @throws FiltroInvalidoException si la modalidad no está permitida.
     */
    private String validarModalidad(String modalidad) {
        if (modalidad == null) {
            return null;
        }

        String modalidadNormalizada = modalidad.trim().toUpperCase();

        if (!MODALIDADES_PERMITIDAS.contains(modalidadNormalizada)) {
            throw new FiltroInvalidoException(construirMensajeFiltroInvalido(
                    "modalidad",
                    modalidad,
                    MODALIDADES_PERMITIDAS
            ));
        }

        return modalidadNormalizada;
    }

    /**
     * Arma el mensaje de error para un filtro inválido.
     *
     * Incluye el nombre del parámetro, el valor recibido
     * y la lista de valores permitidos, para que el cliente
     * sepa exactamente qué corregir.
     *
     * @param parametro nombre del parámetro de la URL.
     * @param valorRecibido valor inválido recibido.
     * @param valoresPermitidos valores aceptados para ese parámetro.
     * @return mensaje descriptivo del error.
     */
    private String construirMensajeFiltroInvalido(
            String parametro,
            String valorRecibido,
            List<String> valoresPermitidos
    ) {
        return "El parametro '" + parametro + "' tiene un valor invalido: '"
                + valorRecibido + "'. Valores permitidos: "
                + String.join(", ", valoresPermitidos) + ".";
    }

    /**
     * Valida el número de página.
     *
     * Si viene negativo, usamos página 0.
     *
     * @param page número de página recibido.
     * @return página segura.
     */
    private int validarPagina(int page) {
        return Math.max(page, 0);
    }

    /**
     * Valida el tamaño de página.
     *
     * Mínimo permitido: 1
     * Máximo permitido: 50
     *
     * @param size tamaño de página recibido.
     * @return tamaño seguro.
     */
    private int validarTamanioPagina(int size) {
        return Math.min(Math.max(size, 1), 50);
    }
}
