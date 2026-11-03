package model;

import java.util.List;
import java.util.Random;

/**
 * Fantasma rosa, o emboscador: em vez de ir atrás do Pacman, tenta chegar
 * {@value #CASAS_A_FRENTE} casas <i>à frente</i> dele, na direção em que ele anda,
 * para cortar o caminho. Quando já está perto (até {@value #RAIO_DIRETO} passos),
 * muda de ideia e vai direto nele.
 */
public class FantasmaRosa extends Fantasma {

    public static final double VELOCIDADE = 5.0;
    public static final double ATRASO_DE_SAIDA = 2.0;
    public static final int CASAS_A_FRENTE = 4;
    public static final int RAIO_DIRETO = 4;
    public static final int COR = 0xF48FB1;

    public FantasmaRosa(int coluna, int linha, Pacman alvo, Random random) {
        super(coluna, linha, VELOCIDADE, COR, ATRASO_DE_SAIDA, alvo, random);
    }

    @Override
    protected Direction decidirDirecao(Labirinto labirinto, List<Direction> opcoes) {
        int[][] ateOPacman = labirinto.calcularDistancias(colunaDoAlvo(), linhaDoAlvo());

        int minhaDistancia = ateOPacman[getLinha()][getColuna()];
        if (minhaDistancia >= 0 && minhaDistancia <= RAIO_DIRETO) {
            return aproximar(ateOPacman, opcoes);
        }

        Posicao emboscada = posicaoDeEmboscada(labirinto);
        int[][] ateAEmboscada =
                labirinto.calcularDistancias(emboscada.getColuna(), emboscada.getLinha());
        return aproximar(ateAEmboscada, opcoes);
    }

    /**
     * Célula até {@value #CASAS_A_FRENTE} casas à frente do Pacman, parando antes
     * de uma parede. Se o Pacman ainda não se moveu, é a própria célula dele.
     */
    protected Posicao posicaoDeEmboscada(Labirinto labirinto) {
        int coluna = colunaDoAlvo();
        int linha = linhaDoAlvo();
        Direction direcao = getAlvo().getDirecao();
        if (direcao == null) {
            return new Posicao(coluna, linha);
        }

        for (int i = 0; i < CASAS_A_FRENTE; i++) {
            int proximaColuna = coluna + direcao.getDx();
            int proximaLinha = linha + direcao.getDy();
            if (labirinto.ehParede(proximaColuna, proximaLinha)) {
                break;
            }
            coluna = proximaColuna;
            linha = proximaLinha;
        }
        return new Posicao(coluna, linha);
    }
}