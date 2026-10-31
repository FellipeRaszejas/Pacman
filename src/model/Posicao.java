package model;

import java.util.Objects;

/** Posição imutável de uma célula do labirinto (coluna = x, linha = y). */
public final class Posicao {

    private final int coluna;
    private final int linha;

    public Posicao(int coluna, int linha) {
        this.coluna = coluna;
        this.linha = linha;
    }

    public int getColuna() {
        return coluna;
    }

    public int getLinha() {
        return linha;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Posicao)) {
            return false;
        }
        Posicao outra = (Posicao) o;
        return coluna == outra.coluna && linha == outra.linha;
    }

    @Override
    public int hashCode() {
        return Objects.hash(coluna, linha);
    }

    @Override
    public String toString() {
        return "(" + coluna + "," + linha + ")";
    }
}
