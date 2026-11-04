package audio;

import controller.EventosDoJogo;
import model.Fruta;
import model.Item;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * Toca os efeitos sonoros quando a engine avisa que algo aconteceu.
 * <p>
 * Se o computador não tiver saída de áudio (ou ela estiver ocupada), o jogo
 * continua normalmente, só que sem som.
 */
public class SomDoJogo implements EventosDoJogo {

    private final Clip chompSobe;
    private final Clip chompDesce;
    private final Clip fruta;
    private final Clip perderVida;
    private final Clip gameOver;
    private final Clip vitoria;
    private final Clip inicio;
    private final Clip[] todos;

    private boolean proximoChompSobe = true;
    private boolean mudo;

    public SomDoJogo() {
        chompSobe = criarClip(Efeitos.chomp(true));
        chompDesce = criarClip(Efeitos.chomp(false));
        fruta = criarClip(Efeitos.fruta());
        perderVida = criarClip(Efeitos.perderVida());
        gameOver = criarClip(Efeitos.gameOver());
        vitoria = criarClip(Efeitos.vitoria());
        inicio = criarClip(Efeitos.inicio());
        todos = new Clip[] {chompSobe, chompDesce, fruta, perderVida, gameOver, vitoria, inicio};

        if (!disponivel()) {
            System.err.println("Áudio indisponível neste computador: o jogo seguirá sem som.");
        }
    }

    /** Verdadeiro se pelo menos um efeito conseguiu ser preparado para tocar. */
    public boolean disponivel() {
        for (Clip c : todos) {
            if (c != null) {
                return true;
            }
        }
        return false;
    }

    public boolean isMudo() {
        return mudo;
    }

    public void alternarMudo() {
        mudo = !mudo;
        if (mudo) {
            silenciar();
        }
    }

    private static Clip criarClip(byte[] pcm) {
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(Sintetizador.FORMATO, pcm, 0, pcm.length);
            return clip;
        } catch (Exception e) { // sem dispositivo de áudio, linha ocupada, permissão negada...
            return null;
        }
    }

    private void tocar(Clip clip) {
        if (mudo || clip == null) {
            return;
        }
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    private void silenciar() {
        for (Clip c : todos) {
            if (c != null) {
                c.stop();
            }
        }
    }

    @Override
    public void aoIniciarPartida() {
        silenciar();
        tocar(inicio);
    }

    @Override
    public void aoColetarItem(Item item) {
        if (item instanceof Fruta) {
            tocar(fruta);
        } else {
            tocar(proximoChompSobe ? chompSobe : chompDesce);
            proximoChompSobe = !proximoChompSobe;
        }
    }

    @Override
    public void aoPerderVida() {
        silenciar();
        tocar(perderVida);
    }

    @Override
    public void aoGameOver() {
        silenciar();
        tocar(gameOver);
    }

    @Override
    public void aoVencer() {
        silenciar();
        tocar(vitoria);
    }
}