package model;

import java.util.List;
import java.util.Random;

/**
 * Fantasma vermelho, o perseguidor: a cada interseção escolhe a saída que o
 * deixa mais perto do Pacman, medindo a distância real pelos corredores.
 * É o mais rápido e o primeiro a sair da casa.
 */
public class FantasmaVermelho extends Fantasma {

    public static final double VELOCIDADE = 5.2;
    public static final double ATRASO_DE_SAIDA = 0.0;
    public static final int COR = 0xE53935;

    public FantasmaVermelho(int coluna, int linha, Pacman alvo, Random random) {
        super(coluna, linha, VELOCIDADE, COR, ATRASO_DE_SAIDA, alvo, random);
    }

    @Override
    protected Direction decidirDirecao(Labirinto labirinto, List<Direction> opcoes) {
        int[][] ate = labirinto.calcularDistancias(colunaDoAlvo(), linhaDoAlvo());
        return aproximar(ate, opcoes);
    }
}