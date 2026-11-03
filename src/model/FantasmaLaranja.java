package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fantasma laranja, o mais simples: movimento aleatório. Em cada interseção
 * sorteia se muda de direção ou segue reto, sem olhar onde o Pacman está.
 */
public class FantasmaLaranja extends Fantasma {

    public static final double VELOCIDADE = 4.8;
    public static final double ATRASO_DE_SAIDA = 6.0;
    public static final double CHANCE_DE_MUDAR = 0.5;
    public static final int COR = 0xFB8C00;

    public FantasmaLaranja(int coluna, int linha, Pacman alvo, Random random) {
        super(coluna, linha, VELOCIDADE, COR, ATRASO_DE_SAIDA, alvo, random);
    }

    @Override
    protected Direction decidirDirecao(Labirinto labirinto, List<Direction> opcoes) {
        Direction atual = getDirecao();
        boolean podeSeguirReto = atual != null && opcoes.contains(atual);

        if (podeSeguirReto) {
            if (getRandom().nextDouble() >= CHANCE_DE_MUDAR) {
                return atual;
            }
            List<Direction> outras = new ArrayList<>(opcoes);
            outras.remove(atual);
            return outras.get(getRandom().nextInt(outras.size()));
        }
        return opcoes.get(getRandom().nextInt(opcoes.size()));
    }
}