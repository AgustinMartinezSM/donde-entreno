package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.UsuarioActualDTO;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.ImagenInvalidaException;
import com.dondeentreno.api.repository.UsuarioRepository;
import com.dondeentreno.api.storage.AlmacenArchivos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Avatar del usuario comun (fase 5d, script 21).
 *
 * Sin moderacion A PROPOSITO: el avatar no tiene superficie publica —
 * solo lo ve su dueño en su propia cabecera y menus. La condicion
 * quedo escrita en el plan: si alguna superficie futura muestra
 * avatares a terceros, la moderacion se diseña ANTES de esa
 * superficie.
 *
 * El archivo va directo al espacio publico usando el par existente
 * guardarPendiente + publicar del almacen (cero cambios de interfaz).
 * El reemplazo borra el avatar anterior best-effort, igual que la
 * eliminacion de aprobadas de fase 2.
 */
@Service
public class AvatarUsuarioService {

    private static final Logger log = LoggerFactory.getLogger(AvatarUsuarioService.class);

    /* Mismos limites que las imagenes del publicador (fase 2). */
    private static final long TAMANIO_MAXIMO_BYTES = 2L * 1024 * 1024;

    private final UsuarioRepository usuarioRepository;
    private final AlmacenArchivos almacenArchivos;

    public AvatarUsuarioService(
            UsuarioRepository usuarioRepository,
            AlmacenArchivos almacenArchivos
    ) {
        this.usuarioRepository = usuarioRepository;
        this.almacenArchivos = almacenArchivos;
    }

    @Transactional
    public UsuarioActualDTO actualizarAvatar(Long userId, MultipartFile archivo) {
        Usuario usuario = obtenerUsuarioActivo(userId);

        byte[] contenido = leerArchivoValidado(archivo);
        String extension = detectarExtension(contenido);

        String rutaPendiente = almacenArchivos.guardarPendiente(
                contenido,
                "avatares/" + usuario.getId(),
                extension
        );
        String urlNueva = almacenArchivos.publicar(rutaPendiente);

        String urlAnterior = usuario.getAvatarUrl();
        usuario.setAvatarUrl(urlNueva);
        usuario.setUpdatedAt(OffsetDateTime.now());
        usuarioRepository.save(usuario);

        eliminarPublicoBestEffort(urlAnterior);

        /* Solo metadata, nunca URLs completas de terceros ni contenido. */
        log.info("Usuario: AVATAR_ACTUALIZADO usuarioId={}", usuario.getId());

        return UsuarioActualDTO.desdeUsuario(usuario);
    }

    /** Vuelve a iniciales. Idempotente: sin avatar no falla ni delata nada. */
    @Transactional
    public UsuarioActualDTO eliminarAvatar(Long userId) {
        Usuario usuario = obtenerUsuarioActivo(userId);

        String urlAnterior = usuario.getAvatarUrl();

        if (urlAnterior != null) {
            usuario.setAvatarUrl(null);
            usuario.setUpdatedAt(OffsetDateTime.now());
            usuarioRepository.save(usuario);
            eliminarPublicoBestEffort(urlAnterior);
            log.info("Usuario: AVATAR_ELIMINADO usuarioId={}", usuario.getId());
        }

        return UsuarioActualDTO.desdeUsuario(usuario);
    }

    private Usuario obtenerUsuarioActivo(Long userId) {
        if (userId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }

        return usuarioRepository.findByIdAndActivoTrueAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CredencialesInvalidasException("No autenticado."));
    }

    /*
      La baja del archivo viejo es best-effort (patron fase 2): la
      columna ya apunta al nuevo y un fallo del bucket no puede tirar la
      operacion. El CDN puede retener la copia un tiempo.
    */
    private void eliminarPublicoBestEffort(String urlPublica) {
        if (urlPublica == null || urlPublica.isBlank()) {
            return;
        }

        try {
            almacenArchivos.eliminarPublicoPorUrl(urlPublica);
        } catch (RuntimeException excepcion) {
            log.warn(
                    "Usuario: no se pudo borrar el avatar anterior del bucket publico ({})",
                    excepcion.getMessage()
            );
        }
    }

    /*
      Validacion espejo de ImagenPublicadorService (fase 2): mismo tope
      de 2 MB y misma deteccion por firma de bytes. Se duplica a
      proposito en vez de extraer un helper: aquel service esta muy
      testeado y no se toca por esta fase.
    */
    private byte[] leerArchivoValidado(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ImagenInvalidaException("No se recibio ningun archivo.");
        }

        if (archivo.getSize() > TAMANIO_MAXIMO_BYTES) {
            throw new ImagenInvalidaException(
                    "El archivo supera el tamano maximo permitido (2 MB)."
            );
        }

        try {
            return archivo.getBytes();
        } catch (IOException exception) {
            throw new ImagenInvalidaException("No se pudo leer el archivo subido.");
        }
    }

    private String detectarExtension(byte[] contenido) {
        if (esJpeg(contenido)) {
            return "jpg";
        }

        if (esPng(contenido)) {
            return "png";
        }

        if (esWebp(contenido)) {
            return "webp";
        }

        throw new ImagenInvalidaException(
                "El archivo no es una imagen valida. Formatos permitidos: JPG, PNG o WebP."
        );
    }

    private boolean esJpeg(byte[] contenido) {
        return contenido.length >= 3
                && (contenido[0] & 0xFF) == 0xFF
                && (contenido[1] & 0xFF) == 0xD8
                && (contenido[2] & 0xFF) == 0xFF;
    }

    private boolean esPng(byte[] contenido) {
        byte[] firma = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        if (contenido.length < firma.length) {
            return false;
        }

        for (int i = 0; i < firma.length; i++) {
            if (contenido[i] != firma[i]) {
                return false;
            }
        }

        return true;
    }

    private boolean esWebp(byte[] contenido) {
        return contenido.length >= 12
                && contenido[0] == 'R'
                && contenido[1] == 'I'
                && contenido[2] == 'F'
                && contenido[3] == 'F'
                && contenido[8] == 'W'
                && contenido[9] == 'E'
                && contenido[10] == 'B'
                && contenido[11] == 'P';
    }
}
