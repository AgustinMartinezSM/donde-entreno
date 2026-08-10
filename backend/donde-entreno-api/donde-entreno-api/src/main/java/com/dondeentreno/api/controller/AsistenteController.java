package com.dondeentreno.api.controller;

import com.dondeentreno.api.asistente.LimitadorConsultas;
import com.dondeentreno.api.dto.AsistenteConsultaRequestDTO;
import com.dondeentreno.api.dto.AsistenteRespuestaDTO;
import com.dondeentreno.api.exception.LimiteConsultasExcedidoException;
import com.dondeentreno.api.service.AsistenteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint público del asistente.
 *
 * Es el único POST público de la API además del registro y el alta de
 * solicitudes, así que el límite por IP no es opcional: sin él, cualquiera
 * puede golpearlo en loop.
 */
@RestController
@RequestMapping("/api/asistente")
public class AsistenteController {

    /**
     * Tope defensivo para el header de proxy. Un X-Forwarded-For enorme
     * no debe terminar como clave de un mapa en memoria.
     */
    private static final int MAX_LARGO_IP = 60;

    private final AsistenteService asistenteService;
    private final LimitadorConsultas limitador;

    public AsistenteController(
            AsistenteService asistenteService,
            LimitadorConsultas limitador
    ) {
        this.asistenteService = asistenteService;
        this.limitador = limitador;
    }

    /**
     * Responde una consulta en lenguaje natural.
     *
     * @param request mensaje del usuario.
     * @param peticion para identificar al cliente y aplicar el límite.
     * @return respuesta con texto, enlaces internos y opciones rápidas.
     */
    @PostMapping("/consulta")
    public ResponseEntity<AsistenteRespuestaDTO> consultar(
            @Valid @RequestBody AsistenteConsultaRequestDTO request,
            HttpServletRequest peticion
    ) {
        if (!limitador.registrarConsulta(obtenerIdentificadorCliente(peticion))) {
            throw new LimiteConsultasExcedidoException(
                    "Estás consultando muy seguido. Esperá un momento y probá de nuevo."
            );
        }

        return ResponseEntity.ok(
                asistenteService.responder(request.getTexto(), request.getHistorial())
        );
    }

    /**
     * Identifica al cliente para el límite por IP.
     *
     * En Render la app corre detrás de un proxy, así que la IP real viene
     * en X-Forwarded-For y getRemoteAddr() devuelve siempre la del proxy.
     *
     * El header lo puede falsear un cliente malicioso, y por eso el límite
     * por IP no es la protección del gasto: esa es el tope diario global,
     * que no se puede esquivar cambiando de IP.
     */
    private String obtenerIdentificadorCliente(HttpServletRequest peticion) {
        String reenviadas = peticion.getHeader("X-Forwarded-For");

        if (reenviadas != null && !reenviadas.isBlank()) {
            String primera = reenviadas.split(",")[0].trim();

            if (!primera.isEmpty()) {
                return recortar(primera);
            }
        }

        String remota = peticion.getRemoteAddr();

        return remota == null ? "desconocida" : recortar(remota);
    }

    private String recortar(String valor) {
        return valor.length() <= MAX_LARGO_IP
                ? valor
                : valor.substring(0, MAX_LARGO_IP);
    }
}
