package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Personagem inimigo. Todos os fantasmas andam igual (herdam o movimento em grade
 * de {@link Personagem}) e respeitam as mesmas regras de trajeto: nunca atravessam
 * paredes e só dão meia-volta em beco sem saída. O que muda de um para outro é
 * <b>como escolhem a direção</b> quando têm mais de uma saída: é o método abstrato
 * {@link #decidirDirecao}, que cada subclasse implementa à sua maneira.
 * <p>
 * Corredores e curvas (uma só saída) são resolvidos aqui, sem consultar a subclasse.
 */
public abstract class Fantasma extends Personagem {

    public static final double TAMANHO = 0.8;
    /** Fração da velocidade normal enquanto o fantasma está assustado. */
    public static final double FATOR_ASSUSTADO = 0.6;

    private final int cor; // 0xRRGGBB, para o modelo não depender de AWT
    private final double atrasoDeSaida;
    private final Pacman alvo;
    private final Random random;
    private final double velocidadeNormal;
    private boolean assustado;
    private boolean regenerando;

    /**
     * @param atrasoDeSaida segundos que o fantasma espera parado no início de cada rodada
     * @param alvo          o Pacman que o fantasma pode perseguir
     */
    protected Fantasma(int coluna, int linha, double velocidade, int cor,
                       double atrasoDeSaida, Pacman alvo, Random random) {
        super(coluna, linha, TAMANHO, velocidade);
        if (!(atrasoDeSaida >= 0.0)) {
            throw new IllegalArgumentException("O atraso de saída não pode ser negativo.");
        }
        if (alvo == null) {
            throw new IllegalArgumentException("O alvo (Pacman) não pode ser nulo.");
        }
        if (random == null) {
            throw new IllegalArgumentException("O gerador aleatório não pode ser nulo.");
        }
        this.velocidadeNormal = velocidade;
        this.cor = cor;
        this.atrasoDeSaida = atrasoDeSaida;
        this.alvo = alvo;
        this.random = random;
    }

    /** Cor no formato 0xRRGGBB. Subclasses podem mudar de cor conforme o estado. */
    public int getCor() {
        return cor;
    }

    public double getAtrasoDeSaida() {
        return atrasoDeSaida;
    }

    /** Assustado = o Pacman está poderoso: o fantasma foge dele, mais devagar, e pode ser devorado. */
    public boolean isAssustado() {
        return assustado;
    }

    /**
     * Regenerando = foi devorado e espera na casa para voltar. Fica inofensivo e não
     * pode ser devorado de novo até sair.
     */
    public boolean isRegenerando() {
        return regenerando;
    }

    public void setRegenerando(boolean regenerando) {
        this.regenerando = regenerando;
    }

    public void setAssustado(boolean assustado) {
        this.assustado = assustado;
        setVelocidade(assustado ? velocidadeNormal * FATOR_ASSUSTADO : velocidadeNormal);
    }

    protected final Random getRandom() {
        return random;
    }

    /** Coluna da célula mais próxima do Pacman. */
    protected final int colunaDoAlvo() {
        return (int) Math.round(alvo.getX());
    }

    /** Linha da célula mais próxima do Pacman. */
    protected final int linhaDoAlvo() {
        return (int) Math.round(alvo.getY());
    }

    protected final Pacman getAlvo() {
        return alvo;
    }

    /**
     * Escolhe a direção entre duas ou mais saídas livres.
     *
     * @param opcoes saídas livres, sem a meia-volta; sempre tem 2 ou mais itens
     * @return uma das {@code opcoes}
     */
    protected abstract Direction decidirDirecao(Labirinto labirinto, List<Direction> opcoes);

    @Override
    protected final Direction escolherDirecao(Labirinto labirinto) {
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
        if (opcoes.size() == 1) {
            return opcoes.get(0); // corredor ou curva: não há o que decidir
        }

        Direction escolha;
        if (assustado) {
            // Medo é igual para todos: foge do Pacman, seja qual for a personalidade do fantasma.
            int[][] ate = labirinto.calcularDistancias(colunaDoAlvo(), linhaDoAlvo());
            escolha = afastar(ate, opcoes);
        } else {
            escolha = decidirDirecao(labirinto, opcoes);
        }
        if (!opcoes.contains(escolha)) {
            // Uma subclasse com defeito não pode quebrar o jogo nem atravessar parede.
            return opcoes.get(random.nextInt(opcoes.size()));
        }
        return escolha;
    }

    /**
     * Entre as opções, escolhe a que deixa o fantasma mais perto do alvo.
     *
     * @param distancias resultado de {@link Labirinto#calcularDistancias} até o alvo
     */
    protected final Direction aproximar(int[][] distancias, List<Direction> opcoes) {
        return melhorOpcao(distancias, opcoes, true);
    }

    /** Entre as opções, escolhe a que deixa o fantasma mais longe do alvo. */
    protected final Direction afastar(int[][] distancias, List<Direction> opcoes) {
        return melhorOpcao(distancias, opcoes, false);
    }

    private Direction melhorOpcao(int[][] distancias, List<Direction> opcoes, boolean menor) {
        List<Direction> melhores = new ArrayList<>(4);
        int melhorValor = 0;
        for (Direction d : opcoes) {
            int valor = distancias[getLinha() + d.getDy()][getColuna() + d.getDx()];
            if (melhores.isEmpty() || (menor ? valor < melhorValor : valor > melhorValor)) {
                melhores.clear();
                melhores.add(d);
                melhorValor = valor;
            } else if (valor == melhorValor) {
                melhores.add(d);
            }
        }
        // Empate: sorteia, para o fantasma não ficar previsível demais.
        return melhores.get(random.nextInt(melhores.size()));
    }
}