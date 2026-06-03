package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.PerfilPublicadorDTO;
import com.dondeentreno.api.service.PerfilPublicadorService;
import com.dondeentreno.api.dto.ImagenDTO;
import com.dondeentreno.api.service.ImagenService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller de perfiles publicadores.
 *
 * Esta clase expone endpoints HTTP relacionados con los perfiles
 * que publican actividades en DondeEntreno.
 *
 * Un perfil publicador puede ser:
 * - Club
 * - Gimnasio
 * - Profesor independiente
 * - Institución
 * - Escuela deportiva
 * - Espacio de entrenamiento
 */
@RestController
@RequestMapping("/api/perfiles-publicadores")
public class PerfilPublicadorController {

    private final PerfilPublicadorService perfilPublicadorService;
    private final ImagenService imagenService;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente los services
     * y los entrega a este controller.
     */
    public PerfilPublicadorController(
            PerfilPublicadorService perfilPublicadorService,
            ImagenService imagenService
    ) {
        this.perfilPublicadorService = perfilPublicadorService;
        this.imagenService = imagenService;
    }

    /**
     * Lista perfiles publicadores activos.
     *
     * Este endpoint permite dos formas de uso:
     *
     * 1) Sin filtro:
     * GET http://localhost:8080/api/perfiles-publicadores
     *
     * 2) Filtrando por tipo de publicador:
     * GET http://localhost:8080/api/perfiles-publicadores?tipoPublicador=CLUB
     *
     * Tipos válidos según la base:
     * CLUB
     * GIMNASIO
     * PROFESOR_INDEPENDIENTE
     * INSTITUCION
     * ESCUELA_DEPORTIVA
     * ESPACIO_ENTRENAMIENTO
     *
     * @param tipoPublicador tipo opcional de publicador.
     * @return lista de perfiles activos en formato DTO.
     */
    @GetMapping
    public List<PerfilPublicadorDTO> listarPerfilesPublicadores(
            @RequestParam(required = false) String tipoPublicador
    ) {
        if (tipoPublicador != null && !tipoPublicador.isBlank()) {
            return perfilPublicadorService.obtenerPerfilesActivosPorTipo(tipoPublicador);
        }

        return perfilPublicadorService.obtenerPerfilesActivos();
    }

    /**
     * Obtiene las imágenes activas de un perfil publicador.
     *
     * Este endpoint permite dos formas de uso:
     *
     * 1) Todas las imágenes:
     * GET http://localhost:8080/api/perfiles-publicadores/1/imagenes
     *
     * 2) Filtrando por tipo:
     * GET http://localhost:8080/api/perfiles-publicadores/1/imagenes?tipoImagen=LOGO
     *
     * Tipos posibles según la base:
     * LOGO, PORTADA, PRINCIPAL, GALERIA.
     *
     * @param id ID del perfil publicador.
     * @param tipoImagen tipo opcional de imagen.
     * @return lista de imágenes activas del perfil publicador.
     */
    @GetMapping("/{id}/imagenes")
    public List<ImagenDTO> obtenerImagenesPorPerfilPublicador(
            @PathVariable Long id,
            @RequestParam(required = false) String tipoImagen
    ) {
        if (tipoImagen != null && !tipoImagen.isBlank()) {
            return imagenService.obtenerImagenesPorPerfilPublicadorYTipo(id, tipoImagen);
        }

        return imagenService.obtenerImagenesPorPerfilPublicador(id);
    }
}