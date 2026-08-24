package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.PerfilPublicadorDTO;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.mapper.PerfilPublicadorMapper;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SeguimientoPublicadorRepository;
import com.dondeentreno.api.repository.ValoracionRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service de PerfilPublicador.
 *
 * Esta capa contiene la lógica relacionada con los perfiles
 * que publican actividades en DondeEntreno.
 */
@Service
public class PerfilPublicadorService {

    private static final String MENSAJE_PERFIL_NO_ENCONTRADO =
            "El perfil publicador solicitado no existe o no está disponible.";
    private static final String ESTADO_PUBLICADA = "PUBLICADA";
    private static final String ESTADO_APROBADA = "APROBADA";

    private final PerfilPublicadorRepository perfilPublicadorRepository;
    private final SeguimientoPublicadorRepository seguimientoPublicadorRepository;
    private final ImagenService imagenService;
    private final ActividadRepository actividadRepository;
    private final ImagenRepository imagenRepository;
    private final ValoracionRepository valoracionRepository;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente los repositories
     * y los entrega a este service.
     */
    public PerfilPublicadorService(
            PerfilPublicadorRepository perfilPublicadorRepository,
            SeguimientoPublicadorRepository seguimientoPublicadorRepository,
            ImagenService imagenService,
            ActividadRepository actividadRepository,
            ImagenRepository imagenRepository,
            ValoracionRepository valoracionRepository
    ) {
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.seguimientoPublicadorRepository = seguimientoPublicadorRepository;
        this.imagenService = imagenService;
        this.actividadRepository = actividadRepository;
        this.imagenRepository = imagenRepository;
        this.valoracionRepository = valoracionRepository;
    }

    /**
     * Obtiene todos los perfiles publicadores activos,
     * ordenados por nombre.
     *
     * @return lista de perfiles publicadores activos en formato DTO.
     */
    public List<PerfilPublicadorDTO> obtenerPerfilesActivos() {
        List<PerfilPublicador> perfiles =
                perfilPublicadorRepository.findByActivoTrueOrderByNombreAsc();

        return mapearConSeguidores(perfiles);
    }

    /**
     * Obtiene un perfil publicador activo por ID.
     *
     * Aplica el mismo criterio de visibilidad que el listado público
     * (activo = true) para que un perfil no pueda aparecer en la lista y
     * responder 404 en su detalle.
     *
     * @param id ID del perfil publicador.
     * @return el perfil en formato DTO.
     * @throws RecursoNoEncontradoException si no existe o no está activo.
     */
    public PerfilPublicadorDTO obtenerPerfilActivoPorId(Long id) {
        if (id == null) {
            throw new RecursoNoEncontradoException(MENSAJE_PERFIL_NO_ENCONTRADO);
        }

        return enriquecerDetalle(
                perfilPublicadorRepository.findByIdAndActivoTrue(id)
                        .map(PerfilPublicadorMapper::toDTO)
                        .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_PERFIL_NO_ENCONTRADO))
        );
    }

    /**
     * Resuelve el detalle por id o por slug (script 27): un path
     * numérico es un id (los links viejos siguen andando); cualquier
     * otro texto se busca como slug. Mismo criterio de visibilidad.
     */
    public PerfilPublicadorDTO obtenerPerfilActivoPorIdOSlug(String idOSlug) {
        if (idOSlug == null || idOSlug.isBlank()) {
            throw new RecursoNoEncontradoException(MENSAJE_PERFIL_NO_ENCONTRADO);
        }

        String valor = idOSlug.trim();

        if (valor.matches("\\d+")) {
            try {
                return obtenerPerfilActivoPorId(Long.parseLong(valor));
            } catch (NumberFormatException excepcion) {
                /* Numérico pero desbordado: no es un id real. */
                throw new RecursoNoEncontradoException(MENSAJE_PERFIL_NO_ENCONTRADO);
            }
        }

        return enriquecerDetalle(
                perfilPublicadorRepository.findBySlugAndActivoTrue(valor)
                        .map(PerfilPublicadorMapper::toDTO)
                        .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_PERFIL_NO_ENCONTRADO))
        );
    }

    private PerfilPublicadorDTO enriquecerDetalle(PerfilPublicadorDTO dto) {
        dto.setCantidadSeguidores(
                seguimientoPublicadorRepository.countByPerfilPublicador_Id(dto.getId())
        );
        asignarLogos(List.of(dto));
        asignarStats(List.of(dto));

        return dto;
    }

    /**
     * Stats de cabecera (Fase 5): actividades, fotos y valoraciones.
     * Todo por queries agrupadas sobre el lote de perfiles — el mismo
     * criterio anti-N+1 que ya rige para seguidores y logos.
     *
     * El promedio respeta el umbral de ValoracionService
     * (MINIMO_PARA_PROMEDIO): con menos de 3 valoraciones viaja null,
     * igual que en el detalle de actividad. Mostrar un promedio de una
     * sola reseña en el perfil contradiría al número de la actividad.
     */
    private void asignarStats(List<PerfilPublicadorDTO> dtos) {
        List<Long> ids = dtos.stream()
                .map(PerfilPublicadorDTO::getId)
                .filter(Objects::nonNull)
                .toList();

        if (ids.isEmpty()) {
            return;
        }

        Map<Long, Long> actividades = new HashMap<>();
        for (Object[] fila : actividadRepository.contarPublicadasPorPerfil(ids, ESTADO_PUBLICADA)) {
            actividades.put((Long) fila[0], (Long) fila[1]);
        }

        Map<Long, Long> fotos = new HashMap<>();
        for (Object[] fila
                : imagenRepository.contarFotosVisiblesPorPublicador(ids, ESTADO_APROBADA)) {
            if (fila[0] != null) {
                fotos.put((Long) fila[0], (Long) fila[1]);
            }
        }

        /* Distribución 1..5 por perfil: de ahí salen promedio Y cantidad. */
        Map<Long, long[]> distribucionPorPerfil = new HashMap<>();
        for (Object[] fila : valoracionRepository.distribucionVisiblesPorPublicador(ids)) {
            Long perfilId = (Long) fila[0];
            int puntaje = ((Number) fila[1]).intValue();
            long cantidad = ((Number) fila[2]).longValue();

            if (perfilId == null || puntaje < 1 || puntaje > 5) {
                continue;
            }

            distribucionPorPerfil
                    .computeIfAbsent(perfilId, clave -> new long[6])[puntaje] += cantidad;
        }

        for (PerfilPublicadorDTO dto : dtos) {
            dto.setCantidadActividades(actividades.getOrDefault(dto.getId(), 0L));
            dto.setCantidadFotos(fotos.getOrDefault(dto.getId(), 0L));

            long[] distribucion = distribucionPorPerfil.get(dto.getId());
            long total = 0;
            long suma = 0;

            if (distribucion != null) {
                for (int puntaje = 1; puntaje <= 5; puntaje++) {
                    total += distribucion[puntaje];
                    suma += (long) puntaje * distribucion[puntaje];
                }
            }

            dto.setCantidadValoraciones(total);
            dto.setValoracionPromedio(
                    total >= ValoracionService.MINIMO_PARA_PROMEDIO
                            ? Math.round((double) suma / total * 10.0) / 10.0
                            : null
            );
        }
    }

    /**
     * Obtiene perfiles activos filtrados por tipo.
     *
     * @param tipoPublicador tipo de publicador.
     * @return lista de perfiles activos de ese tipo.
     */
    public List<PerfilPublicadorDTO> obtenerPerfilesActivosPorTipo(String tipoPublicador) {
        List<PerfilPublicador> perfiles =
                perfilPublicadorRepository.findByActivoTrueAndTipoPublicadorOrderByNombreAsc(tipoPublicador);

        return mapearConSeguidores(perfiles);
    }

    /**
     * Mapea a DTO y completa la cantidad de seguidores con un solo
     * query agrupado, para que un listado de N perfiles no dispare N
     * conteos.
     *
     * Los perfiles sin seguidores no vuelven en el GROUP BY, así que se
     * completan con cero: el campo nunca queda nulo en un listado.
     */
    private List<PerfilPublicadorDTO> mapearConSeguidores(List<PerfilPublicador> perfiles) {
        List<PerfilPublicadorDTO> dtos = perfiles.stream()
                .map(PerfilPublicadorMapper::toDTO)
                .toList();

        List<Long> ids = dtos.stream()
                .map(PerfilPublicadorDTO::getId)
                .filter(Objects::nonNull)
                .toList();

        if (ids.isEmpty()) {
            return dtos;
        }

        Map<Long, Long> seguidoresPorPerfil = new HashMap<>();

        for (SeguimientoPublicadorRepository.ConteoSeguidores conteo
                : seguimientoPublicadorRepository.contarSeguidoresPorPerfiles(ids)) {
            if (conteo.getPerfilPublicadorId() != null) {
                seguidoresPorPerfil.put(conteo.getPerfilPublicadorId(), conteo.getCantidad());
            }
        }

        for (PerfilPublicadorDTO dto : dtos) {
            dto.setCantidadSeguidores(seguidoresPorPerfil.getOrDefault(dto.getId(), 0L));
        }

        asignarLogos(dtos);
        asignarStats(dtos);

        return dtos;
    }

    /**
     * Completa logoUrl con el LOGO aprobado de cada perfil (fix UX
     * 2026-08-22: identidad única del publicador). La resolución vive
     * en ImagenService — la misma consulta que usan las cards.
     */
    private void asignarLogos(List<PerfilPublicadorDTO> dtos) {
        List<Long> ids = dtos.stream()
                .map(PerfilPublicadorDTO::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, String> logoPorPerfil =
                imagenService.obtenerLogosAprobadosPorPerfil(ids);

        for (PerfilPublicadorDTO dto : dtos) {
            dto.setLogoUrl(logoPorPerfil.get(dto.getId()));
        }
    }
}