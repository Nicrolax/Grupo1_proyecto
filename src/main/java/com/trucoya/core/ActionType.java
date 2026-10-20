package com.trucoya.core;

/**
 * Acciones de la botonera. Cada canto tiene una clave de sonido (voz rioplatense) y un texto
 * para el globo sobre el avatar; la Fase 2 los registra como SoundEvent y traducciones.
 */
public enum ActionType {
    PLAY_CARD("Jugar carta", null),

    TRUCO("¡Truco!", "truco"),
    RETRUCO("¡Quiero retruco!", "retruco"),
    VALE_CUATRO("¡Quiero vale cuatro!", "vale_cuatro"),

    ENVIDO("¡Envido!", "envido"),
    REAL_ENVIDO("¡Real envido!", "real_envido"),
    FALTA_ENVIDO("¡Falta envido!", "falta_envido"),

    FLOR("¡Flor!", "flor"),
    CONTRAFLOR("¡Contraflor!", "contraflor"),
    CONTRAFLOR_AL_RESTO("¡Contraflor al resto!", "contraflor_al_resto"),
    CON_FLOR_ME_ACHICO("Con flor me achico", "con_flor_me_achico"),

    QUIERO("¡Quiero!", "quiero"),
    NO_QUIERO("No quiero", "no_quiero"),
    ME_VOY_AL_MAZO("Me voy al mazo", "me_voy_al_mazo");

    private final String speech;
    private final String soundKey;

    ActionType(String speech, String soundKey) {
        this.speech = speech;
        this.soundKey = soundKey;
    }

    /** Texto del globo de diálogo. */
    public String getSpeech() {
        return speech;
    }

    /** Sufijo de SoundEvent ("trucoya:voice.&lt;key&gt;"); null si la acción no tiene voz. */
    public String getSoundKey() {
        return soundKey;
    }

    public boolean isTrucoCall() {
        return this == TRUCO || this == RETRUCO || this == VALE_CUATRO;
    }

    public boolean isEnvidoCall() {
        return this == ENVIDO || this == REAL_ENVIDO || this == FALTA_ENVIDO;
    }

    /** Nivel de truco que propone el canto (1 = truco, 2 = retruco, 3 = vale cuatro). */
    public int trucoLevel() {
        switch (this) {
            case TRUCO: return 1;
            case RETRUCO: return 2;
            case VALE_CUATRO: return 3;
            default: return 0;
        }
    }

    public static ActionType trucoCallForLevel(int level) {
        switch (level) {
            case 1: return TRUCO;
            case 2: return RETRUCO;
            case 3: return VALE_CUATRO;
            default: return null;
        }
    }
}
