package com.dondeentreno.api.dto;

import java.util.List;

/**
 * DTO genérico para respuestas paginadas.
 *
 * Lo usamos para devolver resultados en páginas.
 *
 * Ejemplo:
 * - contenido: lista de actividades
 * - paginaActual: página solicitada
 * - tamanioPagina: cantidad de elementos por página
 * - totalElementos: cantidad total de registros encontrados
 * - totalPaginas: cantidad total de páginas
 * - ultima: indica si es la última página
 *
 * @param <T> tipo de dato que contiene la página.
 */
public class PaginaResponseDTO<T> {

    private List<T> contenido;
    private int paginaActual;
    private int tamanioPagina;
    private long totalElementos;
    private int totalPaginas;
    private boolean ultima;

    public PaginaResponseDTO() {
    }

    public PaginaResponseDTO(
            List<T> contenido,
            int paginaActual,
            int tamanioPagina,
            long totalElementos,
            int totalPaginas,
            boolean ultima
    ) {
        this.contenido = contenido;
        this.paginaActual = paginaActual;
        this.tamanioPagina = tamanioPagina;
        this.totalElementos = totalElementos;
        this.totalPaginas = totalPaginas;
        this.ultima = ultima;
    }

    public List<T> getContenido() {
        return contenido;
    }

    public int getPaginaActual() {
        return paginaActual;
    }

    public int getTamanioPagina() {
        return tamanioPagina;
    }

    public long getTotalElementos() {
        return totalElementos;
    }

    public int getTotalPaginas() {
        return totalPaginas;
    }

    public boolean isUltima() {
        return ultima;
    }

    public void setContenido(List<T> contenido) {
        this.contenido = contenido;
    }

    public void setPaginaActual(int paginaActual) {
        this.paginaActual = paginaActual;
    }

    public void setTamanioPagina(int tamanioPagina) {
        this.tamanioPagina = tamanioPagina;
    }

    public void setTotalElementos(long totalElementos) {
        this.totalElementos = totalElementos;
    }

    public void setTotalPaginas(int totalPaginas) {
        this.totalPaginas = totalPaginas;
    }

    public void setUltima(boolean ultima) {
        this.ultima = ultima;
    }
}
