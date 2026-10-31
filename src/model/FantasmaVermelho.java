package model;

import java.util.Random;

/** Fantasma vermelho: o mais rápido e o mais persistente (raramente muda de direção). */
public class FantasmaVermelho extends Fantasma {

    public static final double VELOCIDADE = 5.7;
    public static final double CHANCE_DE_MUDAR = 0.25;
    public static final int COR = 0xE53935;

    public FantasmaVermelho(int coluna, int linha) {
        this(coluna, linha, new Random());
    }

    public FantasmaVermelho(int coluna, int linha, Random random) {
        super(coluna, linha, VELOCIDADE, CHANCE_DE_MUDAR, COR, random);
    }
}
