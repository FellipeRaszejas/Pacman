package audio;

import javax.sound.sampled.AudioFormat;

/**
 * Gera sons "de videogame antigo" (onda quadrada) como dados PCM, sem arquivos
 * .wav e sem tocar nada: só calcula os bytes. Por isso pode ser testado sem placa de som.
 * <p>
 * Formato: 22.050 Hz, 16 bits, mono, com sinal, little-endian.
 */
public final class Sintetizador {

    public static final float TAXA = 22_050f;
    public static final AudioFormat FORMATO = new AudioFormat(TAXA, 16, 1, true, false);

    private static final double VOLUME = 0.25;       // 0..1; baixo para não assustar
    private static final double ATAQUE_S = 0.004;    // subida/descida curtas evitam "cliques"
    private static final double RELEASE_S = 0.012;

    private Sintetizador() {
    }

    /** Um trecho de som: frequência inicial e final (em Hz; 0 = silêncio) e duração. */
    public static final class Nota {
        private final double hzInicial;
        private final double hzFinal;
        private final double milissegundos;

        private Nota(double hzInicial, double hzFinal, double milissegundos) {
            if (hzInicial < 0 || hzFinal < 0 || !(milissegundos > 0)) {
                throw new IllegalArgumentException("Frequência e duração devem ser positivas.");
            }
            this.hzInicial = hzInicial;
            this.hzFinal = hzFinal;
            this.milissegundos = milissegundos;
        }
    }

    /** Nota de frequência constante. */
    public static Nota nota(double hz, double milissegundos) {
        return new Nota(hz, hz, milissegundos);
    }

    /** Nota cuja frequência sobe ou desce de forma contínua (glissando). */
    public static Nota deslizar(double hzInicial, double hzFinal, double milissegundos) {
        return new Nota(hzInicial, hzFinal, milissegundos);
    }

    /** Silêncio. */
    public static Nota pausa(double milissegundos) {
        return new Nota(0, 0, milissegundos);
    }

    /** Toca as notas em sequência e devolve os bytes PCM. */
    public static byte[] sintetizar(Nota... notas) {
        int total = 0;
        for (Nota n : notas) {
            total += amostras(n);
        }
        byte[] pcm = new byte[total * 2];

        int indice = 0;
        for (Nota n : notas) {
            int quantidade = amostras(n);
            double duracao = quantidade / (double) TAXA;
            double fase = 0.0;
            for (int i = 0; i < quantidade; i++) {
                double t = i / (double) TAXA;
                double valor = 0.0;
                if (n.hzInicial > 0 || n.hzFinal > 0) {
                    double hz = n.hzInicial + (n.hzFinal - n.hzInicial) * (t / duracao);
                    fase += 2 * Math.PI * hz / TAXA;
                    double quadrada = Math.sin(fase) >= 0 ? 1.0 : -1.0;
                    double envelope = Math.min(1.0, Math.min(t / ATAQUE_S, (duracao - t) / RELEASE_S));
                    valor = quadrada * envelope * VOLUME;
                }
                short amostra = (short) Math.round(valor * Short.MAX_VALUE);
                pcm[indice++] = (byte) (amostra & 0xFF);
                pcm[indice++] = (byte) ((amostra >> 8) & 0xFF);
            }
        }
        return pcm;
    }

    private static int amostras(Nota n) {
        return Math.max(1, (int) Math.round(n.milissegundos / 1000.0 * TAXA));
    }

    /** Duração em segundos de um trecho PCM gerado aqui. */
    public static double duracaoEmSegundos(byte[] pcm) {
        return pcm.length / 2.0 / TAXA;
    }
}