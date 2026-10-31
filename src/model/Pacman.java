package model;

/**
 * Personagem controlado pelo jogador.
 * <p>
 * O teclado apenas registra a direção <i>desejada</i>. O Pacman segue nela assim
 * que o caminho estiver livre e, enquanto isso, continua na direção atual até
 * bater em uma parede (comportamento clássico).
 */
public class Pacman extends Personagem {

    public static final double TAMANHO = 0.8;
    public static final double VELOCIDADE = 6.0; // células por segundo

    private Direction direcaoDesejada;

    public Pacman(int coluna, int linha) {
        super(coluna, linha, TAMANHO, VELOCIDADE);
    }

    public Direction getDirecaoDesejada() {
        return direcaoDesejada;
    }

    /** Registra o comando do jogador. Meia-volta no meio do corredor é imediata. */
    public void setDirecaoDesejada(Direction direcao) {
        if (direcao == null) {
            throw new IllegalArgumentException("A direção não pode ser nula.");
        }
        this.direcaoDesejada = direcao;
        inverterSeEmTransito(direcao);
    }

    @Override
    protected Direction escolherDirecao(Labirinto labirinto) {
        if (direcaoDesejada != null && podeAndar(labirinto, direcaoDesejada)) {
            return direcaoDesejada;
        }
        Direction atual = getDirecao();
        if (atual != null && podeAndar(labirinto, atual)) {
            return atual;
        }
        return null; // encostou na parede: espera um comando válido
    }
}
