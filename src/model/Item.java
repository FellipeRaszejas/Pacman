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

    /**
     * Este item deixa o Pacman capaz de devorar fantasmas? Por padrão não; os tipos
     * que dão poder sobrescrevem. Assim a engine não precisa perguntar "é fruta?".
     */
    public boolean concedePoder() {
        return false;
    }

    /** Itens são estáticos: não há nada a atualizar a cada quadro. */
    @Override
    public final void atualizar(Labirinto labirinto, double dt) {
        // intencionalmente vazio
    }
}