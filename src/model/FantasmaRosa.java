package model;

import java.util.Random;

/** Fantasma rosa: velocidade média e chance equilibrada de mudar de direção. */
public class FantasmaRosa extends Fantasma {

    public static final double VELOCIDADE = 5.3;
    public static final double CHANCE_DE_MUDAR = 0.5;
    public static final int COR = 0xF48FB1;

    public FantasmaRosa(int coluna, int linha) {
        this(coluna, linha, new Random());
    }

    public FantasmaRosa(int coluna, int linha, Random random) {
        super(coluna, linha, VELOCIDADE, CHANCE_DE_MUDAR, COR, random);
    }
}
