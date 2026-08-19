package com.dondeentreno.api.storage;

import java.time.Duration;

/**
 * Abstracción de almacenamiento de archivos subidos.
 *
 * Modelo de dos espacios alineado con la moderación:
 * - Las subidas nacen en un espacio PRIVADO (invisible por URL): una
 *   imagen pendiente o rechazada no se puede ver desde afuera.
 * - Al aprobarse, el archivo se publica en un espacio PÚBLICO y desde
 *   ahí se sirve con una URL absoluta cacheable.
 *
 * La implementación productiva es Supabase Storage
 * (AlmacenArchivosSupabase); los tests usan una implementación en
 * memoria. Nunca disco local: el filesystem de Render es efímero y
 * servir archivos sin consultar la base saltea la moderación.
 */
public interface AlmacenArchivos {

    /**
     * Indica si el almacenamiento está configurado en este entorno.
     * Sin configuración, el resto de las operaciones lanza
     * AlmacenNoConfiguradoException.
     */
    boolean estaConfigurado();

    /**
     * Guarda el contenido en el espacio privado (pendiente de
     * moderación) y devuelve la ruta interna del objeto
     * (por ejemplo "actividades/12/uuid.jpg").
     *
     * @param contenido bytes del archivo ya validados por el caller.
     * @param carpetaRelativa carpeta lógica (por ejemplo "actividades/12");
     *                        solo letras, números, guiones y barras simples.
     * @param extension extensión SIN punto (jpg, png, webp), ya validada.
     * @return ruta interna del objeto en el espacio privado.
     */
    String guardarPendiente(byte[] contenido, String carpetaRelativa, String extension);

    /**
     * Publica un objeto pendiente: lo copia al espacio público y lo
     * retira del privado. Devuelve la URL pública absoluta definitiva.
     * Si falla, el objeto sigue pendiente y la operación es reintentable.
     */
    String publicar(String rutaObjeto);

    /**
     * URL firmada de lectura temporal para un objeto del espacio
     * privado (la usan el admin en la cola de moderación y el
     * publicador para previsualizar sus pendientes).
     */
    String firmarUrl(String rutaObjeto, Duration validez);

    /**
     * Elimina físicamente un objeto del espacio privado (rechazo del
     * admin o retiro del publicador). La fila en la base queda como
     * baja lógica con motivo; el archivo no se conserva.
     */
    void eliminar(String rutaObjeto);

    /**
     * Elimina físicamente un objeto del espacio PÚBLICO a partir de la
     * URL pública que quedó guardada al aprobarse (imagen.url). Lo usa
     * la eliminación de imágenes aprobadas por el publicador; el caller
     * lo trata como best-effort (la baja lógica en la base avanza
     * aunque esto falle) y el CDN puede retener la copia un tiempo.
     * Si la URL no pertenece a este almacenamiento, lanza
     * IllegalArgumentException.
     */
    void eliminarPublicoPorUrl(String urlPublica);
}
