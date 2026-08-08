package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ImagenPublicadorDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.ImagenInvalidaException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.storage.AlmacenArchivos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Subida y gestión de imágenes de actividades desde el panel publicador.
 *
 * Las imágenes nacen PENDIENTE, con activa=false y con el archivo en el
 * bucket PRIVADO de Supabase Storage: no existen para el público (ni
 * por listado ni por URL directa) hasta que un admin las aprueba.
 * Mientras la fila está PENDIENTE, imagen.url guarda la ruta interna
 * del objeto; al aprobarse pasa a ser la URL pública definitiva.
 */
@Service
public class ImagenPublicadorService {

    private static final Logger log = LoggerFactory.getLogger(ImagenPublicadorService.class);

    static final String ESTADO_PENDIENTE = "PENDIENTE";
    static final String ESTADO_APROBADA = "APROBADA";
    static final String ESTADO_RECHAZADA = "RECHAZADA";

    private static final String ESTADO_PUBLICACION_PUBLICADA = "PUBLICADA";
    private static final long TAMANIO_MAXIMO_BYTES = 2L * 1024 * 1024;
    private static final List<String> TIPOS_PERMITIDOS = List.of("PRINCIPAL", "GALERIA");
    private static final String MOTIVO_ELIMINADA_POR_PUBLICADOR =
            "Eliminada por el publicador antes de la revision.";
    private static final Duration VALIDEZ_URL_FIRMADA = Duration.ofMinutes(10);

    private final ImagenRepository imagenRepository;
    private final PerfilPublicadorRepository perfilPublicadorRepository;
    private final ActividadRepository actividadRepository;
    private final AlmacenArchivos almacenArchivos;

    public ImagenPublicadorService(
            ImagenRepository imagenRepository,
            PerfilPublicadorRepository perfilPublicadorRepository,
            ActividadRepository actividadRepository,
            AlmacenArchivos almacenArchivos
    ) {
        this.imagenRepository = imagenRepository;
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.actividadRepository = actividadRepository;
        this.almacenArchivos = almacenArchivos;
    }

    /**
     * Sube una imagen para una actividad publicada propia.
     * Valida tipo lógico (PRINCIPAL/GALERIA), tamaño y que el contenido
     * sea realmente JPEG/PNG/WebP (firma de bytes, no solo content-type).
     */
    @Transactional
    public ImagenPublicadorDTO subirImagen(
            Long userId,
            Long actividadId,
            MultipartFile archivo,
            String tipo
    ) {
        PerfilPublicador perfil = buscarPerfil(userId);
        Actividad actividad = buscarActividadPropiaPublicada(actividadId, perfil.getId());

        String tipoNormalizado = validarTipo(tipo);
        byte[] contenido = leerArchivoValidado(archivo);
        String extension = detectarExtension(contenido);

        String rutaObjeto = almacenArchivos.guardarPendiente(
                contenido,
                "actividades/" + actividad.getId(),
                extension
        );

        OffsetDateTime ahora = OffsetDateTime.now();
        Imagen imagen = new Imagen();
        imagen.setActividad(actividad);
        imagen.setUrl(rutaObjeto);
        imagen.setTipoImagen(tipoNormalizado);
        imagen.setOrden(calcularSiguienteOrden(actividad.getId()));
        /*
          activa=false + PENDIENTE: invisible en público hasta que el
          admin apruebe. Además, el archivo vive en el bucket privado:
          tampoco es accesible por URL directa.
        */
        imagen.setActiva(false);
        imagen.setEstadoModeracion(ESTADO_PENDIENTE);
        imagen.setCreatedAt(ahora);
        imagen.setUpdatedAt(ahora);

        Imagen guardada = imagenRepository.save(imagen);

        return aDTO(guardada);
    }

    @Transactional(readOnly = true)
    public List<ImagenPublicadorDTO> listarMias(Long userId, Long actividadId) {
        PerfilPublicador perfil = buscarPerfil(userId);
        Actividad actividad = buscarActividadPropiaPublicada(actividadId, perfil.getId());

        return imagenRepository.findByActividad_IdOrderByCreatedAtDesc(actividad.getId())
                .stream()
                .map(this::aDTO)
                .toList();
    }

    /**
     * Retiro de una imagen propia PENDIENTE: el archivo se elimina del
     * bucket privado y la fila queda como baja lógica con motivo (la
     * tabla imagen no tiene deleted_at).
     */
    @Transactional
    public void eliminarMia(Long userId, Long actividadId, Long imagenId) {
        PerfilPublicador perfil = buscarPerfil(userId);
        Actividad actividad = buscarActividadPropiaPublicada(actividadId, perfil.getId());

        Imagen imagen = imagenRepository.findByIdAndActividad_Id(imagenId, actividad.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la imagen para esta actividad."
                ));

        if (!ESTADO_PENDIENTE.equals(imagen.getEstadoModeracion())) {
            throw new ImagenInvalidaException(
                    "Solo se pueden eliminar imagenes pendientes de revision."
            );
        }

        /*
          Borrado físico best-effort: si el storage falla, la baja
          lógica avanza igual (el objeto queda en el bucket privado,
          inaccesible por URL) y se deja registro en el log.
        */
        try {
            almacenArchivos.eliminar(imagen.getUrl());
        } catch (RuntimeException exception) {
            log.warn(
                    "No se pudo eliminar del almacenamiento la imagen {} ({}).",
                    imagen.getId(),
                    imagen.getUrl(),
                    exception
            );
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        imagen.setActiva(false);
        imagen.setEstadoModeracion(ESTADO_RECHAZADA);
        imagen.setMotivoRechazo(MOTIVO_ELIMINADA_POR_PUBLICADOR);
        imagen.setUpdatedAt(ahora);
        imagenRepository.save(imagen);
    }

    /**
     * DTO con URL visualizable según el estado: pública si está
     * aprobada, firmada temporal si está pendiente (para la preview
     * del publicador), null si fue rechazada (el archivo ya no existe).
     */
    private ImagenPublicadorDTO aDTO(Imagen imagen) {
        ImagenPublicadorDTO dto = ImagenPublicadorDTO.desdeEntidad(imagen);
        dto.setUrl(construirUrlVista(imagen));
        return dto;
    }

    private String construirUrlVista(Imagen imagen) {
        if (ESTADO_APROBADA.equals(imagen.getEstadoModeracion())) {
            return imagen.getUrl();
        }

        if (ESTADO_PENDIENTE.equals(imagen.getEstadoModeracion())
                && almacenArchivos.estaConfigurado()) {
            try {
                return almacenArchivos.firmarUrl(imagen.getUrl(), VALIDEZ_URL_FIRMADA);
            } catch (RuntimeException exception) {
                log.warn("No se pudo firmar la URL de la imagen {}.", imagen.getId(), exception);
                return null;
            }
        }

        return null;
    }

    private PerfilPublicador buscarPerfil(Long userId) {
        if (userId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }

        return perfilPublicadorRepository
                .findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro un perfil publicador para el usuario autenticado."
                ));
    }

    private Actividad buscarActividadPropiaPublicada(Long actividadId, Long perfilId) {
        if (actividadId == null) {
            throw new RecursoNoEncontradoException("No se encontro la actividad.");
        }

        return actividadRepository
                .findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                        actividadId,
                        perfilId,
                        ESTADO_PUBLICACION_PUBLICADA
                )
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro una actividad publicada de tu perfil con ese id."
                ));
    }

    private String validarTipo(String tipo) {
        String tipoNormalizado = tipo != null
                ? tipo.trim().toUpperCase(Locale.ROOT)
                : "";

        if (!TIPOS_PERMITIDOS.contains(tipoNormalizado)) {
            throw new ImagenInvalidaException(
                    "El tipo de imagen es invalido. Valores permitidos: "
                            + String.join(", ", TIPOS_PERMITIDOS) + "."
            );
        }

        return tipoNormalizado;
    }

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

    /**
     * Detecta el formato real por firma de bytes y devuelve la
     * extensión a usar. El content-type declarado no se usa como
     * fuente de verdad porque lo controla el cliente.
     */
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

    private Integer calcularSiguienteOrden(Long actividadId) {
        return imagenRepository.findByActividad_IdOrderByCreatedAtDesc(actividadId).size() + 1;
    }
}
