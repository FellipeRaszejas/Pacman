package audio;

import static audio.Sintetizador.deslizar;
import static audio.Sintetizador.nota;
import static audio.Sintetizador.pausa;
import static audio.Sintetizador.sintetizar;

/** Os efeitos sonoros do jogo, definidos como sequências de notas. */
public final class Efeitos {

    private Efeitos() {
    }

    /** "Waka": ao comer pílulas, alterna entre um chiado que sobe e outro que desce. */
    public static byte[] chomp(boolean subindo) {
        return subindo
                ? sintetizar(deslizar(300, 520, 55))
                : sintetizar(deslizar(520, 300, 55));
    }

    public static byte[] fruta() {
        return sintetizar(nota(660, 70), nota(880, 70), nota(1320, 130));
    }

    public static byte[] perderVida() {
        return sintetizar(deslizar(800, 500, 250), deslizar(600, 300, 250), deslizar(400, 100, 450));
    }

    public static byte[] gameOver() {
        return sintetizar(nota(440, 250), nota(392, 250), nota(330, 250), nota(262, 650));
    }

    public static byte[] vitoria() {
        return sintetizar(nota(523, 120), nota(659, 120), nota(784, 120), nota(1047, 250),
                pausa(60), nota(784, 120), nota(1047, 500));
    }

    /** Vinheta do "PRONTO!": precisa caber nos 2 s da pausa. */
    public static byte[] inicio() {
        return sintetizar(nota(262, 130), nota(330, 130), nota(392, 130), nota(523, 260),
                pausa(60), nota(392, 130), nota(523, 400));
    }
}