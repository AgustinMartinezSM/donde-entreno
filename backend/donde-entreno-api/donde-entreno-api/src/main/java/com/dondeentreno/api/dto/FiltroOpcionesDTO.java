package com.dondeentreno.api.dto;

import java.util.List;

/**
 * DTO que agrupa todas las opciones disponibles
 * para armar los filtros del buscador en el frontend.
 *
 * Esto evita que el frontend tenga que pedir:
 * - categorías
 * - deportes
 * - ciudades
 * - barrios
 *
 * en varias peticiones separadas.
 */
public class FiltroOpcionesDTO {

    private List<CategoriaDeportivaDTO> categorias;
    private List<DeporteDTO> deportes;
    private List<CiudadDTO> ciudades;
    private List<BarrioDTO> barrios;

    private List<String> niveles;
    private List<String> modalidades;
    private List<String> ordenes;

    public FiltroOpcionesDTO() {
    }

    public FiltroOpcionesDTO(
            List<CategoriaDeportivaDTO> categorias,
            List<DeporteDTO> deportes,
            List<CiudadDTO> ciudades,
            List<BarrioDTO> barrios,
            List<String> niveles,
            List<String> modalidades,
            List<String> ordenes
    ) {
        this.categorias = categorias;
        this.deportes = deportes;
        this.ciudades = ciudades;
        this.barrios = barrios;
        this.niveles = niveles;
        this.modalidades = modalidades;
        this.ordenes = ordenes;
    }

    public List<CategoriaDeportivaDTO> getCategorias() {
        return categorias;
    }

    public List<DeporteDTO> getDeportes() {
        return deportes;
    }

    public List<CiudadDTO> getCiudades() {
        return ciudades;
    }

    public List<BarrioDTO> getBarrios() {
        return barrios;
    }

    public List<String> getNiveles() {
        return niveles;
    }

    public List<String> getModalidades() {
        return modalidades;
    }

    public List<String> getOrdenes() {
        return ordenes;
    }

    public void setCategorias(List<CategoriaDeportivaDTO> categorias) {
        this.categorias = categorias;
    }

    public void setDeportes(List<DeporteDTO> deportes) {
        this.deportes = deportes;
    }

    public void setCiudades(List<CiudadDTO> ciudades) {
        this.ciudades = ciudades;
    }

    public void setBarrios(List<BarrioDTO> barrios) {
        this.barrios = barrios;
    }

    public void setNiveles(List<String> niveles) {
        this.niveles = niveles;
    }

    public void setModalidades(List<String> modalidades) {
        this.modalidades = modalidades;
    }

    public void setOrdenes(List<String> ordenes) {
        this.ordenes = ordenes;
    }
}
