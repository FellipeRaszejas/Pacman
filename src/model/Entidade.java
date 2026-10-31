package model;

/**
 * Qualquer elemento posicionado no labirinto.
 * <p>
 * A posição é guardada em unidades de célula (x = coluna, y = linha), e não em
 * pixels, então a lógica do jogo não depende do tamanho da tela. O valor
 * inteiro corresponde ao centro da célula; valores fracionários indicam que a
 * entidade está a meio caminho entre duas células.
 */
public abstract class Entidade {

    private double x;
    private double y;
    private final double tamanho; // fração da célula (0 < tamanho <= 1)

    protected Entidade(int coluna, int linha, double tamanho) {
        if (coluna < 0 || linha < 0) {
            throw new IllegalArgumentException("A posição inicial não pode ser negativa.");
        }
        if (!(tamanho > 0.0 && tamanho <= 1.0)) {
            throw new IllegalArgumentException(
                    "O tamanho deve estar entre 0 (exclusivo) e 1 (inclusivo).");
        }
        this.x = coluna;
        this.y = linha;
        this.tamanho = tamanho;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    /** Tamanho da entidade como fração da célula; vale para o desenho e para a colisão. */
    public double getTamanho() {
        return tamanho;
    }

    /** Só as subclasses movem a entidade; o mundo externo não "teletransporta" ninguém. */
    protected void setPosicao(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y) || x < 0 || y < 0) {
            throw new IllegalArgumentException("Posição inválida: (" + x + ", " + y + ").");
        }
        this.x = x;
        this.y = y;
    }

    /** Colisão por caixa delimitadora (retângulos centrados na posição de cada entidade). */
    public boolean colideCom(Entidade outra) {
        if (outra == null) {
            return false;
        }
        double limite = (this.tamanho + outra.tamanho) / 2.0;
        return Math.abs(this.x - outra.x) < limite && Math.abs(this.y - outra.y) < limite;
    }

    /** Avança a entidade {@code dt} segundos no labirinto. Cada tipo define o seu comportamento. */
    public abstract void atualizar(Labirinto labirinto, double dt);
}
