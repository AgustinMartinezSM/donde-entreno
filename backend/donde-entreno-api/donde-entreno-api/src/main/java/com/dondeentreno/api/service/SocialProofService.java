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
    private final InteresActividadService interesActividadService;
    private final ValoracionService valoracionService;

    public SocialProofService(
            FavoritoActividadRepository favoritoActividadRepository,
            MeGustaImagenRepository meGustaImagenRepository,
            CheckinService checkinService,
            InteresActividadService interesActividadService,
            ValoracionService valoracionService
    ) {
        this.favoritoActividadRepository = favoritoActividadRepository;
        this.meGustaImagenRepository = meGustaImagenRepository;
        this.checkinService = checkinService;
        this.interesActividadService = interesActividadService;
        this.valoracionService = valoracionService;
    }

    @Transactional(readOnly = true)
    public SocialProofDTO deActividad(Long actividadId) {
        SocialProofDTO proof = new SocialProofDTO(
                favoritoActividadRepository.countByActividadId(actividadId),
                meGustaImagenRepository.contarDeActividad(actividadId),
                checkinService.contarPersonas30Dias(actividadId)
        );

        /* Fase 3: interés y valoraciones. */
        proof.setCantidadQuierenProbar(
                interesActividadService.contarQuierenProbar(actividadId)
        );
        double[] promedioYCantidad = valoracionService.promedioYCantidad(actividadId);
        proof.setValoracionPromedio(
                promedioYCantidad[0] >= 0 ? promedioYCantidad[0] : null
        );
        proof.setCantidadValoraciones((long) promedioYCantidad[1]);

        return proof;
    }
}
