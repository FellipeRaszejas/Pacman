package model;

import java.util.List;
import java.util.Random;

/**
 * Fantasma ciano, o instável: alterna entre perseguir o Pacman e fugir dele.
 * Persegue por {@value #DURACAO_PERSEGUICAO} s, foge por {@value #DURACAO_FUGA} s,
 * e repete. Enquanto foge, muda de cor para o jogador perceber.
 */
public class FantasmaCiano extends Fantasma {

    public static final double VELOCIDADE = 5.0;
    public static final double ATRASO_DE_SAIDA = 4.0;
    public static final double DURACAO_PERSEGUICAO = 8.0;
    public static final double DURACAO_FUGA = 5.0;
    public static final int COR = 0x26C6DA;
    public static final int COR_FUGA = 0x9FA8DA;

    private boolean fugindo;
    private double tempoNaFase;

    public FantasmaCiano(int coluna, int linha, Pacman alvo, Random random) {
        super(coluna, linha, VELOCIDADE, COR, ATRASO_DE_SAIDA, alvo, random);
    }

    public boolean isFugindo() {
        return fugindo;
    }

    @Override
    public int getCor() {
        return fugindo ? COR_FUGA : super.getCor();
    }

    @Override
    protected void aoPassarTempo(double dt) {
        tempoNaFase += dt;
        double limite = fugindo ? DURACAO_FUGA : DURACAO_PERSEGUICAO;
        if (tempoNaFase >= limite) {
            tempoNaFase = 0.0;
            fugindo = !fugindo;
        }
    }

    @Override
    protected Direction decidirDirecao(Labirinto labirinto, List<Direction> opcoes) {
        int[][] ate = labirinto.calcularDistancias(colunaDoAlvo(), linhaDoAlvo());
        return fugindo ? afastar(ate, opcoes) : aproximar(ate, opcoes);
    }
}