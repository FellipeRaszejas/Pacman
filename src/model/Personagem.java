package model;

/**
 * Entidade que anda pelo labirinto, de célula em célula, sempre em uma das
 * quatro direções.
 * <p>
 * O movimento é um <b>método-modelo</b> (template method): {@link #atualizar} cuida
 * de andar, respeitar paredes e nunca sair do mapa. Quem decide <i>para onde ir</i>
 * a cada centro de célula é o método abstrato {@link #escolherDirecao}, que o
 * {@link Pacman} (teclado) e os {@link Fantasma fantasmas} (aleatório) implementam
 * de formas diferentes. É aqui que acontece o polimorfismo do jogo.
 */
public abstract class Personagem extends Entidade {

    private static final double EPSILON = 1e-9;

    private int coluna;            // célula de origem do passo atual
    private int linha;
    private Direction direcao;     // direção do passo atual (última, se estiver parado)
    private double progresso;      // 0 = no centro da célula; vai até 1 = próxima célula
    private double velocidade;     // células por segundo

    protected Personagem(int coluna, int linha, double tamanho, double velocidade) {
        super(coluna, linha, tamanho);
        this.coluna = coluna;
        this.linha = linha;
        setVelocidade(velocidade);
    }

    /** Coluna da célula de origem do passo atual. */
    public int getColuna() {
        return coluna;
    }

    /** Linha da célula de origem do passo atual. */
    public int getLinha() {
        return linha;
    }

    /** Direção atual de movimento; mantém o último valor se o personagem estiver parado; null antes do primeiro passo. */
    public Direction getDirecao() {
        return direcao;
    }

    public double getVelocidade() {
        return velocidade;
    }

    protected final void setVelocidade(double velocidade) {
        if (!(velocidade > 0.0) || Double.isInfinite(velocidade)) {
            throw new IllegalArgumentException("A velocidade deve ser positiva.");
        }
        this.velocidade = velocidade;
    }

    /**
     * Chamado sempre que o personagem está exatamente no centro de uma célula.
     * Retorna a direção do próximo passo, ou null para ficar parado.
     * A direção devolvida precisa estar livre; caso contrário o personagem para.
     */
    protected abstract Direction escolherDirecao(Labirinto labirinto);

    /** Indica se a célula vizinha na direção dada é um corredor (a partir da célula atual). */
    protected final boolean podeAndar(Labirinto labirinto, Direction d) {
        return !labirinto.ehParede(coluna + d.getDx(), linha + d.getDy());
    }

    @Override
    public final void atualizar(Labirinto labirinto, double dt) {
        if (labirinto == null) {
            throw new IllegalArgumentException("O labirinto não pode ser nulo.");
        }
        if (!(dt > 0.0)) {
            return;
        }

        double restante = velocidade * dt /2;
        while (restante > EPSILON) {
            if (progresso == 0.0) {
                // No centro de uma célula: hora de decidir para onde ir.
                Direction nova = escolherDirecao(labirinto);
                if (nova == null || !podeAndar(labirinto, nova)) {
                    break; // parede ou sem decisão: fica parado nesta célula
                }
                direcao = nova;
            }

            double passo = Math.min(restante, 1.0 - progresso);
            progresso += passo;
            restante -= passo;

            if (progresso >= 1.0 - EPSILON) {
                coluna += direcao.getDx();
                linha += direcao.getDy();
                progresso = 0.0;
            }
        }
        sincronizarPosicao();
    }

    /**
     * Permite meia-volta imediata no meio do corredor (como no Pac-Man clássico).
     * Só tem efeito se a nova direção for exatamente a oposta à atual.
     */
    protected final void inverterSeEmTransito(Direction nova) {
        if (nova != null && direcao != null && progresso > 0.0 && nova == direcao.opposite()) {
            coluna += direcao.getDx();
            linha += direcao.getDy();
            progresso = 1.0 - progresso;
            direcao = nova;
            sincronizarPosicao();
        }
    }

    private void sincronizarPosicao() {
        double dx = (direcao == null) ? 0.0 : direcao.getDx() * progresso;
        double dy = (direcao == null) ? 0.0 : direcao.getDy() * progresso;
        setPosicao(coluna + dx, linha + dy);
    }
}
