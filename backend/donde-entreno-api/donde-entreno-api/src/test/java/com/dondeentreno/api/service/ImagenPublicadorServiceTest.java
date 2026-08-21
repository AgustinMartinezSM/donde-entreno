package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ImagenPublicadorDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.exception.ImagenInvalidaException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.storage.AlmacenArchivos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImagenPublicadorServiceTest {

    private static final byte[] PNG_MINIMO = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00
    };

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private AlmacenArchivos almacenArchivos;

    private ImagenPublicadorService service;

    @BeforeEach
    void setUp() {
        service = new ImagenPublicadorService(
                imagenRepository,
                perfilPublicadorRepository,
                actividadRepository,
                almacenArchivos,
                /* Defaults reales del deploy: 12 de galería, 15 pendientes. */
                new com.dondeentreno.api.config.MediaProperties()
        );
    }

    @Test
    void subirImagenGuardaPendienteEnElEspacioPrivado() {
        prepararPerfilYActividad(5L, 20L, 10L);

        when(almacenArchivos.guardarPendiente(any(), eq("actividades/10"), eq("png")))
                .thenReturn("actividades/10/uuid.png");
        when(imagenRepository.findByActividad_IdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of());
        when(imagenRepository.save(any(Imagen.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(almacenArchivos.estaConfigurado()).thenReturn(true);
        when(almacenArchivos.firmarUrl(eq("actividades/10/uuid.png"), any(Duration.class)))
                .thenReturn("https://storage/firmada");

        ImagenPublicadorDTO dto = service.subirImagen(
                5L,
                10L,
                new MockMultipartFile("archivo", "foto.png", "image/png", PNG_MINIMO),
                "PRINCIPAL"
        );

        ArgumentCaptor<Imagen> captor = ArgumentCaptor.forClass(Imagen.class);
        verify(imagenRepository).save(captor.capture());
        Imagen guardada = captor.getValue();

        assertEquals("actividades/10/uuid.png", guardada.getUrl());
        assertEquals("PENDIENTE", guardada.getEstadoModeracion());
        assertFalse(guardada.getActiva());
        /* La preview del publicador usa la URL firmada del espacio privado. */
        assertEquals("https://storage/firmada", dto.getUrl());
    }

    @Test
    void subirImagenRechazaContenidoQueNoEsImagen() {
        prepararPerfilYActividad(5L, 20L, 10L);

        assertThrows(ImagenInvalidaException.class, () -> service.subirImagen(
                5L,
                10L,
                new MockMultipartFile(
                        "archivo",
                        "nota.txt",
                        "image/png",
                        "esto no es una imagen".getBytes()
                ),
                "PRINCIPAL"
        ));

        verify(almacenArchivos, never()).guardarPendiente(any(), anyString(), anyString());
    }

    @Test
    void eliminarMiaBorraElArchivoYDejaBajaLogica() {
        prepararPerfilYActividad(5L, 20L, 10L);

        Imagen imagen = crearImagen(77L, "actividades/10/uuid.png", "PENDIENTE");
        when(imagenRepository.findByIdAndActividad_Id(77L, 10L))
                .thenReturn(Optional.of(imagen));

        service.eliminarMia(5L, 10L, 77L);

        verify(almacenArchivos).eliminar("actividades/10/uuid.png");
        assertEquals("RECHAZADA", imagen.getEstadoModeracion());
        assertFalse(imagen.getActiva());
    }

    /*
      crearImagen deja activa=false: una APROBADA inactiva es una que ya
      fue eliminada — no hay nada más que eliminar (fase 2: las
      aprobadas ACTIVAS sí se eliminan; ver los tests de baja lógica).
    */
    @Test
    void eliminarMiaRechazaLasYaEliminadas() {
        prepararPerfilYActividad(5L, 20L, 10L);

        Imagen imagen = crearImagen(77L, "https://storage/publica.png", "APROBADA");
        when(imagenRepository.findByIdAndActividad_Id(77L, 10L))
                .thenReturn(Optional.of(imagen));

        assertThrows(
                ImagenInvalidaException.class,
                () -> service.eliminarMia(5L, 10L, 77L)
        );

        verify(almacenArchivos, never()).eliminar(anyString());
        verify(almacenArchivos, never()).eliminarPublicoPorUrl(anyString());
    }

    @Test
    void eliminarMiaConAprobadaActivaHaceBajaLogicaYBorraDelBucketPublico() {
        prepararPerfilYActividad(5L, 20L, 10L);

        Imagen imagen = crearImagen(77L, "https://storage/publicas/foto.png", "APROBADA");
        imagen.setActiva(true);
        when(imagenRepository.findByIdAndActividad_Id(77L, 10L))
                .thenReturn(Optional.of(imagen));

        service.eliminarMia(5L, 10L, 77L);

        verify(almacenArchivos).eliminarPublicoPorUrl("https://storage/publicas/foto.png");
        /* Sigue APROBADA: la baja es activa=false, no un rechazo. */
        assertEquals("APROBADA", imagen.getEstadoModeracion());
        assertFalse(imagen.getActiva());
        verify(imagenRepository).save(imagen);
    }

    /*
      El borrado del bucket público es best-effort (decisión del plan):
      un storage caído no puede dejar la foto visible para siempre.
    */
    @Test
    void eliminarMiaAvanzaAunqueElBucketPublicoFalle() {
        prepararPerfilYActividad(5L, 20L, 10L);

        Imagen imagen = crearImagen(77L, "https://storage/publicas/foto.png", "APROBADA");
        imagen.setActiva(true);
        when(imagenRepository.findByIdAndActividad_Id(77L, 10L))
                .thenReturn(Optional.of(imagen));
        org.mockito.Mockito.doThrow(new IllegalStateException("storage caido"))
                .when(almacenArchivos).eliminarPublicoPorUrl(anyString());

        service.eliminarMia(5L, 10L, 77L);

        assertFalse(imagen.getActiva());
        verify(imagenRepository).save(imagen);
    }

    @Test
    void ordenarGaleriaAsignaElOrdenElegido() {
        prepararPerfilYActividad(5L, 20L, 10L);

        Imagen primera = crearImagen(1L, "u1", "APROBADA");
        Imagen segunda = crearImagen(2L, "u2", "APROBADA");
        Imagen tercera = crearImagen(3L, "u3", "APROBADA");
        when(imagenRepository.findByActividad_IdAndTipoImagenAndActivaTrue(10L, "GALERIA"))
                .thenReturn(List.of(primera, segunda, tercera));

        service.ordenarGaleria(5L, 10L, List.of(3L, 1L, 2L));

        assertEquals(2, primera.getOrden());
        assertEquals(3, segunda.getOrden());
        assertEquals(1, tercera.getOrden());
        verify(imagenRepository).saveAll(List.of(primera, segunda, tercera));
    }

    @Test
    void ordenarGaleriaExigeExactamenteTodasLasFotos() {
        prepararPerfilYActividad(5L, 20L, 10L);

        Imagen primera = crearImagen(1L, "u1", "APROBADA");
        Imagen segunda = crearImagen(2L, "u2", "APROBADA");
        when(imagenRepository.findByActividad_IdAndTipoImagenAndActivaTrue(10L, "GALERIA"))
                .thenReturn(List.of(primera, segunda));

        /* Falta la 2 y aparece una ajena (99): rechazo sin tocar nada. */
        assertThrows(
                ImagenInvalidaException.class,
                () -> service.ordenarGaleria(5L, 10L, List.of(1L, 99L))
        );

        verify(imagenRepository, never()).saveAll(any());
    }

    @Test
    void elegirPrincipalHaceSwapConLaVigente() {
        prepararPerfilYActividad(5L, 20L, 10L);

        Imagen elegida = crearImagen(30L, "https://storage/publicas/g.png", "APROBADA");
        elegida.setActiva(true);
        elegida.setTipoImagen("GALERIA");
        elegida.setOrden(2);
        when(imagenRepository.findByIdAndActividad_Id(30L, 10L))
                .thenReturn(Optional.of(elegida));

        Imagen otraDeGaleria = crearImagen(31L, "u", "APROBADA");
        otraDeGaleria.setActiva(true);
        otraDeGaleria.setTipoImagen("GALERIA");
        otraDeGaleria.setOrden(1);
        when(imagenRepository.findByActividad_IdAndTipoImagenAndActivaTrue(10L, "GALERIA"))
                .thenReturn(List.of(otraDeGaleria, elegida));

        Imagen principalVigente = crearImagen(40L, "p", "APROBADA");
        principalVigente.setActiva(true);
        principalVigente.setTipoImagen("PRINCIPAL");
        principalVigente.setOrden(0);
        when(imagenRepository.findByActividad_IdAndTipoImagenAndActivaTrue(10L, "PRINCIPAL"))
                .thenReturn(List.of(principalVigente));

        service.elegirPrincipal(5L, 10L, 30L);

        assertEquals("PRINCIPAL", elegida.getTipoImagen());
        assertEquals(0, elegida.getOrden());
        /* La vieja principal baja a la galería, al final del orden. */
        assertEquals("GALERIA", principalVigente.getTipoImagen());
        assertEquals(3, principalVigente.getOrden());
    }

    @Test
    void elegirPrincipalRechazaPendientes() {
        prepararPerfilYActividad(5L, 20L, 10L);

        Imagen pendiente = crearImagen(30L, "ruta", "PENDIENTE");
        pendiente.setTipoImagen("GALERIA");
        when(imagenRepository.findByIdAndActividad_Id(30L, 10L))
                .thenReturn(Optional.of(pendiente));

        assertThrows(
                ImagenInvalidaException.class,
                () -> service.elegirPrincipal(5L, 10L, 30L)
        );

        verify(imagenRepository, never()).save(any(Imagen.class));
    }

    @Test
    void actualizarTextoGuardaTrimYVacioLimpia() {
        prepararPerfilYActividad(5L, 20L, 10L);

        Imagen imagen = crearImagen(77L, "https://storage/publicas/foto.png", "APROBADA");
        imagen.setActiva(true);
        imagen.setDescripcion("epigrafe viejo");
        when(imagenRepository.findByIdAndActividad_Id(77L, 10L))
                .thenReturn(Optional.of(imagen));
        when(imagenRepository.save(any(Imagen.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ImagenPublicadorDTO dto = service.actualizarTexto(
                5L, 10L, 77L, "  Sala de musculacion  ", "   "
        );

        assertEquals("Sala de musculacion", dto.getTitulo());
        /* Vacío/espacios = limpiar (semántica PATCH). */
        assertNull(dto.getDescripcion());
    }

    @Test
    void actualizarTextoConNullNoTocaElCampo() {
        prepararPerfilYActividad(5L, 20L, 10L);

        Imagen imagen = crearImagen(77L, "https://storage/publicas/foto.png", "APROBADA");
        imagen.setActiva(true);
        imagen.setTitulo("titulo vigente");
        when(imagenRepository.findByIdAndActividad_Id(77L, 10L))
                .thenReturn(Optional.of(imagen));
        when(imagenRepository.save(any(Imagen.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ImagenPublicadorDTO dto = service.actualizarTexto(5L, 10L, 77L, null, "Nueva desc");

        assertEquals("titulo vigente", dto.getTitulo());
        assertEquals("Nueva desc", dto.getDescripcion());
    }

    @Test
    void actualizarTextoRechazaEliminadasYRechazadas() {
        prepararPerfilYActividad(5L, 20L, 10L);

        Imagen eliminada = crearImagen(77L, "u", "APROBADA");
        /* activa=false ya viene del helper: es una eliminada. */
        when(imagenRepository.findByIdAndActividad_Id(77L, 10L))
                .thenReturn(Optional.of(eliminada));

        assertThrows(
                ImagenInvalidaException.class,
                () -> service.actualizarTexto(5L, 10L, 77L, "x", null)
        );
    }

    @Test
    void subirImagenRespetaElLimiteDeGaleria() {
        prepararPerfilYActividad(5L, 20L, 10L);

        when(imagenRepository.countByActividad_IdAndEstadoModeracion(10L, "PENDIENTE"))
                .thenReturn(2L);
        /* 10 activas + 2 pendientes = 12: la galería está llena. */
        when(imagenRepository.countByActividad_IdAndTipoImagenAndActivaTrue(10L, "GALERIA"))
                .thenReturn(10L);
        when(imagenRepository.countByActividad_IdAndTipoImagenAndEstadoModeracion(
                10L, "GALERIA", "PENDIENTE"))
                .thenReturn(2L);

        assertThrows(ImagenInvalidaException.class, () -> service.subirImagen(
                5L,
                10L,
                new MockMultipartFile("archivo", "foto.png", "image/png", PNG_MINIMO),
                "GALERIA"
        ));

        verify(almacenArchivos, never()).guardarPendiente(any(), anyString(), anyString());
    }

    @Test
    void subirImagenRespetaElTopeDePendientes() {
        prepararPerfilYActividad(5L, 20L, 10L);

        when(imagenRepository.countByActividad_IdAndEstadoModeracion(10L, "PENDIENTE"))
                .thenReturn(15L);

        assertThrows(ImagenInvalidaException.class, () -> service.subirImagen(
                5L,
                10L,
                new MockMultipartFile("archivo", "foto.png", "image/png", PNG_MINIMO),
                "PRINCIPAL"
        ));

        verify(almacenArchivos, never()).guardarPendiente(any(), anyString(), anyString());
    }

    @Test
    void subirImagenDePerfilRechazaUnSegundoPendienteDelMismoTipo() {
        PerfilPublicador perfil = prepararPerfil(5L);
        when(perfil.getId()).thenReturn(20L);

        when(imagenRepository.existsByPerfilPublicador_IdAndTipoImagenAndEstadoModeracion(
                20L, "LOGO", "PENDIENTE"))
                .thenReturn(true);

        assertThrows(ImagenInvalidaException.class, () -> service.subirImagenDePerfil(
                5L,
                new MockMultipartFile("archivo", "logo.png", "image/png", PNG_MINIMO),
                "LOGO"
        ));

        verify(almacenArchivos, never()).guardarPendiente(any(), anyString(), anyString());
    }

    /*
      El contador de orden es monotónico sobre max(orden): antes era
      size()+1 y tras retirar filas intermedias repetía valores.
    */
    @Test
    void subirImagenAsignaOrdenMonotonicoSobreElMaximo() {
        prepararPerfilYActividad(5L, 20L, 10L);

        Imagen conOrdenAlto = crearImagen(1L, "u1", "APROBADA");
        conOrdenAlto.setOrden(5);
        Imagen retirada = crearImagen(2L, "u2", "RECHAZADA");
        retirada.setOrden(2);
        when(imagenRepository.findByActividad_IdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(conOrdenAlto, retirada));

        when(imagenRepository.countByActividad_IdAndEstadoModeracion(10L, "PENDIENTE"))
                .thenReturn(0L);
        when(imagenRepository.countByActividad_IdAndTipoImagenAndActivaTrue(10L, "GALERIA"))
                .thenReturn(1L);
        when(imagenRepository.countByActividad_IdAndTipoImagenAndEstadoModeracion(
                10L, "GALERIA", "PENDIENTE"))
                .thenReturn(0L);
        when(almacenArchivos.guardarPendiente(any(), eq("actividades/10"), eq("png")))
                .thenReturn("actividades/10/uuid.png");
        when(imagenRepository.save(any(Imagen.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(almacenArchivos.estaConfigurado()).thenReturn(true);
        when(almacenArchivos.firmarUrl(anyString(), any(Duration.class)))
                .thenReturn("https://storage/firmada");

        service.subirImagen(
                5L,
                10L,
                new MockMultipartFile("archivo", "foto.png", "image/png", PNG_MINIMO),
                "GALERIA"
        );

        ArgumentCaptor<Imagen> captor = ArgumentCaptor.forClass(Imagen.class);
        verify(imagenRepository).save(captor.capture());
        assertEquals(6, captor.getValue().getOrden());
    }

    @Test
    void subirImagenDePerfilLaCuelgaDelPerfilYNoDeUnaActividad() {
        PerfilPublicador perfil = prepararPerfil(5L);
        when(perfil.getId()).thenReturn(20L);

        when(almacenArchivos.guardarPendiente(any(), eq("perfiles/20"), eq("png")))
                .thenReturn("perfiles/20/uuid.png");
        when(imagenRepository.save(any(Imagen.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.subirImagenDePerfil(
                5L,
                new MockMultipartFile("archivo", "logo.png", "image/png", PNG_MINIMO),
                "LOGO"
        );

        ArgumentCaptor<Imagen> captor = ArgumentCaptor.forClass(Imagen.class);
        verify(imagenRepository).save(captor.capture());
        Imagen guardada = captor.getValue();

        assertEquals("perfiles/20/uuid.png", guardada.getUrl());
        assertEquals("LOGO", guardada.getTipoImagen());
        assertEquals("PENDIENTE", guardada.getEstadoModeracion());
        assertFalse(guardada.getActiva());
        /*
          La constraint chk_imagen_duenio_unico exige exactamente un
          dueño: si esto se rompe, el insert falla en la base.
        */
        assertEquals(perfil, guardada.getPerfilPublicador());
        assertNull(guardada.getActividad());
    }

    /*
      El perfil no tiene PRINCIPAL ni GALERIA: mezclar los vocabularios
      dejaría un logo compitiendo con la portada de una actividad.
    */
    @Test
    void subirImagenDePerfilRechazaLosTiposDeActividad() {
        prepararPerfil(5L);

        assertThrows(ImagenInvalidaException.class, () -> service.subirImagenDePerfil(
                5L,
                new MockMultipartFile("archivo", "logo.png", "image/png", PNG_MINIMO),
                "PRINCIPAL"
        ));

        verify(almacenArchivos, never()).guardarPendiente(any(), anyString(), anyString());
    }

    @Test
    void eliminarMiaDePerfilRechazaLasYaEliminadas() {
        PerfilPublicador perfil = prepararPerfil(5L);
        when(perfil.getId()).thenReturn(20L);

        /* activa=false del helper: aprobada inactiva = ya eliminada. */
        Imagen aprobada = crearImagen(90L, "https://storage/logo.png", "APROBADA");
        when(imagenRepository.findByIdAndPerfilPublicador_Id(90L, 20L))
                .thenReturn(Optional.of(aprobada));

        assertThrows(
                ImagenInvalidaException.class,
                () -> service.eliminarMiaDePerfil(5L, 90L)
        );

        verify(almacenArchivos, never()).eliminar(anyString());
    }

    @Test
    void eliminarMiaDePerfilConAprobadaActivaHaceBajaLogica() {
        PerfilPublicador perfil = prepararPerfil(5L);
        when(perfil.getId()).thenReturn(20L);

        Imagen logo = crearImagen(90L, "https://storage/publicas/logo.png", "APROBADA");
        logo.setActiva(true);
        when(imagenRepository.findByIdAndPerfilPublicador_Id(90L, 20L))
                .thenReturn(Optional.of(logo));

        service.eliminarMiaDePerfil(5L, 90L);

        verify(almacenArchivos).eliminarPublicoPorUrl("https://storage/publicas/logo.png");
        assertEquals("APROBADA", logo.getEstadoModeracion());
        assertFalse(logo.getActiva());
        verify(imagenRepository).save(logo);
    }

    /*
      Sin stub de getId: el test del tipo inválido corta en la
      validación y nunca llega a pedirlo, y Mockito marca como error los
      stubs que no se usan.
    */
    private PerfilPublicador prepararPerfil(Long userId) {
        PerfilPublicador perfil = mock(PerfilPublicador.class);
        when(perfilPublicadorRepository.findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(userId))
                .thenReturn(Optional.of(perfil));

        return perfil;
    }

    private void prepararPerfilYActividad(Long userId, Long perfilId, Long actividadId) {
        PerfilPublicador perfil = mock(PerfilPublicador.class);
        when(perfil.getId()).thenReturn(perfilId);
        when(perfilPublicadorRepository.findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(userId))
                .thenReturn(Optional.of(perfil));

        Actividad actividad = new Actividad();
        actividad.setId(actividadId);
        when(actividadRepository
                .findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionInAndDeletedAtIsNull(
                        actividadId,
                        perfilId,
                        java.util.List.of("PUBLICADA", "PAUSADA")
                ))
                .thenReturn(Optional.of(actividad));
    }

    private Imagen crearImagen(Long id, String url, String estado) {
        Imagen imagen = new Imagen();
        imagen.setId(id);
        imagen.setUrl(url);
        imagen.setEstadoModeracion(estado);
        imagen.setActiva(false);
        imagen.setCreatedAt(OffsetDateTime.now());
        return imagen;
    }
}
