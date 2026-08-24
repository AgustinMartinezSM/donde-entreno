package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.ReporteAdminDTO;
import com.dondeentreno.api.service.ReporteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Cola de reportes para el admin (script 28, Fase 2 social). El
 * acceso lo corta SecurityConfig: /api/admin/** exige ADMIN o
 * SUPER_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/reportes")
public class AdminReportesController {

    private final ReporteService reporteService;

    public AdminReportesController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping
    public PaginaResponseDTO<ReporteAdminDTO> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reporteService.listarParaAdmin(estado, page, size);
    }

    @GetMapping("/contador")
    public Map<String, Long> contarPendientes() {
        return Map.of("pendientes", reporteService.contarPendientes());
    }

    @PatchMapping("/{id}/estado")
    public ReporteAdminDTO cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> cuerpo
    ) {
        return reporteService.cambiarEstado(id, cuerpo.get("estado"));
    }
}
