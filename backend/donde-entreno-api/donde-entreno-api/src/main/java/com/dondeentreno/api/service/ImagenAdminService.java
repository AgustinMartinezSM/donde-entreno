package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ImagenAdminDTO;
import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.ImagenInvalidaException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.storage.AlmacenArchivos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Moderación administrativa de imágenes subidas por publicadores.
 *
 * Aprobar publica el archivo (bucket privado → bucket público de
 * Supabase Storage) y deja la fila visible (activa + APROBADA + URL
 * pública definitiva); si es PRINCIPAL, la PRINCIPAL activa anterior
 * de la misma actividad se desactiva lógicamente. Rechazar elimina el
 * archivo del bucket privado y deja la fila con el motivo.
 */
@Service
public class ImagenAdminService {

    private static final Logger log = LoggerFactory.getLogger(ImagenAdminService.class);

    private static final String ESTADO_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_APROBADA = "APROBADA";
    private static final String ESTADO_RECHAZADA = "RECHAZADA";
    private static final String TIPO_PRINCIPAL = "PRINCIPAL";
    /* Del perfil hay uno solo de cada uno a la vez. */
    private static final List<String> TIPOS_UNICOS_DE_PERFIL = List.of("LOGO", "PORTADA");
    private static final Duration VALIDEZ_URL_FIRMADA = Duration.ofMinutes(10);

    private static final List<String> ESTADOS_PERMITIDOS =
            List.of(ESTADO_PENDIENTE, ESTADO_APROBADA, ESTADO_RECHAZADA);

    private final ImagenRepository imagenRepository;
    private final AlmacenArchivos almacenArchivos;
    private final NotificacionService notificacionService;

    public ImagenAdminService(
            ImagenRepository imagenRepository,
            AlmacenArchivos almacenArchivos,
            NotificacionService notificacionService
    ) {
        this.imagenRepository = imagenRepository;
        this.almacenArchivos = almacenArchivos;
        this.notificacionService = notificacionService;
    }

    /**
     * Cola de moderación, más antiguas primero (FIFO de revisión).
     */
    @Transactional(readOnly = true)
    public PaginaResponseDTO<ImagenAdminDTO> listar(String estado, int page, int size) {
        String estadoFiltro = validarEstadoFiltro(estado);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.ASC, "createdAt")
        );

        Page<Imagen> pagina = estadoFiltro != null
                ? imagenRepository.findByEstadoModeracion(estadoFiltro, pageable)
                : imagenRepository.findAll(pageable);

        List<ImagenAdminDTO> contenido = pagina.getContent().stream()
                .map(this::aDTO)
                .toList();

        return new PaginaResponseDTO<>(
                contenido,
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isLast()
        );
    }

    /**
     * Aprueba una imagen PENDIENTE: publica el archivo en el bucket
     * público y deja la fila activa con la URL definitiva. Si el
     * storage falla, la imagen sigue PENDIENTE y se puede reintentar.
     * Si es PRINCIPAL, desactiva la PRINCIPAL activa anterior de la
     * misma actividad dentro de la misma transacción.
     */
    @Transactional
    public ImagenAdminDTO aprobar(Long imagenId) {
        Imagen imagen = buscarImagen(imagenId);
        validarPendiente(imagen);

        String urlPublica = almacenArchivos.publicar(imagen.getUrl());

        OffsetDateTime ahora = OffsetDateTime.now();

        if (TIPO_PRINCIPAL.equals(imagen.getTipoImagen()) && imagen.getActividad() != null) {
            desactivarAnteriores(
                    imagenRepository.findByActividad_IdAndTipoImagenAndActivaTrue(
                            imagen.getActividad().getId(),
                            TIPO_PRINCIPAL
                    ),
                    imagen,
                    ahora
            );
        }

        /*
          El perfil tiene un solo logo y una sola portada a la vez: al
          aprobar uno nuevo, el anterior del mismo tipo se desactiva,
          igual que la PRINCIPAL de una actividad.
        */
        if (TIPOS_UNICOS_DE_PERFIL.contains(imagen.getTipoImagen())
                && imagen.getPerfilPublicador() != null) {
            desactivarAnteriores(
                    imagenRepository.findByPerfilPublicador_IdAndTipoImagenAndActivaTrue(
                            imagen.getPerfilPublicador().getId(),
                            imagen.getTipoImagen()
                    ),
                    imagen,
                    ahora
            );
        }

        imagen.setEstadoModeracion(ESTADO_APROBADA);
        imagen.setActiva(true);
        imagen.setUrl(urlPublica);
        imagen.setMotivoRechazo(null);
        imagen.setUpdatedAt(ahora);

        Imagen guardada = imagenRepository.save(imagen);

        /* Aviso al publicador (Fase 2 social), best-effort. */
        notificarDuenio(guardada, "FOTO_APROBADA", "Tu foto fue aprobada y ya se ve publicada.");

        return aDTO(guardada);
    }

    /*
      Resuelve al dueño de la imagen (actividad o perfil) y le avisa.
      La emisión es best-effort dentro de NotificacionService.
    */
    private void notificarDuenio(Imagen imagen, String tipo, String titulo) {
        Long duenioId = null;
        String ruta = "/publicador/fotos";

        if (imagen.getActividad() != null
                && imagen.getActividad().getPerfilPublicador() != null
                && imagen.getActividad().getPerfilPublicador().getUsuario() != null) {
            duenioId = imagen.getActividad().getPerfilPublicador().getUsuario().getId();
            ruta = "/publicador/actividades/" + imagen.getActividad().getId();
        } else if (imagen.getPerfilPublicador() != null
                && imagen.getPerfilPublicador().getUsuario() != null) {
            duenioId = imagen.getPerfilPublicador().getUsuario().getId();
        }

        if (duenioId != null) {
            notificacionService.emitir(duenioId, tipo, titulo, ruta);
        }
    }

    /*
      Baja lógica de las imágenes que la recién aprobada reemplaza.
    */
    private void desactivarAnteriores(
            List<Imagen> anteriores,
            Imagen aprobada,
            OffsetDateTime ahora
    ) {
        for (Imagen anterior : anteriores) {
            if (!anterior.getId().equals(aprobada.getId())) {
                anterior.setActiva(false);
                anterior.setUpdatedAt(ahora);
                imagenRepository.save(anterior);
            }
        }
    }

    /**
     * Rechaza una imagen PENDIENTE con motivo obligatorio (visible
     * para el publicador). El archivo se elimina del bucket privado.
     */
    @Transactional
    public ImagenAdminDTO rechazar(Long imagenId, String motivo) {
        Imagen imagen = buscarImagen(imagenId);
        validarPendiente(imagen);

        String motivoLimpio = motivo != null ? motivo.trim() : "";

        if (motivoLimpio.isEmpty()) {
            throw new ImagenInvalidaException(
                    "Para rechazar una imagen hay que indicar el motivo."
            );
        }

        /*
          Borrado físico best-effort: si el storage falla, el rechazo
          avanza igual (el objeto queda en el bucket privado,
          inaccesible por URL) y se deja registro en el log.
        */
        try {
            almacenArchivos.eliminar(imagen.getUrl());
        } catch (RuntimeException exception) {
            log.warn(
                    "No se pudo eliminar del almacenamiento la imagen rechazada {} ({}).",
                    imagen.getId(),
                    imagen.getUrl(),
                    exception
            );
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        imagen.setEstadoModeracion(ESTADO_RECHAZADA);
        imagen.setActiva(false);
        imagen.setMotivoRechazo(motivoLimpio);
        imagen.setUpdatedAt(ahora);

        Imagen guardada = imagenRepository.save(imagen);

        /* Aviso al publicador (Fase 2 social), best-effort. */
        notificarDuenio(guardada, "FOTO_RECHAZADA", "Una foto tuya fue rechazada: " + motivoLimpio);

        return aDTO(guardada);
    }

    /**
     * DTO con URL visualizable: la cola de moderación necesita VER las
     * pendientes, que viven en el bucket privado → URL firmada temporal.
     * Las aprobadas ya tienen URL pública; las rechazadas no tienen
     * archivo.
     */
    private ImagenAdminDTO aDTO(Imagen imagen) {
        ImagenAdminDTO dto = ImagenAdminDTO.desdeEntidad(imagen);
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

    private Imagen buscarImagen(Long imagenId) {
        if (imagenId == null) {
            throw new RecursoNoEncontradoException("No se encontro la imagen.");
        }

        return imagenRepository.findById(imagenId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la imagen."
                ));
    }

    private void validarPendiente(Imagen imagen) {
        if (!ESTADO_PENDIENTE.equals(imagen.getEstadoModeracion())) {
            throw new ImagenInvalidaException(
                    "La imagen ya fue revisada y no puede modificarse."
            );
        }
    }

    private String validarEstadoFiltro(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }

        String estadoNormalizado = estado.trim().toUpperCase(Locale.ROOT);

        if (!ESTADOS_PERMITIDOS.contains(estadoNormalizado)) {
            throw new FiltroInvalidoException(
                    "El parametro 'estado' tiene un valor invalido: '" + estado
                            + "'. Valores permitidos: "
                            + String.join(", ", ESTADOS_PERMITIDOS) + "."
            );
        }

        return estadoNormalizado;
    }
}
