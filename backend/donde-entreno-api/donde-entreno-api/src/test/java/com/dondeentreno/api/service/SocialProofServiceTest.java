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

    @Mock
    private InteresActividadService interesActividadService;

    @Mock
    private ValoracionService valoracionService;

    private SocialProofService service;

    @BeforeEach
    void setUp() {
        service = new SocialProofService(
                favoritoActividadRepository,
                meGustaImagenRepository,
                checkinService,
                interesActividadService,
                valoracionService
        );
    }

    @Test
    void armaLasSenalesDeLaActividad() {
        when(favoritoActividadRepository.countByActividadId(ACTIVIDAD_ID)).thenReturn(4L);
        when(meGustaImagenRepository.contarDeActividad(ACTIVIDAD_ID)).thenReturn(9L);
        when(checkinService.contarPersonas30Dias(ACTIVIDAD_ID)).thenReturn(2L);
        when(interesActividadService.contarQuierenProbar(ACTIVIDAD_ID)).thenReturn(7L);
        when(valoracionService.promedioYCantidad(ACTIVIDAD_ID))
                .thenReturn(new double[]{4.5, 6});

        SocialProofDTO proof = service.deActividad(ACTIVIDAD_ID);

        assertEquals(4L, proof.getCantidadFavoritos());
        assertEquals(9L, proof.getCantidadLikesFotos());
        assertEquals(2L, proof.getCantidadPersonasEntrenaron30Dias());
        assertEquals(7L, proof.getCantidadQuierenProbar());
        assertEquals(4.5, proof.getValoracionPromedio());
        assertEquals(6L, proof.getCantidadValoraciones());
    }

    /* Con menos de 3 valoraciones el promedio NO viaja (regla del plan). */
    @Test
    void conPocasValoracionesElPromedioEsNull() {
        when(favoritoActividadRepository.countByActividadId(ACTIVIDAD_ID)).thenReturn(0L);
        when(meGustaImagenRepository.contarDeActividad(ACTIVIDAD_ID)).thenReturn(0L);
        when(checkinService.contarPersonas30Dias(ACTIVIDAD_ID)).thenReturn(0L);
        when(interesActividadService.contarQuierenProbar(ACTIVIDAD_ID)).thenReturn(0L);
        when(valoracionService.promedioYCantidad(ACTIVIDAD_ID))
                .thenReturn(new double[]{-1, 2});

        SocialProofDTO proof = service.deActividad(ACTIVIDAD_ID);

        assertEquals(null, proof.getValoracionPromedio());
        assertEquals(2L, proof.getCantidadValoraciones());
    }
}
