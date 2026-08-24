package com.dondeentreno.api.service;

import com.dondeentreno.api.config.MediaProperties;
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

    /** Secciones de galería (script 30): catálogo fijo, "" = General. */
    public static final List<String> SECCIONES_PERMITIDAS =
            List.of("INSTALACIONES", "ENTRENAMIENTOS", "EVENTOS", "EQUIPO");
    static final List<String> TIPOS_PERFIL_PERMITIDOS = List.of("LOGO", "PORTADA");
    private static final String MOTIVO_ELIMINADA_POR_PUBLICADOR =
            "Eliminada por el publicador antes de la revision.";
    private static final Duration VALIDEZ_URL_FIRMADA = Duration.ofMinutes(10);

    private static final String TIPO_PRINCIPAL = "PRINCIPAL";
    private static final String TIPO_GALERIA = "GALERIA";

    private final ImagenRepository imagenRepository;
    private final PerfilPublicadorRepository perfilPublicadorRepository;
    private final ActividadRepository actividadRepository;
    private final AlmacenArchivos almacenArchivos;
    private final MediaProperties mediaProperties;
    /* Fase 6: hasta ahora esta clase no emitía nada a ningún lado. */
    private final FeedEventService feedEventService;

    public ImagenPublicadorService(
            ImagenRepository imagenRepository,
            PerfilPublicadorRepository perfilPublicadorRepository,
            ActividadRepository actividadRepository,
            AlmacenArchivos almacenArchivos,
            MediaProperties mediaProperties,
            FeedEventService feedEventService
    ) {
        this.imagenRepository = imagenRepository;
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.actividadRepository = actividadRepository;
        this.almacenArchivos = almacenArchivos;
        this.mediaProperties = mediaProperties;
        this.feedEventService = feedEventService;
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
        validarLimitesDeActividad(actividad.getId(), tipoNormalizado);
        byte[] contenido = leerArchivoValidado(archivo);
        String extension = detectarExtension(contenido);

        /*
          Subida DIRECTA (Fase 4, filosofía de moderación flexible): el
          archivo se guarda y se publica en el mismo paso — la foto se
          ve al instante y la moderación pasa a ser reactiva (reportes
          + admin oculta). Se reusan los dos primitivos ya probados del
          storage; si publicar falla, la transacción cae entera.
        */
        String rutaObjeto = almacenArchivos.guardarPendiente(
                contenido,
                "actividades/" + actividad.getId(),
                extension
        );
        String urlPublica = almacenArchivos.publicar(rutaObjeto);

        OffsetDateTime ahora = OffsetDateTime.now();

        /* La PRINCIPAL nueva reemplaza a la anterior (antes lo hacía la aprobación). */
        if ("PRINCIPAL".equals(tipoNormalizado)) {
            desactivarAnterioresDeActividad(actividad.getId(), "PRINCIPAL", ahora);
        }
        Imagen imagen = new Imagen();
        imagen.setActividad(actividad);
        imagen.setUrl(urlPublica);
        imagen.setTipoImagen(tipoNormalizado);
        imagen.setOrden(calcularSiguienteOrden(actividad.getId()));
        imagen.setActiva(true);
        imagen.setEstadoModeracion(ESTADO_APROBADA);
        imagen.setCreatedAt(ahora);
        imagen.setUpdatedAt(ahora);

        Imagen guardada = imagenRepository.save(imagen);

        /*
          Feed (Fase 6): una foto nueva es un hecho que los seguidores
          quieren ver. Best-effort: si falla, la foto igual quedó
          publicada.
        */
        if (actividad.getPerfilPublicador() != null) {
            feedEventService.emitir(
                    FeedEventService.TIPO_FOTOS_NUEVAS,
                    actividad.getPerfilPublicador().getId(),
                    actividad.getId(),
                    guardada.getId(),
                    null
            );
        }

        return aDTO(guardada);
    }

    /**
     * Sube el logo o la portada del perfil propio.
     *
     * Mismo circuito que las imágenes de actividad: nace PENDIENTE, con
     * activa=false y el archivo en el bucket privado, y solo se ve en
     * público cuando un admin la aprueba. La diferencia es el dueño: la
     * fila cuelga del perfil (la constraint chk_imagen_duenio_unico
     * exige exactamente uno de actividad o perfil).
     */
    @Transactional
    public ImagenPublicadorDTO subirImagenDePerfil(
            Long userId,
            MultipartFile archivo,
            String tipo
    ) {
        PerfilPublicador perfil = buscarPerfil(userId);

        String tipoNormalizado = validarTipoDePerfil(tipo);

        byte[] contenido = leerArchivoValidado(archivo);
        String extension = detectarExtension(contenido);

        /* Subida DIRECTA (Fase 4): igual que las fotos de actividad. */
        String rutaObjeto = almacenArchivos.guardarPendiente(
                contenido,
                "perfiles/" + perfil.getId(),
                extension
        );
        String urlPublica = almacenArchivos.publicar(rutaObjeto);

        OffsetDateTime ahora = OffsetDateTime.now();

        /*
          El perfil tiene UN logo y UNA portada: el nuevo desactiva al
          anterior del mismo tipo (antes lo hacía la aprobación).
        */
        desactivarAnterioresDePerfil(perfil.getId(), tipoNormalizado, ahora);

        Imagen imagen = new Imagen();
        imagen.setPerfilPublicador(perfil);
        imagen.setUrl(urlPublica);
        imagen.setTipoImagen(tipoNormalizado);
        imagen.setOrden(0);
        imagen.setActiva(true);
        imagen.setEstadoModeracion(ESTADO_APROBADA);
        imagen.setCreatedAt(ahora);
        imagen.setUpdatedAt(ahora);

        return aDTO(imagenRepository.save(imagen));
    }

    /* Baja lógica de las que la recién subida reemplaza (Fase 4). */
    private void desactivarAnterioresDeActividad(
            Long actividadId,
            String tipoImagen,
            OffsetDateTime ahora
    ) {
        for (Imagen anterior : imagenRepository
                .findByActividad_IdAndTipoImagenAndActivaTrue(actividadId, tipoImagen)) {
            anterior.setActiva(false);
            anterior.setUpdatedAt(ahora);
            imagenRepository.save(anterior);
        }
    }

    private void desactivarAnterioresDePerfil(
            Long perfilId,
            String tipoImagen,
            OffsetDateTime ahora
    ) {
        for (Imagen anterior : imagenRepository
                .findByPerfilPublicador_IdAndTipoImagenAndActivaTrue(perfilId, tipoImagen)) {
            anterior.setActiva(false);
            anterior.setUpdatedAt(ahora);
            imagenRepository.save(anterior);
        }
    }

    @Transactional(readOnly = true)
    public List<ImagenPublicadorDTO> listarMiasDePerfil(Long userId) {
        PerfilPublicador perfil = buscarPerfil(userId);

        return imagenRepository
                .findByPerfilPublicador_IdOrderByCreatedAtDesc(perfil.getId())
                .stream()
                .map(this::aDTO)
                .toList();
    }

    /**
     * Eliminación de una imagen propia del perfil: pendiente (retiro
     * antes de la moderación) o aprobada (baja lógica, fase 2).
     */
    @Transactional
    public void eliminarMiaDePerfil(Long userId, Long imagenId) {
        PerfilPublicador perfil = buscarPerfil(userId);

        Imagen imagen = imagenRepository
                .findByIdAndPerfilPublicador_Id(imagenId, perfil.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la imagen para este perfil."
                ));

        eliminarImagenPropia(imagen);
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
     * Eliminación de una imagen propia de actividad: pendiente (retiro
     * antes de la moderación) o aprobada (baja lógica, fase 2).
     */
    @Transactional
    public void eliminarMia(Long userId, Long actividadId, Long imagenId) {
        PerfilPublicador perfil = buscarPerfil(userId);
        Actividad actividad = buscarActividadPropiaPublicada(actividadId, perfil.getId());

        Imagen imagen = imagenRepository.findByIdAndActividad_Id(imagenId, actividad.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la imagen para esta actividad."
                ));

        eliminarImagenPropia(imagen);
    }

    /*
      Eliminación común a imágenes de actividad y de perfil. Dos casos
      con destinos distintos:
      - PENDIENTE: retiro antes de la moderación (comportamiento
        histórico) — el archivo sale del bucket privado y la fila queda
        RECHAZADA con motivo.
      - APROBADA y activa: baja lógica (fase 2) — la fila queda APROBADA
        con activa=false (las vistas públicas filtran por activa) y el
        archivo del bucket público se borra best-effort.
      Rechazadas o ya eliminadas: no hay nada que eliminar.
    */
    private void eliminarImagenPropia(Imagen imagen) {
        if (ESTADO_PENDIENTE.equals(imagen.getEstadoModeracion())) {
            retirarPendiente(imagen);
            return;
        }

        if (ESTADO_APROBADA.equals(imagen.getEstadoModeracion())
                && Boolean.TRUE.equals(imagen.getActiva())) {
            eliminarAprobada(imagen);
            return;
        }

        throw new ImagenInvalidaException(
                "La imagen ya fue eliminada o rechazada: no hay nada que eliminar."
        );
    }

    /*
      Baja lógica de una aprobada. El borrado del archivo público es
      best-effort a propósito (decisión del plan de fase 2): si el
      storage falla, la imagen igual deja de verse (activa=false) y el
      objeto huérfano se loguea; el CDN puede retener la copia un rato.
    */
    private void eliminarAprobada(Imagen imagen) {
        try {
            almacenArchivos.eliminarPublicoPorUrl(imagen.getUrl());
        } catch (RuntimeException exception) {
            log.warn(
                    "No se pudo eliminar del bucket publico la imagen {} ({}).",
                    imagen.getId(),
                    imagen.getUrl(),
                    exception
            );
        }

        imagen.setActiva(false);
        imagen.setUpdatedAt(OffsetDateTime.now());
        imagenRepository.save(imagen);
    }

    /*
      Retiro de pendientes: deja la fila como baja lógica con motivo (la
      tabla imagen no tiene deleted_at).
    */
    private void retirarPendiente(Imagen imagen) {
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
     * Orden manual de la galería (fase 2): la lista debe traer
     * EXACTAMENTE los ids de todas las GALERIA activas de la actividad,
     * en el orden deseado; se asigna orden 1..n. Las vistas públicas ya
     * ordenan por este campo, así que no hay nada más que tocar.
     */
    @Transactional
    public void ordenarGaleria(Long userId, Long actividadId, List<Long> imagenIds) {
        PerfilPublicador perfil = buscarPerfil(userId);
        Actividad actividad = buscarActividadPropiaPublicada(actividadId, perfil.getId());

        List<Imagen> galeria = imagenRepository
                .findByActividad_IdAndTipoImagenAndActivaTrue(actividad.getId(), TIPO_GALERIA);

        java.util.Set<Long> idsActuales = new java.util.HashSet<>();
        for (Imagen imagen : galeria) {
            idsActuales.add(imagen.getId());
        }

        if (imagenIds == null
                || imagenIds.size() != galeria.size()
                || !idsActuales.equals(new java.util.HashSet<>(imagenIds))) {
            throw new ImagenInvalidaException(
                    "La lista debe incluir exactamente todas las fotos de la galeria, sin repetir."
            );
        }

        OffsetDateTime ahora = OffsetDateTime.now();

        for (Imagen imagen : galeria) {
            int posicion = imagenIds.indexOf(imagen.getId()) + 1;
            imagen.setOrden(posicion);
            imagen.setUpdatedAt(ahora);
        }

        imagenRepository.saveAll(galeria);
    }

    /**
     * Promueve una foto APROBADA de la galería a PRINCIPAL (fase 2), sin
     * re-moderación: el archivo ya fue aprobado y cambiarle el rol no
     * cambia su contenido. Es un swap: la PRINCIPAL vigente baja a la
     * galería (al final del orden) — nada se desactiva ni se pierde.
     */
    @Transactional
    public void elegirPrincipal(Long userId, Long actividadId, Long imagenId) {
        PerfilPublicador perfil = buscarPerfil(userId);
        Actividad actividad = buscarActividadPropiaPublicada(actividadId, perfil.getId());

        Imagen elegida = imagenRepository.findByIdAndActividad_Id(imagenId, actividad.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la imagen para esta actividad."
                ));

        if (!ESTADO_APROBADA.equals(elegida.getEstadoModeracion())
                || !Boolean.TRUE.equals(elegida.getActiva())
                || !TIPO_GALERIA.equals(elegida.getTipoImagen())) {
            throw new ImagenInvalidaException(
                    "Solo una foto aprobada de la galeria puede pasar a ser la principal."
            );
        }

        List<Imagen> galeriaActiva = imagenRepository
                .findByActividad_IdAndTipoImagenAndActivaTrue(actividad.getId(), TIPO_GALERIA);
        int colaDeGaleria = 1;
        for (Imagen imagen : galeriaActiva) {
            if (imagen.getOrden() != null && imagen.getOrden() >= colaDeGaleria) {
                colaDeGaleria = imagen.getOrden() + 1;
            }
        }

        OffsetDateTime ahora = OffsetDateTime.now();

        List<Imagen> principalesVigentes = imagenRepository
                .findByActividad_IdAndTipoImagenAndActivaTrue(actividad.getId(), TIPO_PRINCIPAL);
        for (Imagen anterior : principalesVigentes) {
            anterior.setTipoImagen(TIPO_GALERIA);
            anterior.setOrden(colaDeGaleria++);
            anterior.setUpdatedAt(ahora);
        }
        imagenRepository.saveAll(principalesVigentes);

        elegida.setTipoImagen(TIPO_PRINCIPAL);
        elegida.setOrden(0);
        elegida.setUpdatedAt(ahora);
        imagenRepository.save(elegida);
    }

    /**
     * Título y descripción de una imagen propia (fase 2): alimentan el
     * texto alternativo/epígrafe de las vistas públicas. Sin moderación
     * (decisión del plan): texto plano que nunca se renderiza como
     * link, con el mismo nivel de confianza que la descripción del
     * perfil. Semántica PATCH: null no toca, vacío limpia.
     */
    @Transactional
    public ImagenPublicadorDTO actualizarTexto(
            Long userId,
            Long actividadId,
            Long imagenId,
            String titulo,
            String descripcion,
            String seccion,
            Boolean comentariosActivados
    ) {
        PerfilPublicador perfil = buscarPerfil(userId);
        Actividad actividad = buscarActividadPropiaPublicada(actividadId, perfil.getId());

        Imagen imagen = imagenRepository.findByIdAndActividad_Id(imagenId, actividad.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la imagen para esta actividad."
                ));

        boolean eliminada = ESTADO_APROBADA.equals(imagen.getEstadoModeracion())
                && !Boolean.TRUE.equals(imagen.getActiva());

        if (ESTADO_RECHAZADA.equals(imagen.getEstadoModeracion()) || eliminada) {
            throw new ImagenInvalidaException(
                    "La imagen fue rechazada o eliminada: no tiene texto que editar."
            );
        }

        if (titulo != null) {
            imagen.setTitulo(normalizarTextoDeImagen(titulo, 150));
        }

        if (descripcion != null) {
            imagen.setDescripcion(normalizarTextoDeImagen(descripcion, 255));
        }

        /* Fase 4: sección de galería y toggle de comentarios. */
        if (seccion != null) {
            String seccionLimpia = seccion.trim();
            if (seccionLimpia.isEmpty()) {
                imagen.setSeccion(null);
            } else if (SECCIONES_PERMITIDAS.contains(seccionLimpia)) {
                imagen.setSeccion(seccionLimpia);
            } else {
                throw new ImagenInvalidaException("La seccion de galeria no es valida.");
            }
        }

        if (comentariosActivados != null) {
            imagen.setComentariosActivados(comentariosActivados);
        }

        imagen.setUpdatedAt(OffsetDateTime.now());

        return aDTO(imagenRepository.save(imagen));
    }

    /* Trim + vacío→null; el largo ya lo acota Bean Validation en el DTO. */
    private String normalizarTextoDeImagen(String valor, int maximo) {
        String limpio = valor.trim();

        if (limpio.isEmpty()) {
            return null;
        }

        if (limpio.length() > maximo) {
            throw new ImagenInvalidaException(
                    "El texto supera el maximo de " + maximo + " caracteres."
            );
        }

        return limpio;
    }

    /*
      Límites de subida por actividad (fase 2; antes no había NINGUNO):
      un tope anti-flood de pendientes en la cola y un máximo de fotos
      de galería "que van a existir" (activas + pendientes).
    */
    private void validarLimitesDeActividad(Long actividadId, String tipoNormalizado) {
        long pendientes = imagenRepository
                .countByActividad_IdAndEstadoModeracion(actividadId, ESTADO_PENDIENTE);

        if (pendientes >= mediaProperties.getMaxPendientesPorActividad()) {
            throw new ImagenInvalidaException(
                    "Esta actividad ya tiene " + pendientes
                            + " imagenes esperando revision. Espera la moderacion antes de subir mas."
            );
        }

        if (TIPO_GALERIA.equals(tipoNormalizado)) {
            long galeriaExistente = imagenRepository
                    .countByActividad_IdAndTipoImagenAndActivaTrue(actividadId, TIPO_GALERIA)
                    + imagenRepository.countByActividad_IdAndTipoImagenAndEstadoModeracion(
                            actividadId, TIPO_GALERIA, ESTADO_PENDIENTE);

            if (galeriaExistente >= mediaProperties.getMaxGaleriaPorActividad()) {
                throw new ImagenInvalidaException(
                        "La galeria admite hasta "
                                + mediaProperties.getMaxGaleriaPorActividad()
                                + " fotos por actividad. Elimina alguna para subir otra."
                );
            }
        }
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

        /*
          Publicada O pausada (fase 6): pausar oculta al publico, no
          congela la gestion — las fotos se siguen administrando.
        */
        return actividadRepository
                .findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionInAndDeletedAtIsNull(
                        actividadId,
                        perfilId,
                        PublicadorActividadService.ESTADOS_DEL_PANEL
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

    /**
     * Tipos válidos para una imagen de perfil. El perfil no tiene
     * PRINCIPAL ni GALERIA: tiene logo y portada, que son piezas de
     * identidad y hay una sola de cada una a la vez.
     */
    private String validarTipoDePerfil(String tipo) {
        String tipoNormalizado = tipo != null
                ? tipo.trim().toUpperCase(Locale.ROOT)
                : "";

        if (!TIPOS_PERFIL_PERMITIDOS.contains(tipoNormalizado)) {
            throw new ImagenInvalidaException(
                    "El tipo de imagen es invalido. Valores permitidos: "
                            + String.join(", ", TIPOS_PERFIL_PERMITIDOS) + "."
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

    /*
      Contador monotónico: max(orden)+1 sobre TODAS las filas de la
      actividad. Antes era size()+1, que tras retirar filas intermedias
      podía repetir valores y romper el orden estable de la galería.
    */
    private Integer calcularSiguienteOrden(Long actividadId) {
        int maximo = 0;

        for (Imagen imagen : imagenRepository.findByActividad_IdOrderByCreatedAtDesc(actividadId)) {
            if (imagen.getOrden() != null && imagen.getOrden() > maximo) {
                maximo = imagen.getOrden();
            }
        }

        return maximo + 1;
    }
}
