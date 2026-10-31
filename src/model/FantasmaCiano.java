package model;

import java.util.Random;

/** Fantasma ciano: um pouco mais lento e inclinado a mudar de direção nas interseções. */
public class FantasmaCiano extends Fantasma {

    public static final double VELOCIDADE = 5.0;
    public static final double CHANCE_DE_MUDAR = 0.7;
    public static final int COR = 0x26C6DA;

    public FantasmaCiano(int coluna, int linha) {
        this(coluna, linha, new Random());
    }

    public FantasmaCiano(int coluna, int linha, Random random) {
        super(coluna, linha, VELOCIDADE, CHANCE_DE_MUDAR, COR, random);
    }
}
