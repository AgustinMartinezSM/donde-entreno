package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ReporteAdminDTO;
import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.entity.Reporte;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.ReporteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Reportes de contenido (script 28, Fase 2 social): la base de la
 * moderación flexible. V1 sin auto-ocultar (decisión del plan): los
 * reportes van a la cola del admin, que decide.
 */
@Service
public class ReporteService {

    private static final List<String> TIPOS_OBJETO =
            List.of("IMAGEN", "PERFIL_PUBLICADOR", "ACTIVIDAD");
    private static final List<String> MOTIVOS = List.of(
            "CONTENIDO_INAPROPIADO",
            "INFORMACION_FALSA",
            "SPAM",
            "SUPLANTACION",
            "OTRO"
    );
    private static final List<String> ESTADOS =
            List.of("PENDIENTE", "REVISADO", "DESESTIMADO", "ACCIONADO");
    private static final int MAX_DETALLE = 280;
    private static final int MAX_PAGINA = 50;

    private final ReporteRepository reporteRepository;
    private final ImagenRepository imagenRepository;
    private final PerfilPublicadorRepository perfilPublicadorRepository;
    private final ActividadRepository actividadRepository;

    public ReporteService(
            ReporteRepository reporteRepository,
            ImagenRepository imagenRepository,
            PerfilPublicadorRepository perfilPublicadorRepository,
            ActividadRepository actividadRepository
    ) {
        this.reporteRepository = reporteRepository;
        this.imagenRepository = imagenRepository;
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.actividadRepository = actividadRepository;
    }

    /**
     * Crea el reporte. Idempotente: reportar dos veces lo mismo no
     * duplica ni falla (el UNIQUE absorbe la carrera).
     */
    @Transactional
    public void reportar(
            Long usuarioId,
            String tipoObjeto,
            Long objetoId,
            String motivo,
            String detalle
    ) {
        validarUserId(usuarioId);

        if (tipoObjeto == null || !TIPOS_OBJETO.contains(tipoObjeto)) {
            throw new FiltroInvalidoException("El tipo de objeto reportado no es valido.");
        }
        if (motivo == null || !MOTIVOS.contains(motivo)) {
            throw new FiltroInvalidoException("El motivo del reporte no es valido.");
        }
        if (objetoId == null) {
            throw new FiltroInvalidoException("Falta el objeto reportado.");
        }

        validarObjetoVisible(tipoObjeto, objetoId);

        if (reporteRepository.existsByUsuarioIdAndTipoObjetoAndObjetoId(
                usuarioId, tipoObjeto, objetoId)) {
            return;
        }

        Reporte reporte = new Reporte();
        reporte.setUsuarioId(usuarioId);
        reporte.setTipoObjeto(tipoObjeto);
        reporte.setObjetoId(objetoId);
        reporte.setMotivo(motivo);
        reporte.setDetalle(normalizarDetalle(detalle));
        reporte.setEstado("PENDIENTE");
        reporte.setCreatedAt(OffsetDateTime.now());

        try {
            reporteRepository.saveAndFlush(reporte);
        } catch (DataIntegrityViolationException excepcion) {
            /* Otro request lo reporto en el medio: mismo resultado. */
        }
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<ReporteAdminDTO> listarParaAdmin(
            String estado,
            int page,
            int size
    ) {
        int tamanio = Math.min(Math.max(size, 1), MAX_PAGINA);
        PageRequest pagina = PageRequest.of(Math.max(page, 0), tamanio);

        Page<Reporte> resultado;
        if (estado == null || estado.isBlank()) {
            resultado = reporteRepository.findAllByOrderByCreatedAtDesc(pagina);
        } else {
            if (!ESTADOS.contains(estado)) {
                throw new FiltroInvalidoException("El estado del filtro no es valido.");
            }
            resultado = reporteRepository.findByEstadoOrderByCreatedAtDesc(estado, pagina);
        }

        return new PaginaResponseDTO<>(
                resultado.getContent().stream().map(this::toAdminDTO).toList(),
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages(),
                resultado.isLast()
        );
    }

    @Transactional
    public ReporteAdminDTO cambiarEstado(Long reporteId, String estadoNuevo) {
        if (estadoNuevo == null || !ESTADOS.contains(estadoNuevo)) {
            throw new FiltroInvalidoException("El estado del reporte no es valido.");
        }

        Reporte reporte = reporteRepository.findById(reporteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro el reporte."));

        reporte.setEstado(estadoNuevo);
        return toAdminDTO(reporteRepository.save(reporte));
    }

    @Transactional(readOnly = true)
    public long contarPendientes() {
        return reporteRepository.countByEstado("PENDIENTE");
    }

    /**
     * Solo lo visible se reporta: lo demas da 404, no 403 — no se
     * delata que existe (patrón likes).
     */
    private void validarObjetoVisible(String tipoObjeto, Long objetoId) {
        boolean visible = switch (tipoObjeto) {
            case "IMAGEN" -> imagenRepository.findById(objetoId)
                    .filter(imagen -> Boolean.TRUE.equals(imagen.getActiva())
                            && "APROBADA".equals(imagen.getEstadoModeracion()))
                    .isPresent();
            case "PERFIL_PUBLICADOR" ->
                    perfilPublicadorRepository.findByIdAndActivoTrue(objetoId).isPresent();
            case "ACTIVIDAD" -> actividadRepository.findById(objetoId)
                    .filter(actividad -> Boolean.TRUE.equals(actividad.getActiva())
                            && "PUBLICADA".equals(actividad.getEstadoPublicacion())
                            && actividad.getDeletedAt() == null)
                    .isPresent();
            default -> false;
        };

        if (!visible) {
            throw new RecursoNoEncontradoException("No se encontro el contenido reportado.");
        }
    }

    private ReporteAdminDTO toAdminDTO(Reporte reporte) {
        return new ReporteAdminDTO(
                reporte.getId(),
                reporte.getTipoObjeto(),
                reporte.getObjetoId(),
                reporte.getMotivo(),
                reporte.getDetalle(),
                reporte.getEstado(),
                reporte.getCreatedAt()
        );
    }

    private String normalizarDetalle(String detalle) {
        if (detalle == null || detalle.isBlank()) {
            return null;
        }

        String limpio = detalle.trim();
        return limpio.length() <= MAX_DETALLE ? limpio : limpio.substring(0, MAX_DETALLE);
    }

    private void validarUserId(Long usuarioId) {
        if (usuarioId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }
    }
}
