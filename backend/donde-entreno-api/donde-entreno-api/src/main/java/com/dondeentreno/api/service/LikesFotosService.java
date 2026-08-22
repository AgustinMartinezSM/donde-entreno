package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.MeGustaImagen;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.MeGustaImagenRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Likes en fotos (script 23, bloque 14) — el patron exacto de
 * favoritos: dar y quitar son idempotentes, la carrera del UNIQUE se
 * absorbe, y solo se puede likear lo que se puede ver (foto APROBADA y
 * activa). La fila sobrevive si la foto se despublica: el conteo
 * publico la omite solo (el enriquecimiento cuenta sobre las visibles).
 */
@Service
public class LikesFotosService {

    private static final String ESTADO_MODERACION_APROBADA = "APROBADA";
    private static final String MENSAJE_FOTO_NO_ENCONTRADA = "No se encontro la foto.";

    private final MeGustaImagenRepository meGustaImagenRepository;
    private final ImagenRepository imagenRepository;

    public LikesFotosService(
            MeGustaImagenRepository meGustaImagenRepository,
            ImagenRepository imagenRepository
    ) {
        this.meGustaImagenRepository = meGustaImagenRepository;
        this.imagenRepository = imagenRepository;
    }

    @Transactional
    public void dar(Long usuarioId, Long imagenId) {
        validarUserId(usuarioId);

        Imagen imagen = imagenRepository.findById(imagenId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_FOTO_NO_ENCONTRADA));

        /* Solo lo visible se likea: 404, no 403 — no se delata que existe. */
        if (!Boolean.TRUE.equals(imagen.getActiva())
                || !ESTADO_MODERACION_APROBADA.equals(imagen.getEstadoModeracion())) {
            throw new RecursoNoEncontradoException(MENSAJE_FOTO_NO_ENCONTRADA);
        }

        if (meGustaImagenRepository.existsByUsuarioIdAndImagenId(usuarioId, imagenId)) {
            return;
        }

        MeGustaImagen like = new MeGustaImagen();
        like.setUsuarioId(usuarioId);
        like.setImagenId(imagenId);
        like.setCreatedAt(OffsetDateTime.now());

        try {
            meGustaImagenRepository.saveAndFlush(like);
        } catch (DataIntegrityViolationException excepcion) {
            /* Otro request lo dio en el medio: mismo resultado. */
        }
    }

    @Transactional
    public void quitar(Long usuarioId, Long imagenId) {
        validarUserId(usuarioId);
        meGustaImagenRepository.deleteByUsuarioIdAndImagenId(usuarioId, imagenId);
    }

    /** Ids de fotos con like del usuario, para pintar los corazones. */
    @Transactional(readOnly = true)
    public List<Long> listarIds(Long usuarioId) {
        validarUserId(usuarioId);
        return meGustaImagenRepository.imagenIdsDe(usuarioId);
    }

    private void validarUserId(Long usuarioId) {
        if (usuarioId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }
    }
}
