package model;

/** Item comum, pequeno e espalhado por todo o labirinto. */
public class Pilula extends Item {

    public static final int PONTOS = 10;
    public static final double TAMANHO = 0.2;

    public Pilula(int coluna, int linha) {
        super(coluna, linha, TAMANHO);
    }

    @Override
    public int getPontos() {
        return PONTOS;
    }
}