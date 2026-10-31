import controller.GameEngine;
import model.Direction;
import model.EstadoJogo;
import model.Fantasma;
import model.FantasmaCiano;
import model.FantasmaLaranja;
import model.FantasmaRosa;
import model.FantasmaVermelho;
import model.Labirinto;
import model.Pacman;
import model.Personagem;
import model.Posicao;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Verificações da Fase 1, sem JUnit e sem janela.
 * <pre>
 *   javac -encoding UTF-8 -d out -sourcepath src test/Fase1Verificacao.java
 *   java -cp out Fase1Verificacao
 * </pre>
 */
public class Fase1Verificacao {

    private static final double DT = 1.0 / 60;

    private static int verificacoes = 0;
    private static int falhas = 0;

    public static void main(String[] args) {
        testeDirection();
        testeLabirintoPadrao();
        testeValidacaoDoLabirinto();
        testeValidacaoDasEntidades();
        testePacmanNaoAtravessaParedes();
        testePacmanComDtGigante();
        testeMeiaVoltaNoMeioDoCorredor();
        testeFantasmas();
        testeEstadoInicialDoJogo();
        testeColisaoGeraGameOver();

        System.out.println();
        System.out.println(verificacoes + " verificações, " + falhas + " falhas.");
        if (falhas > 0) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static void check(boolean ok, String descricao) {
        verificacoes++;
        if (!ok) {
            falhas++;
            System.out.println("FALHA: " + descricao);
        }
    }

    /** O personagem está sempre dentro do mapa e apenas sobre corredores (origem e destino do passo). */
    private static boolean posicaoValida(Personagem p, Labirinto lab) {
        double x = p.getX();
        double y = p.getY();
        if (x < 0 || y < 0 || x > lab.getColunas() - 1 || y > lab.getLinhas() - 1) {
            return false;
        }
        int x0 = (int) Math.floor(x + 1e-9);
        int y0 = (int) Math.floor(y + 1e-9);
        int x1 = (int) Math.ceil(x - 1e-9);
        int y1 = (int) Math.ceil(y - 1e-9);
        return !lab.ehParede(x0, y0) && !lab.ehParede(x1, y1);
    }

    private static int vizinhosLivres(Labirinto lab, int coluna, int linha) {
        int n = 0;
        for (Direction d : Direction.values()) {
            if (!lab.ehParede(coluna + d.getDx(), linha + d.getDy())) {
                n++;
            }
        }
        return n;
    }

    private static int celulasLivres(Labirinto lab) {
        int n = 0;
        for (int l = 0; l < lab.getLinhas(); l++) {
            for (int c = 0; c < lab.getColunas(); c++) {
                if (!lab.ehParede(c, l)) {
                    n++;
                }
            }
        }
        return n;
    }

    private static boolean lanca(String[] mapa) {
        try {
            new Labirinto(mapa);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    // ------------------------------------------------------------------ testes

    private static void testeDirection() {
        boolean ok = true;
        for (Direction d : Direction.values()) {
            ok &= d.opposite().opposite() == d;
            ok &= d.getDx() + d.opposite().getDx() == 0;
            ok &= d.getDy() + d.opposite().getDy() == 0;
        }
        check(ok, "Direction.opposite() deve ser a direção contrária");
    }

    private static void testeLabirintoPadrao() {
        Labirinto lab = Labirinto.padrao();
        check(lab.getColunas() == 19 && lab.getLinhas() == 21, "labirinto padrão deve ser 19 x 21");
        check(lab.getIniciosFantasmas().size() == 4, "labirinto padrão deve ter 4 fantasmas");

        boolean bordaFechada = true;
        for (int c = 0; c < lab.getColunas(); c++) {
            bordaFechada &= lab.ehParede(c, 0) && lab.ehParede(c, lab.getLinhas() - 1);
        }
        for (int l = 0; l < lab.getLinhas(); l++) {
            bordaFechada &= lab.ehParede(0, l) && lab.ehParede(lab.getColunas() - 1, l);
        }
        check(bordaFechada, "borda do labirinto deve ser toda de paredes");

        check(lab.ehParede(-1, 5) && lab.ehParede(5, -1)
                        && lab.ehParede(99, 5) && lab.ehParede(5, 99),
                "fora do mapa deve contar como parede");

        Posicao p = lab.getInicioPacman();
        check(!lab.ehParede(p.getColuna(), p.getLinha()), "início do Pacman deve ser corredor");

        boolean simetrico = true;
        for (int l = 0; l < lab.getLinhas(); l++) {
            for (int c = 0; c < lab.getColunas(); c++) {
                simetrico &= lab.ehParede(c, l) == lab.ehParede(lab.getColunas() - 1 - c, l);
            }
        }
        check(simetrico, "mapa padrão deve ser simétrico na horizontal");

        try {
            lab.getIniciosFantasmas().add(new Posicao(1, 1));
            check(false, "lista de inícios dos fantasmas deve ser imutável");
        } catch (UnsupportedOperationException e) {
            check(true, "lista de inícios dos fantasmas é imutável");
        }
    }

    private static void testeValidacaoDoLabirinto() {
        check(!lanca(new String[] {"#######", "#P...G#", "#######"}), "mapa mínimo válido deve ser aceito");

        check(lanca(null), "mapa nulo deve ser rejeitado");
        check(lanca(new String[0]), "mapa vazio deve ser rejeitado");
        check(lanca(new String[] {"#####", "#P.G.", "#####"}), "borda aberta deve ser rejeitada");
        check(lanca(new String[] {"#####", "#..G#", "#####"}), "mapa sem Pacman deve ser rejeitado");
        check(lanca(new String[] {"#####", "#PPG#", "#####"}), "dois Pacmans devem ser rejeitados");
        check(lanca(new String[] {"#####", "#P..#", "#####"}), "mapa sem fantasma deve ser rejeitado");
        check(lanca(new String[] {"#####", "#P.G#", "####"}), "linhas de tamanhos diferentes devem ser rejeitadas");
        check(lanca(new String[] {"#####", "#P?G#", "#####"}), "caractere inválido deve ser rejeitado");
        check(lanca(new String[] {"#######", "#P#.#G#", "#######"}), "corredor inacessível deve ser rejeitado");
    }

    private static void testeValidacaoDasEntidades() {
        boolean lancou = false;
        try {
            new Pacman(-1, 0);
        } catch (IllegalArgumentException e) {
            lancou = true;
        }
        check(lancou, "posição inicial negativa deve ser rejeitada");

        lancou = false;
        try {
            new Pacman(1, 1).setDirecaoDesejada(null);
        } catch (IllegalArgumentException e) {
            lancou = true;
        }
        check(lancou, "direção desejada nula deve ser rejeitada");

        lancou = false;
        try {
            new FantasmaVermelho(1, 1, null);
        } catch (IllegalArgumentException e) {
            lancou = true;
        }
        check(lancou, "gerador aleatório nulo deve ser rejeitado");
    }

    private static void testePacmanNaoAtravessaParedes() {
        Labirinto lab = Labirinto.padrao();
        Posicao ini = lab.getInicioPacman();
        Pacman pacman = new Pacman(ini.getColuna(), ini.getLinha());
        Direction[] dirs = Direction.values();
        Random rnd = new Random(42);

        Set<String> visitadas = new HashSet<>();
        boolean sempreValido = true;
        for (int i = 0; i < 200_000; i++) {
            if (i % 15 == 0) {
                pacman.setDirecaoDesejada(dirs[rnd.nextInt(dirs.length)]);
            }
            pacman.atualizar(lab, DT);
            if (!posicaoValida(pacman, lab)) {
                sempreValido = false;
                break;
            }
            visitadas.add(pacman.getColuna() + "," + pacman.getLinha());
        }
        check(sempreValido, "Pacman nunca pode sair do mapa nem entrar em parede (200 mil quadros)");
        check(visitadas.size() > 80, "Pacman deve conseguir explorar o mapa (visitou " + visitadas.size() + " células)");

        // Encosta na parede e para: no início ele só tem corredor para a esquerda/direita.
        Pacman parado = new Pacman(ini.getColuna(), ini.getLinha());
        parado.setDirecaoDesejada(Direction.UP); // parede acima da posição inicial
        parado.atualizar(lab, 1.0);
        check(parado.getX() == ini.getColuna() && parado.getY() == ini.getLinha(),
                "Pacman deve ficar parado quando a direção desejada é uma parede");
    }

    private static void testePacmanComDtGigante() {
        Labirinto lab = Labirinto.padrao();
        Posicao ini = lab.getInicioPacman();
        Pacman pacman = new Pacman(ini.getColuna(), ini.getLinha());
        pacman.setDirecaoDesejada(Direction.RIGHT);
        pacman.atualizar(lab, 100.0); // 600 células de deslocamento em um único quadro
        check(posicaoValida(pacman, lab), "um dt gigante não pode atravessar paredes");
    }

    private static void testeMeiaVoltaNoMeioDoCorredor() {
        Labirinto lab = new Labirinto(new String[] {"#######", "#P...G#", "#######"});
        Pacman pacman = new Pacman(1, 1);
        pacman.setDirecaoDesejada(Direction.RIGHT);
        pacman.atualizar(lab, 0.25); // 6 * 0.25 = 1,5 célula
        check(Math.abs(pacman.getX() - 2.5) < 1e-6, "Pacman deve estar em x = 2,5 (estava " + pacman.getX() + ")");

        pacman.setDirecaoDesejada(Direction.LEFT);
        check(Math.abs(pacman.getX() - 2.5) < 1e-6, "meia-volta não pode teletransportar o Pacman");
        check(pacman.getDirecao() == Direction.LEFT, "meia-volta deve ser imediata no meio do corredor");

        pacman.atualizar(lab, 0.1); // 0,6 célula para a esquerda
        check(Math.abs(pacman.getX() - 1.9) < 1e-6, "após a meia-volta, x deve ser 1,9 (era " + pacman.getX() + ")");
    }

    // ----------------------------------------------------------------- fantasmas

    private static final class Estatisticas {
        boolean valido = true;
        boolean meiaVoltaSoEmBeco = true;
        int mudancas = 0;
        double distancia = 0;
        final Set<String> visitadas = new HashSet<>();
    }

    private static Estatisticas simular(Fantasma f, Labirinto lab, int quadros) {
        Estatisticas s = new Estatisticas();
        Direction anterior = null;
        for (int i = 0; i < quadros; i++) {
            f.atualizar(lab, DT);
            s.distancia += f.getVelocidade() * DT;
            if (!posicaoValida(f, lab)) {
                s.valido = false;
                break;
            }
            s.visitadas.add(f.getColuna() + "," + f.getLinha());

            Direction atual = f.getDirecao();
            if (anterior != null && atual != anterior) {
                s.mudancas++;
                boolean meiaVolta = atual == anterior.opposite();
                if (meiaVolta && vizinhosLivres(lab, f.getColuna(), f.getLinha()) != 1) {
                    s.meiaVoltaSoEmBeco = false;
                }
            }
            anterior = atual;
        }
        return s;
    }

    private static void testeFantasmas() {
        Labirinto lab = Labirinto.padrao();
        List<Posicao> inicios = lab.getIniciosFantasmas();
        int livres = celulasLivres(lab);

        double mudancasVermelho = 0, distanciaVermelho = 0;
        double mudancasLaranja = 0, distanciaLaranja = 0;
        boolean todosValidos = true;
        boolean meiaVoltaOk = true;
        int menorCobertura = Integer.MAX_VALUE;

        for (long semente = 1; semente <= 5; semente++) {
            Random rnd = new Random(semente);
            Fantasma[] fantasmas = {
                    new FantasmaVermelho(inicios.get(0).getColuna(), inicios.get(0).getLinha(), rnd),
                    new FantasmaRosa(inicios.get(1).getColuna(), inicios.get(1).getLinha(), rnd),
                    new FantasmaCiano(inicios.get(2).getColuna(), inicios.get(2).getLinha(), rnd),
                    new FantasmaLaranja(inicios.get(3).getColuna(), inicios.get(3).getLinha(), rnd)
            };
            for (Fantasma f : fantasmas) {
                Estatisticas s = simular(f, lab, 30_000); // 500 s de jogo
                todosValidos &= s.valido;
                meiaVoltaOk &= s.meiaVoltaSoEmBeco;
                menorCobertura = Math.min(menorCobertura, s.visitadas.size());
                if (f instanceof FantasmaVermelho) {
                    mudancasVermelho += s.mudancas;
                    distanciaVermelho += s.distancia;
                } else if (f instanceof FantasmaLaranja) {
                    mudancasLaranja += s.mudancas;
                    distanciaLaranja += s.distancia;
                }
            }
        }

        check(todosValidos, "fantasmas nunca podem sair do mapa nem entrar em parede");
        check(meiaVoltaOk, "fantasmas só podem dar meia-volta em beco sem saída");
        check(menorCobertura >= livres * 0.6,
                "cada fantasma deve explorar ao menos 60% do mapa (menor cobertura: "
                        + menorCobertura + " de " + livres + ")");

        double taxaVermelho = mudancasVermelho / distanciaVermelho;
        double taxaLaranja = mudancasLaranja / distanciaLaranja;
        System.out.printf("Mudanças de direção por célula percorrida: vermelho (chance %.2f) = %.3f | laranja (chance %.2f) = %.3f%n",
                FantasmaVermelho.CHANCE_DE_MUDAR, taxaVermelho, FantasmaLaranja.CHANCE_DE_MUDAR, taxaLaranja);
        check(taxaLaranja > taxaVermelho,
                "o fantasma com maior chance de mudar deve mudar de direção com mais frequência");
    }

    // --------------------------------------------------------------------- jogo

    private static void testeEstadoInicialDoJogo() {
        GameEngine engine = new GameEngine(Labirinto.padrao(), new Random(7));
        check(engine.getEstado() == EstadoJogo.JOGANDO, "o jogo deve começar em JOGANDO");
        check(engine.getFantasmas().size() == 4, "o jogo deve começar com 4 fantasmas");

        Set<Integer> cores = new HashSet<>();
        for (Fantasma f : engine.getFantasmas()) {
            cores.add(f.getCor());
        }
        check(cores.size() == 4, "os 4 fantasmas devem ter cores diferentes");

        for (int i = 0; i < 60; i++) {
            engine.atualizar(DT);
        }
        check(engine.getEstado() == EstadoJogo.JOGANDO,
                "sem se mexer por 1 s, o Pacman não deve perder (fantasmas começam longe)");
    }

    private static void testeColisaoGeraGameOver() {
        Labirinto lab = new Labirinto(new String[] {"#######", "#P...G#", "#######"});
        GameEngine engine = new GameEngine(lab, new Random(3));
        engine.direcionarPacman(Direction.RIGHT);

        int quadros = 0;
        while (engine.getEstado() == EstadoJogo.JOGANDO && quadros < 60 * 20) {
            engine.atualizar(DT);
            quadros++;
        }
        check(engine.getEstado() == EstadoJogo.GAME_OVER, "tocar em um fantasma deve causar GAME_OVER");
        check(engine.getPacman().colideCom(engine.getFantasmas().get(0)),
                "no game over, Pacman e fantasma devem estar colidindo");

        double px = engine.getPacman().getX();
        double gx = engine.getFantasmas().get(0).getX();
        for (int i = 0; i < 120; i++) {
            engine.atualizar(DT);
        }
        check(engine.getPacman().getX() == px && engine.getFantasmas().get(0).getX() == gx,
                "após o game over ninguém mais se move");

        engine.direcionarPacman(Direction.LEFT);
        check(engine.getPacman().getDirecaoDesejada() == Direction.RIGHT,
                "comandos do teclado são ignorados após o game over");

        engine.reiniciar();
        check(engine.getEstado() == EstadoJogo.JOGANDO, "reiniciar() deve voltar para JOGANDO");
        check(engine.getPacman().getX() == 1 && engine.getPacman().getY() == 1,
                "reiniciar() deve devolver o Pacman à posição inicial");
    }
}
