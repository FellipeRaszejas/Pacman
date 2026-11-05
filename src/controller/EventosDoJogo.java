package controller;

import model.Item;

/**
 * Avisos que a {@link GameEngine} dá quando algo importante acontece. Quem quiser
 * reagir (tocar um som, por exemplo) implementa esta interface e se registra com
 * {@link GameEngine#setEventos}. A engine não sabe nada sobre áudio.
 * <p>
 * Todos os métodos têm implementação vazia: implemente só os que interessam.
 */
public interface EventosDoJogo {

    /** Uma partida começou (ao abrir o jogo e ao reiniciar). */
    default void aoIniciarPartida() {
    }

    /** O Pacman coletou um item (pílula, fruta...). */
    default void aoColetarItem(Item item) {
    }

    /** O Pacman perdeu uma vida e ainda restam vidas. */
    default void aoPerderVida() {
    }

    /** O Pacman perdeu a última vida. */
    default void aoGameOver() {
    }

    /** O último item foi coletado. */
    default void aoVencer() {
    }

    /** O Pacman comeu uma fruta e está poderoso: por um tempo, os fantasmas podem ser devorados. */
    default void aoFicarPoderoso() {
    }

    /** O Pacman devorou um fantasma e ganhou {@code pontos}. */
    default void aoComerFantasma(int pontos) {
    }
}