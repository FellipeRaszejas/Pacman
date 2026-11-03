package model;

/** Estados possíveis de uma partida. */
public enum EstadoJogo {
    /** Pausa curta ("PRONTO!") no início da partida e depois de perder uma vida. */
    PRONTO,
    JOGANDO,
    GAME_OVER,
    VITORIA
}