package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.SocialProofDTO;
import com.dondeentreno.api.repository.FavoritoActividadRepository;
import com.dondeentreno.api.repository.MeGustaImagenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialProofServiceTest {

    private static final Long ACTIVIDAD_ID = 70L;

    @Mock
    private FavoritoActividadRepository favoritoActividadRepository;

    @Mock
    private MeGustaImagenRepository meGustaImagenRepository;

    @Mock
    private CheckinService checkinService;

    private SocialProofService service;

    @BeforeEach
    void setUp() {
        service = new SocialProofService(
                favoritoActividadRepository,
                meGustaImagenRepository,
                checkinService
        );
    }

    @Test
    void armaLasTresSenalesDeLaActividad() {
        when(favoritoActividadRepository.countByActividadId(ACTIVIDAD_ID)).thenReturn(4L);
        when(meGustaImagenRepository.contarDeActividad(ACTIVIDAD_ID)).thenReturn(9L);
        when(checkinService.contarPersonas30Dias(ACTIVIDAD_ID)).thenReturn(2L);

        SocialProofDTO proof = service.deActividad(ACTIVIDAD_ID);

        assertEquals(4L, proof.getCantidadFavoritos());
        assertEquals(9L, proof.getCantidadLikesFotos());
        assertEquals(2L, proof.getCantidadPersonasEntrenaron30Dias());
    }
}
