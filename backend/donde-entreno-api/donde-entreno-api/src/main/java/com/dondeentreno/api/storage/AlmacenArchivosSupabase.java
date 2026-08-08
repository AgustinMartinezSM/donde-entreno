package com.dondeentreno.api.storage;

import com.dondeentreno.api.exception.AlmacenNoConfiguradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Implementación de AlmacenArchivos contra la Storage API de Supabase
 * (HTTP puro con RestClient: sin SDK ni dependencias nuevas).
 *
 * Autenticación: service role key en Authorization + apikey (solo vive
 * en el backend, referenciada por nombre de variable de entorno).
 *
 * Seguridad de rutas: el nombre del archivo SIEMPRE se genera (UUID),
 * nunca se usa el nombre original; la carpeta se valida con lista
 * blanca de caracteres (sin "..", sin barras dobles ni backslashes).
 */
public class AlmacenArchivosSupabase implements AlmacenArchivos {

    private static final Logger log = LoggerFactory.getLogger(AlmacenArchivosSupabase.class);

    private static final Pattern CARPETA_SEGURA =
            Pattern.compile("^[a-z0-9-]+(/[a-z0-9-]+)*$");

    private static final Pattern EXTENSION_SEGURA =
            Pattern.compile("^[a-z0-9]{2,5}$");

    private static final Pattern RUTA_OBJETO_SEGURA =
            Pattern.compile("^[a-z0-9-]+(/[a-z0-9-]+)*/[a-f0-9-]+\\.[a-z0-9]{2,5}$");

    private final AlmacenArchivosProperties propiedades;
    private final RestClient restClient;

    public AlmacenArchivosSupabase(
            AlmacenArchivosProperties propiedades,
            RestClient.Builder restClientBuilder
    ) {
        this.propiedades = propiedades;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public boolean estaConfigurado() {
        return !propiedades.getUrl().isBlank()
                && !propiedades.getServiceKey().isBlank();
    }

    @Override
    public String guardarPendiente(byte[] contenido, String carpetaRelativa, String extension) {
        exigirConfigurado();

        if (contenido == null || contenido.length == 0) {
            throw new IllegalArgumentException("El archivo esta vacio.");
        }

        if (carpetaRelativa == null || !CARPETA_SEGURA.matcher(carpetaRelativa).matches()) {
            throw new IllegalArgumentException(
                    "Carpeta invalida para guardar el archivo: " + carpetaRelativa
            );
        }

        if (extension == null || !EXTENSION_SEGURA.matcher(extension).matches()) {
            throw new IllegalArgumentException(
                    "Extension invalida para guardar el archivo: " + extension
            );
        }

        String rutaObjeto = carpetaRelativa + "/" + UUID.randomUUID() + "." + extension;

        try {
            restClient.post()
                    .uri(urlObjeto(propiedades.getBucketPendientes(), rutaObjeto))
                    .headers(this::agregarAutenticacion)
                    .contentType(mediaTypePorExtension(extension))
                    .body(contenido)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "No se pudo guardar el archivo en el almacenamiento.",
                    exception
            );
        }

        return rutaObjeto;
    }

    @Override
    public String publicar(String rutaObjeto) {
        exigirConfigurado();
        validarRutaObjeto(rutaObjeto);

        /*
          Copy privado→público y recién después el delete del privado.
          Si la copia falla, no se tocó nada y la aprobación es
          reintentable. Si falla solo el delete, queda un residuo en el
          bucket privado (inaccesible por URL): se loguea y no bloquea.
        */
        try {
            restClient.post()
                    .uri(propiedades.getUrl() + "/storage/v1/object/copy")
                    .headers(this::agregarAutenticacion)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "bucketId", propiedades.getBucketPendientes(),
                            "sourceKey", rutaObjeto,
                            "destinationBucket", propiedades.getBucketPublicas(),
                            "destinationKey", rutaObjeto
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "No se pudo publicar el archivo aprobado.",
                    exception
            );
        }

        try {
            eliminar(rutaObjeto);
        } catch (RuntimeException exception) {
            log.warn(
                    "La imagen {} se publico pero no se pudo retirar del bucket privado.",
                    rutaObjeto,
                    exception
            );
        }

        return urlPublica(rutaObjeto);
    }

    @Override
    public String firmarUrl(String rutaObjeto, Duration validez) {
        exigirConfigurado();
        validarRutaObjeto(rutaObjeto);

        try {
            RespuestaFirma respuesta = restClient.post()
                    .uri(propiedades.getUrl() + "/storage/v1/object/sign/"
                            + propiedades.getBucketPendientes() + "/" + rutaObjeto)
                    .headers(this::agregarAutenticacion)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", Math.max(60, validez.toSeconds())))
                    .retrieve()
                    .body(RespuestaFirma.class);

            if (respuesta == null || respuesta.signedURL() == null) {
                throw new IllegalStateException("La firma de URL no devolvio resultado.");
            }

            return propiedades.getUrl() + "/storage/v1" + respuesta.signedURL();
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "No se pudo firmar la URL de la imagen pendiente.",
                    exception
            );
        }
    }

    @Override
    public void eliminar(String rutaObjeto) {
        exigirConfigurado();
        validarRutaObjeto(rutaObjeto);

        try {
            restClient.delete()
                    .uri(urlObjeto(propiedades.getBucketPendientes(), rutaObjeto))
                    .headers(this::agregarAutenticacion)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "No se pudo eliminar el archivo del almacenamiento.",
                    exception
            );
        }
    }

    private void exigirConfigurado() {
        if (!estaConfigurado()) {
            throw new AlmacenNoConfiguradoException();
        }
    }

    private void validarRutaObjeto(String rutaObjeto) {
        if (rutaObjeto == null || !RUTA_OBJETO_SEGURA.matcher(rutaObjeto).matches()) {
            throw new IllegalArgumentException(
                    "Ruta de objeto invalida: " + rutaObjeto
            );
        }
    }

    private void agregarAutenticacion(org.springframework.http.HttpHeaders headers) {
        headers.setBearerAuth(propiedades.getServiceKey());
        headers.set("apikey", propiedades.getServiceKey());
    }

    private String urlObjeto(String bucket, String rutaObjeto) {
        return propiedades.getUrl() + "/storage/v1/object/" + bucket + "/" + rutaObjeto;
    }

    private String urlPublica(String rutaObjeto) {
        return propiedades.getUrl() + "/storage/v1/object/public/"
                + propiedades.getBucketPublicas() + "/" + rutaObjeto;
    }

    private MediaType mediaTypePorExtension(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    /** Respuesta de POST /object/sign: {"signedURL": "/object/sign/..."} */
    private record RespuestaFirma(String signedURL) {
    }
}
