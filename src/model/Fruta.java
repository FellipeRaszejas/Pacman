package model;

/**
 * Item raro e valioso: poucas frutas no mapa, grandes e todas diferentes. Cada
 * uma vale bem mais que uma pílula e, ao ser comida, deixa o Pacman poderoso
 * (ele pode devorar os fantasmas por um tempo).
 */
public class Fruta extends Item {

    public static final int PONTOS = 100;
    public static final double TAMANHO = 0.8;

    private final TipoFruta tipo;

    public Fruta(int coluna, int linha) {
        this(coluna, linha, TipoFruta.CEREJA);
    }

    public Fruta(int coluna, int linha, TipoFruta tipo) {
        super(coluna, linha, TAMANHO);
        if (tipo == null) {
            throw new IllegalArgumentException("O tipo da fruta não pode ser nulo.");
        }
        this.tipo = tipo;
    }

    public TipoFruta getTipo() {
        return tipo;
    }

    @Override
    public int getPontos() {
        return PONTOS;
    }

    @Override
    public boolean concedePoder() {
        return true;
    }
}