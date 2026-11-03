package view;

import controller.GameEngine;
import model.Direction;
import model.EstadoJogo;
import model.Fantasma;
import model.Fruta;
import model.Item;
import model.Labirinto;
import model.Pacman;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Só desenha o estado do {@link GameEngine} e repassa o teclado a ele.
 * Nenhuma regra do jogo vive aqui. O labirinto é escalado e centralizado para
 * caber na janela, com uma faixa no topo para o HUD (pontos e itens restantes).
 * <p>
 * Personagens e itens são desenhados com formas geométricas simples (marcadores
 * de posição). Para usar seus próprios sprites, basta trocar os métodos
 * {@code desenharPacman}, {@code desenharFantasma} e {@code desenharItem}.
 */
public class GamePanel extends JPanel {

    /** Altura da faixa do HUD, em células. */
    private static final int LINHAS_HUD = 2;

    private static final Color COR_FUNDO = Color.BLACK;
    private static final Color COR_PAREDE = new Color(33, 66, 255);
    private static final Color COR_PACMAN = new Color(255, 214, 0);
    private static final Color COR_PILULA = new Color(255, 224, 178);
    private static final Color COR_FRUTA = new Color(229, 57, 53);
    private static final Color COR_FOLHA = new Color(76, 175, 80);
    private static final Color COR_VITORIA = new Color(255, 214, 0);
    private static final Color COR_DERROTA = new Color(239, 83, 80);

    private static final Map<Integer, Direction> TECLAS = new HashMap<>();

    static {
        TECLAS.put(KeyEvent.VK_W, Direction.UP);
        TECLAS.put(KeyEvent.VK_UP, Direction.UP);
        TECLAS.put(KeyEvent.VK_S, Direction.DOWN);
        TECLAS.put(KeyEvent.VK_DOWN, Direction.DOWN);
        TECLAS.put(KeyEvent.VK_A, Direction.LEFT);
        TECLAS.put(KeyEvent.VK_LEFT, Direction.LEFT);
        TECLAS.put(KeyEvent.VK_D, Direction.RIGHT);
        TECLAS.put(KeyEvent.VK_RIGHT, Direction.RIGHT);
    }

    private final GameEngine engine;

    public GamePanel(GameEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("A engine não pode ser nula.");
        }
        this.engine = engine;
        setBackground(COR_FUNDO);
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int tecla = e.getKeyCode();
                if (tecla == KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                } else if (tecla == KeyEvent.VK_R) {
                    GamePanel.this.engine.reiniciar();
                } else {
                    Direction d = TECLAS.get(tecla);
                    if (d != null) {
                        GamePanel.this.engine.direcionarPacman(d);
                    }
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Labirinto lab = engine.getLabirinto();
        int celula = Math.min(getWidth() / lab.getColunas(),
                getHeight() / (lab.getLinhas() + LINHAS_HUD));
        if (celula < 1) {
            return;
        }
        int larguraMapa = celula * lab.getColunas();
        int offX = (getWidth() - larguraMapa) / 2;
        int topo = (getHeight() - celula * (lab.getLinhas() + LINHAS_HUD)) / 2;
        int offY = topo + celula * LINHAS_HUD;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        desenharHud(g2, celula, offX, topo, larguraMapa);
        desenharLabirinto(g2, lab, celula, offX, offY);
        for (Item item : engine.getItens()) {
            desenharItem(g2, item, celula, offX, offY);
        }
        for (Fantasma f : engine.getFantasmas()) {
            desenharFantasma(g2, f, celula, offX, offY);
        }
        desenharPacman(g2, engine.getPacman(), celula, offX, offY);

               EstadoJogo estado = engine.getEstado();
        if (estado == EstadoJogo.PRONTO) {
            desenharPronto(g2, lab, celula, offX, offY);
        } else if (estado == EstadoJogo.GAME_OVER) {
            desenharFim(g2, "GAME OVER", COR_DERROTA);
        } else if (estado == EstadoJogo.VITORIA) {
            desenharFim(g2, "VOCÊ VENCEU!", COR_VITORIA);
        }
        g2.dispose();
    }
    /** Faixa superior: pontuação à esquerda e itens restantes à direita, alinhados ao mapa. */
    /**
     * Faixa superior: pontuação à esquerda, vidas no centro e itens restantes à
     * direita.
     */
    private void desenharHud(Graphics2D g, int celula, int offX, int topo, int larguraMapa) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, (int) (celula * 0.8))));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int baseline = topo + (int) (celula * 1.3);

        g.drawString("PONTOS: " + engine.getPontuacao(), offX, baseline);

        String restantes = "ITENS: " + engine.getItensRestantes();
        g.drawString(restantes, offX + larguraMapa - fm.stringWidth(restantes), baseline);

        // Vidas: um círculo amarelo para cada vida restante.
        int diametro = (int) (celula * 0.8);
        int espaco = (int) (celula * 0.3);
        int vidas = engine.getVidas();
        int total = vidas * diametro + Math.max(0, vidas - 1) * espaco;
        int x = offX + (larguraMapa - total) / 2;
        int y = topo + (int) (celula * 0.55);
        g.setColor(COR_PACMAN);
        for (int i = 0; i < vidas; i++) {
            g.fillOval(x + i * (diametro + espaco), y, diametro, diametro);
        }
    }

    /** Contorna apenas as faces das paredes voltadas para corredores. */
    private void desenharLabirinto(Graphics2D g, Labirinto lab, int celula, int offX, int offY) {
        g.setColor(COR_PAREDE);
        g.setStroke(new BasicStroke(Math.max(2f, celula / 12f),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (int l = 0; l < lab.getLinhas(); l++) {
            for (int c = 0; c < lab.getColunas(); c++) {
                if (!lab.ehParede(c, l)) {
                    continue;
                }
                int x0 = offX + c * celula;
                int y0 = offY + l * celula;
                int x1 = x0 + celula;
                int y1 = y0 + celula;

                if (!lab.ehParede(c, l - 1)) {
                    g.drawLine(x0, y0, x1, y0);
                }
                if (!lab.ehParede(c, l + 1)) {
                    g.drawLine(x0, y1, x1, y1);
                }
                if (!lab.ehParede(c - 1, l)) {
                    g.drawLine(x0, y0, x0, y1);
                }
                if (!lab.ehParede(c + 1, l)) {
                    g.drawLine(x1, y0, x1, y1);
                }
            }
        }
    }

    private void desenharItem(Graphics2D g, Item item, int celula, int offX, int offY) {
        int cx = offX + (int) Math.round((item.getX() + 0.5) * celula);
        int cy = offY + (int) Math.round((item.getY() + 0.5) * celula);
        int d = Math.max(2, (int) Math.round(item.getTamanho() * celula));

        if (item instanceof Fruta) {
            g.setColor(COR_FRUTA);
            g.fillOval(cx - d / 2, cy - d / 2 + d / 8, d, d);
            g.setColor(COR_FOLHA);
            g.fillOval(cx, cy - d / 2 - d / 8, Math.max(2, d / 3), Math.max(2, d / 4));
        } else {
            g.setColor(COR_PILULA);
            g.fillOval(cx - d / 2, cy - d / 2, d, d);
        }
    }

    private void desenharPacman(Graphics2D g, Pacman p, int celula, int offX, int offY) {
        int lado = (int) Math.round(p.getTamanho() * celula);
        int px = offX + (int) Math.round((p.getX() + 0.5) * celula) - lado / 2;
        int py = offY + (int) Math.round((p.getY() + 0.5) * celula) - lado / 2;
        g.setColor(COR_PACMAN);
        g.fillOval(px, py, lado, lado);
    }

    private void desenharFantasma(Graphics2D g, Fantasma f, int celula, int offX, int offY) {
        int lado = (int) Math.round(f.getTamanho() * celula);
        int px = offX + (int) Math.round((f.getX() + 0.5) * celula) - lado / 2;
        int py = offY + (int) Math.round((f.getY() + 0.5) * celula) - lado / 2;
        g.setColor(new Color(f.getCor()));
        g.fillRoundRect(px, py, lado, lado, lado / 3, lado / 3);
    }

    /** "PRONTO!" logo abaixo da casa dos fantasmas, como no jogo clássico. */
    private void desenharPronto(Graphics2D g, Labirinto lab, int celula, int offX, int offY) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, celula)));
        g.setColor(COR_PACMAN);
        String texto = "PRONTO!";
        FontMetrics fm = g.getFontMetrics();
        int centro = offX + lab.getColunas() * celula / 2;
        int baseline = offY + (lab.getLinhas() / 2 + 2) * celula + (int) (celula * 0.8);
        g.drawString(texto, centro - fm.stringWidth(texto) / 2, baseline);
    }

    /**
     * Tela de fim de partida (derrota ou vitória): título, pontuação final e
     * instruções.
     */
    private void desenharFim(Graphics2D g, String titulo, Color corTitulo) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, getWidth(), getHeight());

        int base = Math.max(12, Math.min(getWidth(), getHeight()) / 20);
        int meio = getHeight() / 2;

        g.setColor(corTitulo);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, base * 2));
        centralizar(g, titulo, meio - base / 2);

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, base));
        centralizar(g, "Pontuação final: " + engine.getPontuacao(), meio + base);
        centralizar(g, "R - jogar novamente     ESC - sair", meio + base * 2 + base / 2);
    }

    private void centralizar(Graphics2D g, String texto, int baseline) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(texto, (getWidth() - fm.stringWidth(texto)) / 2, baseline);
    }
}