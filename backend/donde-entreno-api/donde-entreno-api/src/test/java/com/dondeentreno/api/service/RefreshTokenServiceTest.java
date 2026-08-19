package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.RefreshToken;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.repository.RefreshTokenRepository;
import com.dondeentreno.api.security.RefreshTokenProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest {

    private static final Instant AHORA = Instant.parse("2026-08-18T12:00:00Z");
    private static final OffsetDateTime AHORA_ODT = AHORA.atOffset(ZoneOffset.UTC);

    private RefreshTokenRepository repository;
    private RefreshTokenService service;

    @BeforeEach
    void preparar() {
        repository = mock(RefreshTokenRepository.class);
        service = new RefreshTokenService(
                repository,
                new RefreshTokenProperties(),
                Clock.fixed(AHORA, ZoneOffset.UTC)
        );
    }

    @Test
    void emitirGuardaSoloElHashYDevuelveElTokenEnClaro() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.TokenEmitido emitido = service.emitirParaSesionNueva(7L);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        RefreshToken guardado = captor.getValue();

        /* 32 bytes en base64url sin padding: 43 chars, y NUNCA el hash. */
        assertEquals(43, emitido.token().length());
        assertEquals(64, guardado.getTokenHash().length());
        assertNotEquals(emitido.token(), guardado.getTokenHash());
        assertEquals(7L, guardado.getUsuarioId());
        assertNotNull(guardado.getFamilia());
        assertEquals(AHORA_ODT, guardado.getEmitidoEn());
        /* Default de 30 dias, deslizante. */
        assertEquals(AHORA_ODT.plusDays(30), guardado.getExpiraEn());
        assertEquals(30L * 24 * 60 * 60, emitido.expiresInSeconds());
    }

    @Test
    void rotarUnTokenValidoLoMarcaUsadoYEmiteOtroDeLaMismaFamilia() {
        UUID familia = UUID.randomUUID();
        RefreshToken token = token(7L, familia);
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.Rotacion rotacion = service.rotar("token-en-claro");

        assertEquals(7L, rotacion.usuarioId());
        assertEquals(AHORA_ODT, token.getUsadoEn());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository, Mockito.times(2)).save(captor.capture());
        RefreshToken nuevo = captor.getAllValues().get(1);
        assertEquals(familia, nuevo.getFamilia());
        assertEquals(7L, nuevo.getUsuarioId());
    }

    @Test
    void rotarUnTokenInexistenteDevuelve401() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> service.rotar("no-existe"));
    }

    @Test
    void rotarUnTokenRevocadoDevuelve401SinTocarLaFamilia() {
        RefreshToken token = token(7L, UUID.randomUUID());
        token.setRevocadoEn(AHORA_ODT.minusHours(1));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThrows(CredencialesInvalidasException.class, () -> service.rotar("revocado"));
        verify(repository, never()).revocarFamilia(any(), any());
    }

    @Test
    void rotarUnTokenVencidoDevuelve401() {
        RefreshToken token = token(7L, UUID.randomUUID());
        token.setExpiraEn(AHORA_ODT.minusMinutes(1));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThrows(CredencialesInvalidasException.class, () -> service.rotar("vencido"));
    }

    /*
      La garantia central del diseño: un token ya usado que reaparece
      pasada la gracia es un robo detectado, y cae la familia entera.
    */
    @Test
    void rotarUnTokenYaUsadoFueraDeLaGraciaRevocaLaFamiliaEntera() {
        UUID familia = UUID.randomUUID();
        RefreshToken token = token(7L, familia);
        token.setUsadoEn(AHORA_ODT.minusMinutes(5));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThrows(CredencialesInvalidasException.class, () -> service.rotar("robado"));
        verify(repository).revocarFamilia(eq(familia), eq(AHORA_ODT));
    }

    /*
      Dos pestañas arrancando a la vez con el mismo refresh guardado no
      son un robo: dentro de la gracia se emite otro token de la familia
      sin castigo. Sin esto, abrir dos pestañas juntas desloguearia a la
      persona.
    */
    @Test
    void rotarUnTokenUsadoDentroDeLaGraciaEmiteOtroSinRevocar() {
        UUID familia = UUID.randomUUID();
        RefreshToken token = token(7L, familia);
        token.setUsadoEn(AHORA_ODT.minusSeconds(5));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.Rotacion rotacion = service.rotar("carrera-de-pestanas");

        assertEquals(7L, rotacion.usuarioId());
        assertNotNull(rotacion.token().token());
        verify(repository, never()).revocarFamilia(any(), any());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        assertEquals(familia, captor.getValue().getFamilia());
    }

    @Test
    void revocarFamiliaDeUnTokenExistenteRevocaLaFamilia() {
        UUID familia = UUID.randomUUID();
        RefreshToken token = token(7L, familia);
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        service.revocarFamiliaDe("token-de-logout");

        verify(repository).revocarFamilia(eq(familia), eq(AHORA_ODT));
    }

    @Test
    void revocarFamiliaDeUnTokenInexistenteNoExplota() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        service.revocarFamiliaDe("cualquier-cosa");

        verify(repository, never()).revocarFamilia(any(), any());
    }

    @Test
    void limpiarVencidosBorraLosAnterioresAlLimiteDe30Dias() {
        service.limpiarVencidosDe(7L);

        verify(repository).borrarVencidosDe(7L, AHORA_ODT.minusDays(30));
    }

    /*
      Un token nulo o en blanco no llega al repositorio: se corta antes
      (el hash de "" tambien es un hash valido y no debe consultarse).
    */
    @Test
    void unTokenEnBlancoDevuelve401SinConsultarElRepositorio() {
        assertThrows(CredencialesInvalidasException.class, () -> service.rotar("   "));
        assertThrows(CredencialesInvalidasException.class, () -> service.rotar(null));
        verify(repository, never()).findByTokenHash(any());
    }

    @Test
    void dosEmisionesNuncaRepitenTokenNiHash() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.TokenEmitido primero = service.emitirParaSesionNueva(7L);
        RefreshTokenService.TokenEmitido segundo = service.emitirParaSesionNueva(7L);

        assertNotEquals(primero.token(), segundo.token());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository, Mockito.times(2)).save(captor.capture());
        assertNotEquals(
                captor.getAllValues().get(0).getTokenHash(),
                captor.getAllValues().get(1).getTokenHash()
        );
        assertTrue(
                !captor.getAllValues().get(0).getFamilia()
                        .equals(captor.getAllValues().get(1).getFamilia())
        );
    }

    private RefreshToken token(Long usuarioId, UUID familia) {
        RefreshToken token = new RefreshToken();
        token.setUsuarioId(usuarioId);
        token.setTokenHash("hash-ficticio");
        token.setFamilia(familia);
        token.setEmitidoEn(AHORA_ODT.minusDays(1));
        token.setExpiraEn(AHORA_ODT.plusDays(29));
        return token;
    }
}
