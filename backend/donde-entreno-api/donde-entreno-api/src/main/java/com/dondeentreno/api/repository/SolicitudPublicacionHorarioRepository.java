package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.SolicitudPublicacionHorario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository de SolicitudPublicacionHorario.
 *
 * Esta interfaz se encarga de consultar la tabla solicitud_publicacion_horario
 * usando Spring Data JPA.
 */
public interface SolicitudPublicacionHorarioRepository extends JpaRepository<SolicitudPublicacionHorario, Long> {

    List<SolicitudPublicacionHorario> findBySolicitudPublicacion_Id(Long solicitudPublicacionId);
}