package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ImagenDTO;
import com.dondeentreno.api.entity.FotoGuardada;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.mapper.ImagenMapper;
import com.dondeentreno.api.repository.FotoGuardadaRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fotos guardadas (script 30): el patrón exacto de likes — guardar y
 * quitar idempotentes, solo fotos visibles, y el guardado sobrevive si
 * la foto se despublica (el listado la omite solo).
 */
@Service
public class FotoGuardadaService {

    private static final String ESTADO_MODERACION_APROBADA = "APROBADA";
    private static final String MENSAJE_FOTO_NO_ENCONTRADA = "No se encontro la foto.";

    private final FotoGuardadaRepository fotoGuardadaRepository;
    private final ImagenRepository imagenRepository;
    private final ImagenService imagenService;

    public FotoGuardadaService(
            FotoGuardadaRepository fotoGuardadaRepository,
            ImagenRepository imagenRepository,
            ImagenService imagenService
    ) {
        this.fotoGuardadaRepository = fotoGuardadaRepository;
        this.imagenRepository = imagenRepository;
        this.imagenService = imagenService;
    }

    @Transactional
    public void guardar(Long usuarioId, Long imagenId) {
        validarUserId(usuarioId);

        Imagen imagen = imagenRepository.findById(imagenId)
                .filter(encontrada -> Boolean.TRUE.equals(encontrada.getActiva())
                        && ESTADO_MODERACION_APROBADA.equals(encontrada.getEstadoModeracion()))
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_FOTO_NO_ENCONTRADA));

        if (fotoGuardadaRepository.existsByUsuarioIdAndImagenId(usuarioId, imagen.getId())) {
            return;
        }

        FotoGuardada guardada = new FotoGuardada();
        guardada.setUsuarioId(usuarioId);
        guardada.setImagenId(imagen.getId());
        guardada.setCreatedAt(OffsetDateTime.now());

        try {
            fotoGuardadaRepository.saveAndFlush(guardada);
        } catch (DataIntegrityViolationException excepcion) {
            /* Carrera del UNIQUE: mismo resultado. */
        }
    }

    @Transactional
    public void quitar(Long usuarioId, Long imagenId) {
        validarUserId(usuarioId);
        fotoGuardadaRepository.deleteByUsuarioIdAndImagenId(usuarioId, imagenId);
    }

    /** Ids guardados, para pintar los bookmarks. */
    @Transactional(readOnly = true)
    public List<Long> listarIds(Long usuarioId) {
        validarUserId(usuarioId);
        return fotoGuardadaRepository.imagenIdsDe(usuarioId);
    }

    /**
     * Las fotos guardadas VISIBLES, con snapshot vivo (la imagen real
     * de hoy): las despublicadas se omiten sin borrar el guardado.
     */
    @Transactional(readOnly = true)
    public List<ImagenDTO> listarVisibles(Long usuarioId) {
        validarUserId(usuarioId);

        List<FotoGuardada> guardadas =
                fotoGuardadaRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId);

        if (guardadas.isEmpty()) {
            return List.of();
        }

        List<Long> ids = guardadas.stream().map(FotoGuardada::getImagenId).toList();
        Map<Long, Imagen> visibles = imagenRepository.findAllById(ids).stream()
                .filter(imagen -> Boolean.TRUE.equals(imagen.getActiva())
                        && ESTADO_MODERACION_APROBADA.equals(imagen.getEstadoModeracion()))
                .collect(Collectors.toMap(Imagen::getId, Function.identity()));

        /* Mismos contadores agrupados que las galerías públicas. */
        return imagenService.conLikes(guardadas.stream()
                .map(guardada -> visibles.get(guardada.getImagenId()))
                .filter(imagen -> imagen != null)
                .map(ImagenMapper::toDTO)
                .toList());
    }

    private void validarUserId(Long usuarioId) {
        if (usuarioId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }
    }
}
