package controller;

import model.Direction;
import model.EstadoJogo;
import model.Fantasma;
import model.FantasmaCiano;
import model.FantasmaLaranja;
import model.FantasmaRosa;
import model.FantasmaVermelho;
import model.Fruta;
import model.Item;
import model.Labirinto;
import model.Pacman;
import model.Pilula;
import model.Posicao;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Coração do jogo: possui o loop e aplica as regras (movimento, colisão, coleta
 * de itens, pontuação, vitória e game over).
 * Não desenha nada; a view apenas lê o estado por meio dos getters.
 * <p>
 * {@link #atualizar(double)} é público para permitir testar a lógica sem janela.
 */
public class GameEngine {

    private static final int PERIODO_MS = 16;
    /** Evita que um travamento da janela vire um "salto" gigante dos personagens. */
    private static final double DT_MAXIMO = 0.05;

    private final Labirinto labirinto;
    private final Random random;
    private final List<Fantasma> fantasmas = new ArrayList<>();
    private final List<Item> itens = new ArrayList<>();
    private Pacman pacman;
    private int pontuacao;
    private EstadoJogo estado;

    private Timer timer;
    private Runnable aoAtualizar;
    private long ultimoTickNs;

    public GameEngine(Labirinto labirinto) {
        this(labirinto, new Random());
    }

    public GameEngine(Labirinto labirinto, Random random) {
        if (labirinto == null || random == null) {
            throw new IllegalArgumentException("Labirinto e gerador aleatório são obrigatórios.");
        }
        this.labirinto = labirinto;
        this.random = random;
        montarPartida();
    }

    // ---------------------------------------------------------------- partida

    /** Recomeça a partida: personagens nas posições iniciais, itens repostos e placar zerado. */
    public void reiniciar() {
        montarPartida();
    }

    private void montarPartida() {
        Posicao inicio = labirinto.getInicioPacman();
        pacman = new Pacman(inicio.getColuna(), inicio.getLinha());

        fantasmas.clear();
        List<Posicao> inicios = labirinto.getIniciosFantasmas();
        for (int i = 0; i < inicios.size(); i++) {
            fantasmas.add(criarFantasma(i, inicios.get(i)));
        }

        itens.clear();
        for (Posicao p : labirinto.getPosicoesPilulas()) {
            itens.add(new Pilula(p.getColuna(), p.getLinha()));
        }
        for (Posicao p : labirinto.getPosicoesFrutas()) {
            itens.add(new Fruta(p.getColuna(), p.getLinha()));
        }

        pontuacao = 0;
        estado = EstadoJogo.JOGANDO;
    }

    private Fantasma criarFantasma(int indice, Posicao p) {
        switch (indice % 4) {
            case 0:
                return new FantasmaVermelho(p.getColuna(), p.getLinha(), random);
            case 1:
                return new FantasmaRosa(p.getColuna(), p.getLinha(), random);
            case 2:
                return new FantasmaCiano(p.getColuna(), p.getLinha(), random);
            default:
                return new FantasmaLaranja(p.getColuna(), p.getLinha(), random);
        }
    }

    // ------------------------------------------------------------------- loop

    /** Inicia o loop do jogo. {@code aoAtualizar} é chamado após cada quadro (ex.: repaint). */
    public void iniciar(Runnable aoAtualizar) {
        this.aoAtualizar = aoAtualizar;
        this.ultimoTickNs = System.nanoTime();
        this.timer = new Timer(PERIODO_MS, e -> tick());
        this.timer.start();
    }

    public void parar() {
        if (timer != null) {
            timer.stop();
        }
    }

    private void tick() {
        long agora = System.nanoTime();
        double dt = (agora - ultimoTickNs) / 1_000_000_000.0;
        ultimoTickNs = agora;

        atualizar(dt);

        if (aoAtualizar != null) {
            aoAtualizar.run();
        }
    }

    // ----------------------------------------------------------------- regras

    /** Avança o jogo {@code dt} segundos. Com game over ou vitória, nada mais se move. */
    public void atualizar(double dt) {
        if (estado != EstadoJogo.JOGANDO) {
            return;
        }
        double passo = Math.min(dt, DT_MAXIMO);

        pacman.atualizar(labirinto, passo);
        for (Fantasma f : fantasmas) {
            f.atualizar(labirinto, passo);
        }

        // A derrota tem prioridade: se o Pacman pega o último item e encosta
        // em um fantasma no mesmo quadro, o jogador perde.
        verificarColisoes();
        if (estado == EstadoJogo.JOGANDO) {
            coletarItens();
            verificarVitoria();
        }
    }

    private void verificarColisoes() {
        for (Fantasma f : fantasmas) {
            if (pacman.colideCom(f)) {
                estado = EstadoJogo.GAME_OVER;
                return;
            }
        }
    }

    /** Remove da lista todo item tocado pelo Pacman e soma seus pontos (polimorfismo: item.getPontos()). */
    private void coletarItens() {
        // Laço reverso: remover pelo índice é seguro durante a iteração.
        for (int i = itens.size() - 1; i >= 0; i--) {
            Item item = itens.get(i);
            if (pacman.colideCom(item)) {
                pontuacao += item.getPontos();
                itens.remove(i);
            }
        }
    }

    private void verificarVitoria() {
        if (itens.isEmpty()) {
            estado = EstadoJogo.VITORIA;
        }
    }

    /** Comando do jogador (teclado). Ignorado depois do fim da partida. */
    public void direcionarPacman(Direction direcao) {
        if (estado == EstadoJogo.JOGANDO) {
            pacman.setDirecaoDesejada(direcao);
        }
    }

    // ---------------------------------------------------------------- leitura

    public Labirinto getLabirinto() {
        return labirinto;
    }

    public Pacman getPacman() {
        return pacman;
    }

    public List<Fantasma> getFantasmas() {
        return Collections.unmodifiableList(fantasmas);
    }

    /** Itens que ainda não foram coletados. */
    public List<Item> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public int getItensRestantes() {
        return itens.size();
    }

    public int getPontuacao() {
        return pontuacao;
    }

    public EstadoJogo getEstado() {
        return estado;
    }
}