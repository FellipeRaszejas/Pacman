package model;

/**
 * Qualquer coisa que o Pacman pode coletar. Itens ficam parados em uma célula
 * do labirinto; cada tipo define quantos pontos vale.
 */
public abstract class Item extends Entidade {

    protected Item(int coluna, int linha, double tamanho) {
        super(coluna, linha, tamanho);
    }

    /** Pontos que o jogador ganha ao coletar este item. */
    public abstract int getPontos();

    /** Itens são estáticos: não há nada a atualizar a cada quadro. */
    @Override
    public final void atualizar(Labirinto labirinto, double dt) {
        // intencionalmente vazio
    }
}