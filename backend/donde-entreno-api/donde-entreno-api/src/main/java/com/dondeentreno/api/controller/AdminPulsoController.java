package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.PulsoDTO;
import com.dondeentreno.api.service.PulsoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El pulso del producto. Bajo /api/admin/** → exige ADMIN.
 *
 * Solo conteos AGREGADOS: ningún dato de una persona, ningún contenido.
 * Es la pantalla que responde "qué se está usando", no una ventana a
 * lo que la gente escribe — eso sigue siendo privado (ver el inbox y
 * los grupos).
 */
@RestController
@RequestMapping("/api/admin/pulso")
public class AdminPulsoController {

    private final PulsoService pulsoService;

    public AdminPulsoController(PulsoService pulsoService) {
        this.pulsoService = pulsoService;
    }

    @GetMapping
    public PulsoDTO obtener() {
        return pulsoService.obtener();
    }
}
