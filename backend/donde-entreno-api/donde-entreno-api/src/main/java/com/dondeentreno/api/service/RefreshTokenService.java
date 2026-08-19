package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.RefreshToken;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.repository.RefreshTokenRepository;
import com.dondeentreno.api.security.RefreshTokenProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Emision, rotacion y revocacion de refresh tokens opacos.
 *
 * Las tres garantias del diseño (docs/plan-refresh-token.md):
 * - El token en claro NUNCA se persiste: solo su SHA-256. Un dump de la
 *   tabla no autentica a nadie.
 * - Cada uso rota: el token consumido queda marcado y sale uno nuevo de
 *   la MISMA familia. Un token ya usado que vuelve a aparecer es un robo
 *   detectado y revoca la familia entera.
 * - El logout revoca en el servidor: a diferencia del access token (que
 *   muere solo por expiracion), el refresh muere de verdad.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    /** 256 bits de entropia: fuerza bruta inviable, sin necesidad de rate limit propio. */
    private static final int BYTES_DE_TOKEN = 32;

    /**
     * Un token usado hace menos de esto NO es un robo: es la carrera
     * benigna de dos pestañas arrancando a la vez con el mismo refresh
     * guardado. Se emite otro token de la familia sin revocar nada; el
     * que las pestañas no pisen queda sin usar y expira solo. Pasada la
     * gracia, un reuso es robo y cae la familia.
     */
    static final Duration GRACIA_REUSO = Duration.ofSeconds(30);

    private static final String MENSAJE_SESION_INVALIDA = "Sesion invalida o vencida.";

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom;

    @Autowired
    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenProperties properties
    ) {
        this(refreshTokenRepository, properties, Clock.systemUTC());
    }

    RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenProperties properties,
            Clock clock
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
        this.clock = clock;
        this.secureRandom = new SecureRandom();
    }

    /** El token en claro que viaja al cliente, con su vencimiento en segundos. */
    public record TokenEmitido(String token, long expiresInSeconds) {
    }

    /** Resultado de una rotacion valida: a quien pertenece y el token nuevo. */
    public record Rotacion(Long usuarioId, TokenEmitido token) {
    }

    /** Emite el primer token de una familia nueva (login o registro). */
    @Transactional
    public TokenEmitido emitirParaSesionNueva(Long usuarioId) {
        return emitir(usuarioId, UUID.randomUUID());
    }

    /**
     * Consume un token y emite el siguiente de su familia.
     *
     * El orden de los chequeos importa: revocado gana a todo (una
     * familia caida no se rehabilita), el reuso se evalua antes que el
     * vencimiento (un token robado viejo sigue delatando el robo), y
     * recien despues la expiracion.
     */
    @Transactional
    public Rotacion rotar(String tokenPlano) {
        RefreshToken token = buscarPorTokenPlano(tokenPlano)
                .orElseThrow(() -> new CredencialesInvalidasException(MENSAJE_SESION_INVALIDA));

        OffsetDateTime ahora = OffsetDateTime.now(clock);

        if (token.getRevocadoEn() != null) {
            throw new CredencialesInvalidasException(MENSAJE_SESION_INVALIDA);
        }

        if (token.getUsadoEn() != null) {
            boolean dentroDeLaGracia = Duration.between(token.getUsadoEn(), ahora)
                    .compareTo(GRACIA_REUSO) <= 0;

            if (!dentroDeLaGracia) {
                refreshTokenRepository.revocarFamilia(token.getFamilia(), ahora);
                /*
                  Solo metadata: nunca el token. Esta linea es la unica
                  senal de un robo detectado, tiene que poder greparse.
                */
                log.warn(
                        "Auth: REFRESH_REUSO_DETECTADO familia={} usuarioId={} — familia revocada",
                        token.getFamilia(),
                        token.getUsuarioId()
                );
                throw new CredencialesInvalidasException(MENSAJE_SESION_INVALIDA);
            }

            /* Carrera de dos pestañas: token fresco para la segunda, sin castigo. */
            return new Rotacion(token.getUsuarioId(), emitir(token.getUsuarioId(), token.getFamilia()));
        }

        if (token.getExpiraEn().isBefore(ahora)) {
            throw new CredencialesInvalidasException(MENSAJE_SESION_INVALIDA);
        }

        token.setUsadoEn(ahora);
        refreshTokenRepository.save(token);

        return new Rotacion(token.getUsuarioId(), emitir(token.getUsuarioId(), token.getFamilia()));
    }

    /**
     * Logout: revoca la familia del token, exista en el estado que
     * exista. Nunca lanza — un logout no puede fallar hacia el usuario,
     * y la respuesta no debe delatar si un token era valido.
     */
    @Transactional
    public void revocarFamiliaDe(String tokenPlano) {
        buscarPorTokenPlano(tokenPlano).ifPresent(token ->
                refreshTokenRepository.revocarFamilia(token.getFamilia(), OffsetDateTime.now(clock))
        );
    }

    /** Higiene en el login: borra los tokens del usuario vencidos hace mas de 30 dias. */
    @Transactional
    public void limpiarVencidosDe(Long usuarioId) {
        OffsetDateTime limite = OffsetDateTime.now(clock).minusDays(30);
        refreshTokenRepository.borrarVencidosDe(usuarioId, limite);
    }

    private TokenEmitido emitir(Long usuarioId, UUID familia) {
        byte[] bytes = new byte[BYTES_DE_TOKEN];
        secureRandom.nextBytes(bytes);
        String tokenPlano = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        OffsetDateTime ahora = OffsetDateTime.now(clock);

        RefreshToken registro = new RefreshToken();
        registro.setUsuarioId(usuarioId);
        registro.setTokenHash(calcularHash(tokenPlano));
        registro.setFamilia(familia);
        registro.setEmitidoEn(ahora);
        registro.setExpiraEn(ahora.plusDays(properties.getExpirationDaysValidado()));
        refreshTokenRepository.save(registro);

        return new TokenEmitido(tokenPlano, properties.getExpirationSeconds());
    }

    private java.util.Optional<RefreshToken> buscarPorTokenPlano(String tokenPlano) {
        if (tokenPlano == null || tokenPlano.isBlank()) {
            return java.util.Optional.empty();
        }

        return refreshTokenRepository.findByTokenHash(calcularHash(tokenPlano.trim()));
    }

    private String calcularHash(String tokenPlano) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(tokenPlano.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            /* SHA-256 es obligatorio en toda JVM: si falta, nada funciona. */
            throw new IllegalStateException("SHA-256 no disponible.", exception);
        }
    }
}
