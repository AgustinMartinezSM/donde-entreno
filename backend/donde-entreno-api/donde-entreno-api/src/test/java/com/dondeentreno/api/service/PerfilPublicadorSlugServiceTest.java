package com.dondeentreno.api.service;

import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilPublicadorSlugServiceTest {

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    private PerfilPublicadorSlugService service;

    @BeforeEach
    void setUp() {
        service = new PerfilPublicadorSlugService(perfilPublicadorRepository);
    }

    @Test
    void normalizaAcentosMayusculasYSimbolos() {
        when(perfilPublicadorRepository.existsBySlug("club-atletico-sur")).thenReturn(false);

        assertEquals("club-atletico-sur", service.generarSlugUnico("Club Atlético Sur"));
    }

    @Test
    void anteColisionAgregaSufijoNumerico() {
        when(perfilPublicadorRepository.existsBySlug("club-union")).thenReturn(true);
        when(perfilPublicadorRepository.existsBySlug("club-union-2")).thenReturn(false);

        assertEquals("club-union-2", service.generarSlugUnico("Club Unión"));
    }

    @Test
    void unNombreSinCaracteresValidosCaeAlFallback() {
        when(perfilPublicadorRepository.existsBySlug("publicador")).thenReturn(false);

        assertEquals("publicador", service.generarSlugUnico("¡¡¡···!!!"));
    }
}
