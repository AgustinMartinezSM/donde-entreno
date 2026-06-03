package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.HorarioActividadDTO;
import com.dondeentreno.api.entity.HorarioActividad;
import com.dondeentreno.api.mapper.HorarioActividadMapper;
import com.dondeentreno.api.repository.HorarioActividadRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service de HorarioActividad.
 * Esta capa contiene la lógica relacionada con los horarios
 * de las actividades deportivas.
 */
@Service
public class HorarioActividadService {

    private final HorarioActividadRepository horarioActividadRepository;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente el repository
     * y lo entrega a este service.
     */
    public HorarioActividadService(HorarioActividadRepository horarioActividadRepository) {
        this.horarioActividadRepository = horarioActividadRepository;
    }

    /**
     * Obtiene horarios activos por ID de actividad.
     *
     * @param actividadId ID de la actividad.
     * @return lista de horarios activos en formato DTO.
     */
    public List<HorarioActividadDTO> obtenerHorariosPorActividadId(Long actividadId) {
        List<HorarioActividad> horarios =
                horarioActividadRepository.findByActivoTrueAndActividad_IdOrderByDiaSemanaAscHoraInicioAsc(actividadId);

        return horarios.stream()
                .map(HorarioActividadMapper::toDTO)
                .toList();
    }

    /**
     * Obtiene horarios activos por slug de actividad.
     *
     * Este metodo nos sirve para el endpoint:
     * GET /api/actividades/{slug}/horarios
     *
     * @param actividadSlug slug de la actividad.
     * @return lista de horarios activos en formato DTO.
     */
    public List<HorarioActividadDTO> obtenerHorariosPorActividadSlug(String actividadSlug) {
        List<HorarioActividad> horarios =
                horarioActividadRepository.findByActivoTrueAndActividad_SlugOrderByDiaSemanaAscHoraInicioAsc(actividadSlug);

        return horarios.stream()
                .map(HorarioActividadMapper::toDTO)
                .toList();
    }
}