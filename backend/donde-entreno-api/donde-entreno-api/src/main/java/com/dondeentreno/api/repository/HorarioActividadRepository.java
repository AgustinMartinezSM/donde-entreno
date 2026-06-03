package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.HorarioActividad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository de HorarioActividad.
 *
 * Esta interfaz se encarga de consultar la tabla horario_actividad
 * usando Spring Data JPA.
 */
public interface HorarioActividadRepository extends JpaRepository<HorarioActividad, Long> {

    /**
     * Busca horarios activos de una actividad por ID.
     *
     * Spring interpreta:
     * WHERE activo = true
     * AND actividad.id = ?
     *
     * Ordenamos por día de la semana y hora de inicio.
     */
    List<HorarioActividad> findByActivoTrueAndActividad_IdOrderByDiaSemanaAscHoraInicioAsc(Long actividadId);

    /**
     * Busca horarios activos de una actividad usando el slug de la actividad.
     *
     * Esto nos va a servir para:
     * GET /api/actividades/{slug}/horarios
     */
    List<HorarioActividad> findByActivoTrueAndActividad_SlugOrderByDiaSemanaAscHoraInicioAsc(String actividadSlug);
}
