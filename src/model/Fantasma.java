package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Personagem inimigo com movimento aleatório.
 * <p>
 * Regra: a cada centro de célula o fantasma olha as saídas livres (sem contar a
 * meia-volta). Em corredor ou curva ele simplesmente segue. Em uma interseção,
 * ele muda de direção com probabilidade {@code chanceDeMudar}; caso contrário
 * continua reto. Só dá meia-volta em beco sem saída.
 * <p>
 * Cada subclasse define cor, velocidade e chance de mudança próprias.
 */
public abstract class Fantasma extends Personagem {

    public static final double TAMANHO = 0.8;

    private final double chanceDeMudar;
    private final int cor; // 0xRRGGBB, para o modelo não depender de AWT
    private final Random random;

    protected Fantasma(int coluna, int linha, double velocidade,
                       double chanceDeMudar, int cor, Random random) {
        super(coluna, linha, TAMANHO, velocidade);
        if (!(chanceDeMudar >= 0.0 && chanceDeMudar <= 1.0)) {
            throw new IllegalArgumentException("A chance de mudar deve estar entre 0 e 1.");
        }
        if (random == null) {
            throw new IllegalArgumentException("O gerador aleatório não pode ser nulo.");
        }
        this.chanceDeMudar = chanceDeMudar;
        this.cor = cor;
        this.random = random;
    }

    public double getChanceDeMudar() {
        return chanceDeMudar;
    }

    /** Cor no formato 0xRRGGBB. */
    public int getCor() {
        return cor;
    }

    @Override
    protected Direction escolherDirecao(Labirinto labirinto) {
        Direction atual = getDirecao();

        List<Direction> opcoes = new ArrayList<>(4);
        for (Direction d : Direction.values()) {
            boolean ehMeiaVolta = atual != null && d == atual.opposite();
            if (!ehMeiaVolta && podeAndar(labirinto, d)) {
                opcoes.add(d);
            }
        }

        if (opcoes.isEmpty()) {
            // Beco sem saída: a única saída é voltar por onde veio.
            return atual == null ? null : atual.opposite();
        }

        boolean podeSeguirReto = atual != null && opcoes.contains(atual);
        if (podeSeguirReto) {
            if (opcoes.size() == 1 || random.nextDouble() >= chanceDeMudar) {
                return atual;
            }
            // Interseção e decidiu mudar: escolhe entre as outras saídas.
            List<Direction> outras = new ArrayList<>(opcoes);
            outras.remove(atual);
            return outras.get(random.nextInt(outras.size()));
        }

        // Curva (parede à frente) ou primeiro passo: sorteia entre as saídas livres.
        return opcoes.get(random.nextInt(opcoes.size()));
    }
}
