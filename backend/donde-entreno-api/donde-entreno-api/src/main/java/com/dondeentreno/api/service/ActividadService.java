package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.mapper.ActividadMapper;
import com.dondeentreno.api.dto.ActividadDetalleDTO;
import com.dondeentreno.api.dto.HorarioActividadDTO;
import com.dondeentreno.api.dto.ImagenDTO;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.dto.PaginaResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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

    private final ActividadRepository actividadRepository;
    private final HorarioActividadService horarioActividadService;
    private final ImagenService imagenService;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente los servicios y repositories
     * necesarios y los entrega a esta clase.
     */
    public ActividadService(
            ActividadRepository actividadRepository,
            HorarioActividadService horarioActividadService,
            ImagenService imagenService
    ) {
        this.actividadRepository = actividadRepository;
        this.horarioActividadService = horarioActividadService;
        this.imagenService = imagenService;
    }

    /**
     * Obtiene todas las actividades activas y publicadas.
     *
     * @return lista de actividades públicas en formato DTO.
     */
    public List<ActividadDTO> obtenerActividadesPublicadas() {
        List<Actividad> actividades =
                actividadRepository.findByActivaTrueAndEstadoPublicacionOrderByCreatedAtDesc(ESTADO_PUBLICADA);

        return actividades.stream()
                .map(ActividadMapper::toDTO)
                .toList();
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

        return actividades.stream()
                .map(ActividadMapper::toDTO)
                .toList();
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

        return actividades.stream()
                .map(ActividadMapper::toDTO)
                .toList();
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

        return actividades.stream()
                .map(ActividadMapper::toDTO)
                .toList();
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

        return actividades.stream()
                .map(ActividadMapper::toDTO)
                .toList();
    }

    /**
     * Obtiene actividades publicadas filtradas por perfil publicador.
     *
     * @param perfilPublicadorId ID del perfil publicador.
     * @return lista de actividades públicas de ese perfil.
     */
    public List<ActividadDTO> obtenerActividadesPorPerfilPublicador(Long perfilPublicadorId) {
        List<Actividad> actividades =
                actividadRepository.findByActivaTrueAndEstadoPublicacionAndPerfilPublicador_IdOrderByCreatedAtDesc(
                        ESTADO_PUBLICADA,
                        perfilPublicadorId
                );

        return actividades.stream()
                .map(ActividadMapper::toDTO)
                .toList();
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

        return actividades.stream()
                .map(ActividadMapper::toDTO)
                .toList();
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

        return actividades.stream()
                .map(ActividadMapper::toDTO)
                .toList();
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

        return ActividadMapper.toDTO(actividad);
    }

    /**
     * Busca actividades publicadas usando filtros combinados.
     *
     * Todos los filtros son opcionales.
     * Si un filtro viene en null, no se aplica.
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
                        limpiarTexto(nivel),
                        limpiarTexto(modalidad),
                        prepararTextoBusqueda(texto)
                );

        return actividades.stream()
                .map(ActividadMapper::toDTO)
                .toList();
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

        return texto;
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

        return new ActividadDetalleDTO(
                actividad,
                horarios,
                imagenes
        );
    }

    /**
     * Busca actividades publicadas usando filtros combinados,
     * búsqueda por texto, paginación y ordenamiento.
     *
     * También valida parámetros para evitar valores inválidos
     * recibidos desde la URL.
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

        List<ActividadDTO> contenido = paginaActividades.getContent()
                .stream()
                .map(ActividadMapper::toDTO)
                .toList();

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
     * Si viene un valor desconocido, usamos "recientes".
     *
     * @param orden criterio recibido desde la URL.
     * @return objeto Sort para Spring Data.
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
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    /**
     * Valida el nivel recibido desde la URL.
     *
     * Si viene null, no aplica filtro.
     * Si viene un valor inválido, tampoco aplica filtro.
     *
     * Más adelante podríamos devolver error 400,
     * pero por ahora elegimos no romper la búsqueda.
     *
     * @param nivel nivel recibido.
     * @return nivel válido o null.
     */
    private String validarNivel(String nivel) {
        if (nivel == null) {
            return null;
        }

        return switch (nivel.toUpperCase()) {
            case "PRINCIPIANTE", "INTERMEDIO", "AVANZADO", "TODOS" -> nivel.toUpperCase();
            default -> null;
        };
    }

    /**
     * Valida la modalidad recibida desde la URL.
     *
     * Si viene null, no aplica filtro.
     * Si viene un valor inválido, tampoco aplica filtro.
     *
     * @param modalidad modalidad recibida.
     * @return modalidad válida o null.
     */
    private String validarModalidad(String modalidad) {
        if (modalidad == null) {
            return null;
        }

        return switch (modalidad.toUpperCase()) {
            case "PRESENCIAL", "ONLINE", "MIXTA" -> modalidad.toUpperCase();
            default -> null;
        };
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
