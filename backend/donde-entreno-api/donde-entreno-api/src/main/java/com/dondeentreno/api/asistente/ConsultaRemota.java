package com.dondeentreno.api.asistente;

import com.dondeentreno.api.dto.AsistenteMensajeDTO;

import java.util.List;
import java.util.Set;

/**
 * Todo lo que el motor remoto necesita saber para contestar un turno.
 *
 * Se pasa junto y explícito en vez de dejar que el cliente vaya a buscar
 * datos por su cuenta: así queda a la vista qué información sale del
 * servidor hacia Google en cada llamada. Es todo público — catálogo,
 * vocabulario y la charla que la persona ya vio en pantalla — y no hay
 * ningún dato de cuenta, sesión ni identificación.
 *
 * @param mensaje       lo que la persona acaba de escribir.
 * @param historial     turnos previos, ya recortados por el servicio.
 * @param catalogo      términos reales para el campo "filtros".
 * @param vocabulario   nombres de deportes que el modelo puede usar.
 * @param rechazados    lo que la persona ya descartó. Va en el prompt para
 *                      que no insista, pero el filtro que de verdad manda
 *                      está en código: acá solo evitamos gastar el turno.
 * @param conActividades deportes que hoy tienen actividades publicadas. El
 *                      modelo NO puede afirmar disponibilidad, pero saberlo
 *                      lo ayuda a priorizar lo que sí se puede mostrar.
 */
public record ConsultaRemota(
        String mensaje,
        List<AsistenteMensajeDTO> historial,
        String catalogo,
        String vocabulario,
        Set<String> rechazados,
        Set<String> conActividades
) {
}
