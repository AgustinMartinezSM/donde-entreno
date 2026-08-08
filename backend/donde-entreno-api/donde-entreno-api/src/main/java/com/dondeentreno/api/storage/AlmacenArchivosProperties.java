package com.dondeentreno.api.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del almacenamiento en Supabase Storage.
 *
 * Los valores llegan por variables de entorno (ver
 * application.properties); los buckets tienen defaults razonables.
 * URL y service key vacíos = almacenamiento no configurado: la app
 * arranca igual y las operaciones de storage responden 503.
 */
@ConfigurationProperties(prefix = "dondeentreno.storage.supabase")
public class AlmacenArchivosProperties {

    /** URL del proyecto Supabase (https://<ref>.supabase.co). */
    private String url = "";

    /** Service role key (solo backend; jamás en el frontend). */
    private String serviceKey = "";

    /** Bucket privado donde nacen las subidas pendientes. */
    private String bucketPendientes = "imagenes-pendientes";

    /** Bucket público desde donde se sirven las aprobadas. */
    private String bucketPublicas = "imagenes-publicas";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    public String getBucketPendientes() {
        return bucketPendientes;
    }

    public void setBucketPendientes(String bucketPendientes) {
        this.bucketPendientes = bucketPendientes;
    }

    public String getBucketPublicas() {
        return bucketPublicas;
    }

    public void setBucketPublicas(String bucketPublicas) {
        this.bucketPublicas = bucketPublicas;
    }
}
