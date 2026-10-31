package model;

import java.util.Random;

/** Fantasma laranja: o mais lento e o mais errático (quase sempre muda nas interseções). */
public class FantasmaLaranja extends Fantasma {

    public static final double VELOCIDADE = 4.7;
    public static final double CHANCE_DE_MUDAR = 0.9;
    public static final int COR = 0xFB8C00;

    public FantasmaLaranja(int coluna, int linha) {
        this(coluna, linha, new Random());
    }

    public FantasmaLaranja(int coluna, int linha, Random random) {
        super(coluna, linha, VELOCIDADE, CHANCE_DE_MUDAR, COR, random);
    }
}
