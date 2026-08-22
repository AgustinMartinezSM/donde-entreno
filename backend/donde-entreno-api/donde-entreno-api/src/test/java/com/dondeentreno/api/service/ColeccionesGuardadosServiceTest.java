package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ColeccionGuardadosDTO;
import com.dondeentreno.api.entity.ColeccionGuardados;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.exception.SolicitudPublicacionInvalidaException;
import com.dondeentreno.api.repository.ColeccionGuardadosRepository;
import com.dondeentreno.api.repository.FavoritoActividadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ColeccionesGuardadosServiceTest {

    private ColeccionGuardadosRepository coleccionRepository;
    private FavoritoActividadRepository favoritoRepository;
    private ColeccionesGuardadosService service;

    @BeforeEach
    void preparar() {
        coleccionRepository = mock(ColeccionGuardadosRepository.class);
        favoritoRepository = mock(FavoritoActividadRepository.class);
        service = new ColeccionesGuardadosService(coleccionRepository, favoritoRepository);
    }

    @Test
    void listarDevuelveLasColeccionesConSusConteosSinNMasUno() {
        when(favoritoRepository.contarPorColeccion(7L))
                .thenReturn(List.<Object[]>of(new Object[]{5L, 3L}));
        when(coleccionRepository.findByUsuarioIdOrderByNombreAsc(7L))
                .thenReturn(List.of(
                        coleccion(5L, 7L, "Cerca de casa"),
                        coleccion(6L, 7L, "Para probar")
                ));

        List<ColeccionGuardadosDTO> resultado = service.listar(7L);

        assertEquals(2, resultado.size());
        assertEquals("Cerca de casa", resultado.get(0).getNombre());
        assertEquals(3L, resultado.get(0).getCantidad());
        assertEquals(0L, resultado.get(1).getCantidad());
    }

    @Test
    void crearNormalizaElNombreYArrancaEnCero() {
        when(coleccionRepository.countByUsuarioId(7L)).thenReturn(2L);
        when(coleccionRepository.existsByUsuarioIdAndNombreIgnoreCase(7L, "Para probar"))
                .thenReturn(false);
        when(coleccionRepository.save(any(ColeccionGuardados.class)))
                .thenAnswer(inv -> {
                    ColeccionGuardados guardada = inv.getArgument(0);
                    ColeccionGuardados conId = coleccion(9L, guardada.getUsuarioId(), guardada.getNombre());
                    return conId;
                });

        ColeccionGuardadosDTO resultado = service.crear(7L, "  Para probar  ");

        assertEquals("Para probar", resultado.getNombre());
        assertEquals(0L, resultado.getCantidad());
    }

    @Test
    void crearConNombreDuplicadoRechaza() {
        when(coleccionRepository.countByUsuarioId(7L)).thenReturn(2L);
        when(coleccionRepository.existsByUsuarioIdAndNombreIgnoreCase(7L, "Para probar"))
                .thenReturn(true);

        assertThrows(
                SolicitudPublicacionInvalidaException.class,
                () -> service.crear(7L, "Para probar")
        );
        verify(coleccionRepository, never()).save(any(ColeccionGuardados.class));
    }

    @Test
    void crearConElTopeAlcanzadoRechaza() {
        when(coleccionRepository.countByUsuarioId(7L))
                .thenReturn((long) ColeccionesGuardadosService.MAX_COLECCIONES);

        assertThrows(
                SolicitudPublicacionInvalidaException.class,
                () -> service.crear(7L, "Una mas")
        );
    }

    @Test
    void renombrarConservaSuPropioNombreSinChocarConsigoMisma() {
        when(coleccionRepository.findByIdAndUsuarioId(5L, 7L))
                .thenReturn(Optional.of(coleccion(5L, 7L, "para probar")));
        when(coleccionRepository.save(any(ColeccionGuardados.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(favoritoRepository.contarPorColeccion(7L))
                .thenReturn(List.<Object[]>of(new Object[]{5L, 2L}));

        ColeccionGuardadosDTO resultado = service.renombrar(7L, 5L, "Para Probar");

        assertEquals("Para Probar", resultado.getNombre());
        assertEquals(2L, resultado.getCantidad());
        /* Mismo nombre con otras mayusculas NO consulta el duplicado. */
        verify(coleccionRepository, never()).existsByUsuarioIdAndNombreIgnoreCase(7L, "Para Probar");
    }

    @Test
    void unaColeccionAjenaDa404EnTodo() {
        when(coleccionRepository.findByIdAndUsuarioId(5L, 7L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.renombrar(7L, 5L, "Nueva")
        );
        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.eliminar(7L, 5L)
        );
        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.validarPropia(7L, 5L)
        );
    }

    private ColeccionGuardados coleccion(Long id, Long usuarioId, String nombre) {
        ColeccionGuardados coleccion = new ColeccionGuardados();
        /* El id se setea por reflexion del test: la entidad no expone setter. */
        try {
            java.lang.reflect.Field campo = ColeccionGuardados.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(coleccion, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        coleccion.setUsuarioId(usuarioId);
        coleccion.setNombre(nombre);
        return coleccion;
    }
}
