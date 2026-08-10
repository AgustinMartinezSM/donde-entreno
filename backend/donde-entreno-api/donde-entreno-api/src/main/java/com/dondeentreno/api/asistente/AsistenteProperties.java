package com.dondeentreno.api.asistente;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del asistente.
 *
 * Los valores llegan por variables de entorno (ver application.properties)
 * y todos tienen default: sin configurar nada, el asistente funciona en
 * modo local (resolución determinística contra el catálogo real) y la
 * parte de Gemini queda apagada.
 *
 * La API key jamás se loguea ni se expone por ningún endpoint.
 */
@ConfigurationProperties(prefix = "dondeentreno.asistente")
public class AsistenteProperties {

    /** Largo máximo del mensaje del usuario. Más largo se rechaza con 400. */
    private int maxInputChars = 300;

    /** Consultas permitidas por IP en una ventana de un minuto. */
    private int maxConsultasPorMinuto = 8;

    /** Consultas permitidas por IP en una ventana de una hora. */
    private int maxConsultasPorHora = 60;

    /** Interruptor de Gemini. Apagado, el asistente responde solo con el motor local. */
    private boolean geminiEnabled = false;

    /** API key de Gemini (solo backend, solo panel de Render). */
    private String geminiApiKey = "";

    /**
     * Id del modelo. Configurable a propósito para no hornear una versión
     * en el código: los ids de Gemini cambian con cada familia.
     */
    private String geminiModel = "";

    /** Tope global de llamadas a Gemini por día. Al agotarse se cae al motor local. */
    private int geminiDailyLimit = 30;

    public int getMaxInputChars() {
        return maxInputChars;
    }

    public void setMaxInputChars(int maxInputChars) {
        this.maxInputChars = maxInputChars;
    }

    public int getMaxConsultasPorMinuto() {
        return maxConsultasPorMinuto;
    }

    public void setMaxConsultasPorMinuto(int maxConsultasPorMinuto) {
        this.maxConsultasPorMinuto = maxConsultasPorMinuto;
    }

    public int getMaxConsultasPorHora() {
        return maxConsultasPorHora;
    }

    public void setMaxConsultasPorHora(int maxConsultasPorHora) {
        this.maxConsultasPorHora = maxConsultasPorHora;
    }

    public boolean isGeminiEnabled() {
        return geminiEnabled;
    }

    public void setGeminiEnabled(boolean geminiEnabled) {
        this.geminiEnabled = geminiEnabled;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public String getGeminiModel() {
        return geminiModel;
    }

    public void setGeminiModel(String geminiModel) {
        this.geminiModel = geminiModel;
    }

    public int getGeminiDailyLimit() {
        return geminiDailyLimit;
    }

    public void setGeminiDailyLimit(int geminiDailyLimit) {
        this.geminiDailyLimit = geminiDailyLimit;
    }

    /**
     * Gemini se considera disponible solo si está encendido Y tiene key y
     * modelo. Encenderlo sin credenciales no debe romper nada: el asistente
     * sigue respondiendo con el motor local.
     */
    public boolean geminiDisponible() {
        return geminiEnabled
                && !geminiApiKey.isBlank()
                && !geminiModel.isBlank();
    }
}
