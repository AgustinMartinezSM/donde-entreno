package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.SocialProofDTO;
import com.dondeentreno.api.repository.FavoritoActividadRepository;
import com.dondeentreno.api.repository.MeGustaImagenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Señales agregadas de confianza del detalle público (etapa A del plan
 * de valoraciones): guardados, likes de fotos visibles y personas que
 * entrenaron (30 días). Es el detalle de UNA actividad: tres counts
 * directos, sin N+1. Nunca nombres — solo números anónimos.
 */
@Service
public class SocialProofService {

    private final FavoritoActividadRepository favoritoActividadRepository;
    private final MeGustaImagenRepository meGustaImagenRepository;
    private final CheckinService checkinService;

    public SocialProofService(
            FavoritoActividadRepository favoritoActividadRepository,
            MeGustaImagenRepository meGustaImagenRepository,
            CheckinService checkinService
    ) {
        this.favoritoActividadRepository = favoritoActividadRepository;
        this.meGustaImagenRepository = meGustaImagenRepository;
        this.checkinService = checkinService;
    }

    @Transactional(readOnly = true)
    public SocialProofDTO deActividad(Long actividadId) {
        return new SocialProofDTO(
                favoritoActividadRepository.countByActividadId(actividadId),
                meGustaImagenRepository.contarDeActividad(actividadId),
                checkinService.contarPersonas30Dias(actividadId)
        );
    }
}
