package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Mapa do jogo em grade, definido por uma matriz de caracteres:
 * <ul>
 *   <li>{@code #} parede</li>
 *   <li>{@code .} corredor com uma pílula</li>
 *   <li>{@code F} corredor com uma fruta</li>
 *   <li>{@code ' '} (espaço) corredor vazio, sem item</li>
 *   <li>{@code P} início do Pacman (corredor vazio)</li>
 *   <li>{@code G} início de um fantasma (corredor vazio)</li>
 * </ul>
 * O mapa é validado na construção: retangular, com a borda toda fechada por
 * paredes, um único Pacman, ao menos um fantasma, ao menos um item e todos os
 * corredores alcançáveis a partir do Pacman. Qualquer posição fora do mapa conta
 * como parede, então nenhum personagem consegue sair dele.
 * <p>
 * Todos os métodos usam a ordem (coluna, linha), ou seja, (x, y).
 */
public class Labirinto {

    public static final char PAREDE = '#';
    public static final char PILULA = '.';
    public static final char FRUTA = 'F';
    public static final char VAZIO = ' ';
    public static final char INICIO_PACMAN = 'P';
    public static final char INICIO_FANTASMA = 'G';

    // Mapa simplificado 19 x 21, simétrico, com a "casa" dos fantasmas no centro.
    private static final String[] MAPA_PADRAO = {
            "###################",
            "#F.......#.......F#",
            "#.##.###.#.###.##.#",
            "#.................#",
            "#.##.#.#####.#.##.#",
            "#....#...#...#....#",
            "####.###.#.###.####",
            "####.#.......#.####",
            "####.#.## ##.#.####",
            "#......#G G#......#",
            "####.#.#G G#.#.####",
            "####.#.#####.#.####",
            "####.#.......#.####",
            "#........#........#",
            "#.##.###.#.###.##.#",
            "#..#.....P.....#..#",
            "##.#.#.#####.#.#.##",
            "#....#...#...#....#",
            "#.######.#.######.#",
            "#F...............F#",
            "###################"
    };

    private final boolean[][] paredes; // [linha][coluna]
    private final int colunas;
    private final int linhas;
    private final Posicao inicioPacman;
    private final List<Posicao> iniciosFantasmas;
    private final List<Posicao> posicoesPilulas;
    private final List<Posicao> posicoesFrutas;

    /** Cria o labirinto padrão do jogo. */
    public static Labirinto padrao() {
        return new Labirinto(MAPA_PADRAO);
    }

    public Labirinto(String[] mapa) {
        if (mapa == null || mapa.length == 0) {
            throw new IllegalArgumentException("O mapa não pode ser vazio.");
        }
        for (int l = 0; l < mapa.length; l++) {
            if (mapa[l] == null) {
                throw new IllegalArgumentException("A linha " + l + " do mapa é nula.");
            }
        }

        this.linhas = mapa.length;
        this.colunas = mapa[0].length();
        if (colunas == 0) {
            throw new IllegalArgumentException("O mapa não pode ter linhas vazias.");
        }

        boolean[][] grade = new boolean[linhas][colunas];
        Posicao pacman = null;
        List<Posicao> fantasmas = new ArrayList<>();
        List<Posicao> pilulas = new ArrayList<>();
        List<Posicao> frutas = new ArrayList<>();

        for (int l = 0; l < linhas; l++) {
            if (mapa[l].length() != colunas) {
                throw new IllegalArgumentException(
                        "A linha " + l + " tem tamanho diferente das demais.");
            }
            for (int c = 0; c < colunas; c++) {
                char ch = mapa[l].charAt(c);
                switch (ch) {
                    case PAREDE:
                        grade[l][c] = true;
                        break;
                    case PILULA:
                        pilulas.add(new Posicao(c, l));
                        break;
                    case FRUTA:
                        frutas.add(new Posicao(c, l));
                        break;
                    case VAZIO:
                        break;
                    case INICIO_PACMAN:
                        if (pacman != null) {
                            throw new IllegalArgumentException(
                                    "O mapa deve ter apenas um início do Pacman ('P').");
                        }
                        pacman = new Posicao(c, l);
                        break;
                    case INICIO_FANTASMA:
                        fantasmas.add(new Posicao(c, l));
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Caractere inválido '" + ch + "' em (" + c + "," + l + ").");
                }
            }
        }

        if (pacman == null) {
            throw new IllegalArgumentException("O mapa precisa de um início do Pacman ('P').");
        }
        if (fantasmas.isEmpty()) {
            throw new IllegalArgumentException("O mapa precisa de ao menos um fantasma ('G').");
        }
        if (pilulas.isEmpty() && frutas.isEmpty()) {
            throw new IllegalArgumentException(
                    "O mapa precisa de ao menos um item ('.' ou 'F'); senão o jogo já começa vencido.");
        }

        this.paredes = grade;
        this.inicioPacman = pacman;
        this.iniciosFantasmas = Collections.unmodifiableList(fantasmas);
        this.posicoesPilulas = Collections.unmodifiableList(pilulas);
        this.posicoesFrutas = Collections.unmodifiableList(frutas);

        validarBordaFechada();
        validarConectividade();
    }

    private void validarBordaFechada() {
        for (int c = 0; c < colunas; c++) {
            if (!paredes[0][c] || !paredes[linhas - 1][c]) {
                throw new IllegalArgumentException(
                        "A borda do mapa deve ser toda de paredes (coluna " + c + ").");
            }
        }
        for (int l = 0; l < linhas; l++) {
            if (!paredes[l][0] || !paredes[l][colunas - 1]) {
                throw new IllegalArgumentException(
                        "A borda do mapa deve ser toda de paredes (linha " + l + ").");
            }
        }
    }

    /** Garante que todo corredor pode ser alcançado a partir do Pacman. */
    private void validarConectividade() {
        int livres = 0;
        for (int l = 0; l < linhas; l++) {
            for (int c = 0; c < colunas; c++) {
                if (!paredes[l][c]) {
                    livres++;
                }
            }
        }

        boolean[][] visitada = new boolean[linhas][colunas];
        Deque<Posicao> fila = new ArrayDeque<>();
        fila.add(inicioPacman);
        visitada[inicioPacman.getLinha()][inicioPacman.getColuna()] = true;
        int alcancadas = 0;

        while (!fila.isEmpty()) {
            Posicao atual = fila.poll();
            alcancadas++;
            for (Direction d : Direction.values()) {
                int c = atual.getColuna() + d.getDx();
                int l = atual.getLinha() + d.getDy();
                if (!ehParede(c, l) && !visitada[l][c]) {
                    visitada[l][c] = true;
                    fila.add(new Posicao(c, l));
                }
            }
        }

        if (alcancadas != livres) {
            throw new IllegalArgumentException(
                    "Existem corredores inacessíveis a partir do Pacman ("
                            + (livres - alcancadas) + " células).");
        }
    }

    public int getColunas() {
        return colunas;
    }

    public int getLinhas() {
        return linhas;
    }

    /** Fora do mapa conta como parede: ninguém atravessa as bordas. */
    public boolean ehParede(int coluna, int linha) {
        if (coluna < 0 || linha < 0 || coluna >= colunas || linha >= linhas) {
            return true;
        }
        return paredes[linha][coluna];
    }

    public Posicao getInicioPacman() {
        return inicioPacman;
    }

    /** Lista imutável com a posição inicial de cada fantasma. */
    public List<Posicao> getIniciosFantasmas() {
        return iniciosFantasmas;
    }

    /** Lista imutável com a posição de cada pílula do mapa. */
    public List<Posicao> getPosicoesPilulas() {
        return posicoesPilulas;
    }

    /** Lista imutável com a posição de cada fruta do mapa. */
    public List<Posicao> getPosicoesFrutas() {
        return posicoesFrutas;
    }
}