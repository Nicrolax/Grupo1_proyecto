package com.trucoya.core;

/**
 * Resultado de intentar una acción. El motor nunca lanza excepciones por entradas de
 * jugadores (paquetes de red pueden llegar tarde o manipulados): las rechaza con un motivo.
 */
public final class ActionResult {

    private static final ActionResult OK = new ActionResult(true, null);

    private final boolean success;
    private final String reason;

    private ActionResult(boolean success, String reason) {
        this.success = success;
        this.reason = reason;
    }

    public static ActionResult ok() {
        return OK;
    }

    public static ActionResult fail(String reason) {
        return new ActionResult(false, reason);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return success ? "OK" : "RECHAZADO: " + reason;
    }
}
