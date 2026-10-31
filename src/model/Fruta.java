package model;

/** Item raro e valioso: poucas frutas no mapa, cada uma vale bem mais que uma pílula. */
public class Fruta extends Item {

    public static final int PONTOS = 100;
    public static final double TAMANHO = 0.6;

    public Fruta(int coluna, int linha) {
        super(coluna, linha, TAMANHO);
    }

    @Override
    public int getPontos() {
        return PONTOS;
    }
}