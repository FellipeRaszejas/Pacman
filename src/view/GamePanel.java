package view;

import controller.GameEngine;
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

import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Só desenha o estado do {@link GameEngine} e repassa o teclado e o mouse a ele.
 * Nenhuma regra do jogo vive aqui.
 * <p>
 * Telas, em ordem: <b>título</b> (imagem de início com o botão PLAY) → <b>escolha</b> do
 * Pacman → <b>jogo</b>. Na vitória (e só nela) aparece uma imagem em pixel art.
 * Os fantasmas e o Pacman são desenhados só com as imagens PNG (fundo transparente);
 * se uma imagem não for encontrada, o personagem cai num desenho simples.
 */
public class GamePanel extends JPanel {

    /** As telas que existem antes e durante a partida. */
    private enum Tela {
        TITULO, ESCOLHA, JOGO
    }

    /** Altura da faixa do HUD, em células. */
    private static final int LINHAS_HUD = 2;

    private static final Color COR_FUNDO = Color.BLACK;
    private static final Color COR_PAREDE = new Color(33, 66, 255);
    private static final Color COR_PACMAN = new Color(255, 214, 0);
    private static final Color COR_PILULA = new Color(255, 224, 178);
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

    /** Tamanho das imagens dos personagens, como fração da célula. */
    private static final double TAMANHO_AVATAR = 1.1;
    /** Contorno amarelo no Pacman, para ele não se confundir com os fantasmas. */
    private static final boolean CONTORNO_PACMAN = true;
    /** Cor e intensidade da camada azulada sobre o fantasma quando ele foge (Ciano). */
    private static final float FORCA_FUGA = 0.55f;
    /** Camada sobre o fantasma assustado (Pacman poderoso) e, no fim do poder, a que pisca. */
    private static final int COR_ASSUSTADO = 0x2D4BFF;
    private static final int COR_PISCA = 0xFFFFFF;
    private static final float FORCA_ASSUSTADO = 0.6f;
    /** Nos últimos segundos do poder, os fantasmas começam a piscar. */
    private static final double AVISO_FIM_DO_PODER = 3.0;

    /** Um personagem que o jogador pode escolher para ser o Pacman. */
    private static final class Opcao {
        final String nome;
        final Avatar avatar;

        Opcao(String nome, Avatar avatar) {
            this.nome = nome;
            this.avatar = avatar;
        }
    }

    private final GameEngine engine;
    /** Qual imagem cada tipo de fantasma usa. Tipo sem imagem cai no desenho simples. */
    private final Map<Class<? extends Fantasma>, Avatar> avatares = new HashMap<>();
    /** Os rostos que aparecem na tela de título (os mesmos dos fantasmas). */
    private final List<Avatar> rostosDoTitulo = new ArrayList<>();
    private final List<Opcao> opcoes = new ArrayList<>();
    private final List<Rectangle> cartas = new ArrayList<>();
    private final Rectangle botaoIniciar = new Rectangle();
    private boolean mouseNoBotao;

    private Tela tela = Tela.TITULO;
    private int escolhido;
    private Runnable aoAlternarSom;
    private Runnable aoIniciarPartida;

    /** Região do botão PLAY dentro da imagem de início, como fração da imagem (x, y, largura, altura). */
    private static final double[] PLAY_NA_IMAGEM = {0.835, 0.455, 0.155, 0.175};

    private final BufferedImage imagemInicio;
    private final BufferedImage imagemVitoria;
    /** Cada imagem grande já escalada para o tamanho atual da janela (evita reescalar a cada quadro). */
    private final Map<BufferedImage, BufferedImage> escaladas = new HashMap<>();

    public GamePanel(GameEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("A engine não pode ser nula.");
        }
        this.engine = engine;
        setBackground(COR_FUNDO);
        setFocusable(true);
        carregarAvatares();
        carregarOpcoesDePacman();
        imagemInicio = CarregadorDeImagens.carregar("image/inicio.jpg");
        imagemVitoria = CarregadorDeImagens.carregar("image/vitoria.jpg");

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                aoApertarTecla(e.getKeyCode());
            }
        });

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean dentro = tela == Tela.TITULO && botaoIniciar.contains(e.getPoint());
                if (dentro != mouseNoBotao) {
                    mouseNoBotao = dentro;
                    repaint();
                }
                boolean clicavel = dentro || (tela == Tela.ESCOLHA && cartaSob(e.getPoint()) >= 0);
                setCursor(Cursor.getPredefinedCursor(clicavel ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
                if (tela == Tela.TITULO && botaoIniciar.contains(e.getPoint())) {
                    avancarDoTitulo();
                } else if (tela == Tela.ESCOLHA) {
                    int carta = cartaSob(e.getPoint());
                    if (carta >= 0) {
                        escolhido = carta;
                        confirmarEscolha();
                    }
                }
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    /** Ação executada quando o jogador começa a partida (normalmente, iniciar o loop da engine). */
    public void setAoIniciarPartida(Runnable aoIniciarPartida) {
        this.aoIniciarPartida = aoIniciarPartida;
    }

    /** Ação executada ao apertar M (ligar/desligar o som). */
    public void setAoAlternarSom(Runnable aoAlternarSom) {
        this.aoAlternarSom = aoAlternarSom;
    }

    // ------------------------------------------------------------ carregamento

    /** Uma imagem para cada fantasma. Para trocar quem é quem, mude as linhas abaixo. */
    private void carregarAvatares() {
        registrar(FantasmaVermelho.class, criarAvatar("image/fantasma1.png"));
        registrar(FantasmaRosa.class, criarAvatar("image/fantasma2.png"));
        registrar(FantasmaCiano.class, criarAvatar("image/deiv.png"));
        registrar(FantasmaLaranja.class, criarAvatar("image/glauco.png"));
        for (Class<? extends Fantasma> tipo : List.of(FantasmaVermelho.class, FantasmaRosa.class,
                FantasmaCiano.class, FantasmaLaranja.class)) {
            if (avatares.containsKey(tipo)) {
                rostosDoTitulo.add(avatares.get(tipo));
            }
        }
    }

    /** Personagens que podem ser o Pacman. Só entram os que tiverem imagem. */
    private void carregarOpcoesDePacman() {
        adicionarOpcao("Fellipe", criarAvatar("image/fellipe.png"));
        adicionarOpcao("Ana Júlia", criarAvatar("image/anajulia.png"));
    }

    private void adicionarOpcao(String nome, Avatar avatar) {
        if (avatar != null) {
            opcoes.add(new Opcao(nome, avatar));
        }
    }

    private static Avatar criarAvatar(String caminho) {
        BufferedImage imagem = CarregadorDeImagens.carregar(caminho);
        return (imagem != null) ? new Avatar(imagem) : null;
    }

    private void registrar(Class<? extends Fantasma> tipo, Avatar avatar) {
        if (avatar != null) {
            avatares.put(tipo, avatar);
        }
    }

    // ------------------------------------------------------------------ entrada

    private void aoApertarTecla(int tecla) {
        if (tecla == KeyEvent.VK_ESCAPE) {
            System.exit(0);
        } else if (tecla == KeyEvent.VK_M) {
            if (aoAlternarSom != null) {
                aoAlternarSom.run();
            }
        } else if (tela == Tela.TITULO) {
            if (tecla == KeyEvent.VK_ENTER || tecla == KeyEvent.VK_SPACE) {
                avancarDoTitulo();
            }
        } else if (tela == Tela.ESCOLHA) {
            teclaNaEscolha(tecla);
        } else if (tecla == KeyEvent.VK_R) {
            engine.reiniciar();
        } else {
            Direction d = TECLAS.get(tecla);
            if (d != null) {
                engine.direcionarPacman(d);
            }
        }
        repaint();
    }

    private void teclaNaEscolha(int tecla) {
        if (tecla == KeyEvent.VK_LEFT || tecla == KeyEvent.VK_A) {
            escolhido = (escolhido + opcoes.size() - 1) % opcoes.size();
        } else if (tecla == KeyEvent.VK_RIGHT || tecla == KeyEvent.VK_D) {
            escolhido = (escolhido + 1) % opcoes.size();
        } else if (tecla == KeyEvent.VK_1 || tecla == KeyEvent.VK_2) {
            int indice = tecla - KeyEvent.VK_1;
            if (indice < opcoes.size()) {
                escolhido = indice;
                confirmarEscolha();
            }
        } else if (tecla == KeyEvent.VK_ENTER || tecla == KeyEvent.VK_SPACE) {
            confirmarEscolha();
        }
    }

    private void avancarDoTitulo() {
        if (opcoes.isEmpty()) {
            iniciarPartida(); // nenhuma imagem de Pacman: pula a escolha
        } else {
            tela = Tela.ESCOLHA;
        }
        mouseNoBotao = false;
    }

    private void confirmarEscolha() {
        iniciarPartida();
    }

    private void iniciarPartida() {
        tela = Tela.JOGO;
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        if (aoIniciarPartida != null) {
            aoIniciarPartida.run();
        }
    }

    private int cartaSob(java.awt.Point p) {
        for (int i = 0; i < cartas.size(); i++) {
            if (cartas.get(i).contains(p)) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ desenho

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            if (tela == Tela.TITULO) {
                desenharTitulo(g2);
            } else if (tela == Tela.ESCOLHA) {
                desenharEscolha(g2);
            } else {
                desenharJogo(g2);
            }
        } finally {
            g2.dispose();
        }
    }

    private void desenharJogo(Graphics2D g2) {
        // Vitória: só ela ganha a tela com a imagem em pixel art.
        if (engine.getEstado() == EstadoJogo.VITORIA && imagemVitoria != null) {
            desenharVitoria(g2);
            return;
        }

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
            desenharFim(g2, "VOCÊ VENCEU!", COR_VITORIA); // só se a imagem da vitória faltar
        }
    }

    /** Faixa superior: pontuação à esquerda, vidas no centro e itens restantes à direita. */
    private void desenharHud(Graphics2D g, int celula, int offX, int topo, int larguraMapa) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, (int) (celula * 0.8))));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int baseline = topo + (int) (celula * 1.3);

        g.drawString("PONTOS: " + engine.getPontuacao(), offX, baseline);

        String restantes = "ITENS: " + engine.getItensRestantes();
        g.drawString(restantes, offX + larguraMapa - fm.stringWidth(restantes), baseline);

        // Vidas: o rosto do Pacman escolhido (ou um círculo amarelo) para cada vida restante.
        int diametro = (int) (celula * 0.8);
        int espaco = (int) (celula * 0.3);
        int vidas = engine.getVidas();
        int total = vidas * diametro + Math.max(0, vidas - 1) * espaco;
        int x = offX + (larguraMapa - total) / 2;
        int y = topo + (int) (celula * 0.55);
        Avatar rosto = avatarDoPacman();
        g.setColor(COR_PACMAN);
        for (int i = 0; i < vidas; i++) {
            int xi = x + i * (diametro + espaco);
            if (rosto != null) {
                g.drawImage(rosto.obter(diametro), xi, y, null);
            } else {
                g.fillOval(xi, y, diametro, diametro);
            }
        }

        desenharBarraDePoder(g, celula, offX, topo, larguraMapa);
    }

    /** Barra que se esvazia enquanto o Pacman está poderoso. */
    private void desenharBarraDePoder(Graphics2D g, int celula, int offX, int topo, int larguraMapa) {
        if (!engine.isPoderAtivo()) {
            return;
        }
        double fracao = engine.getTempoDePoder() / GameEngine.DURACAO_PODER;
        int largura = (int) (larguraMapa * 0.32);
        int altura = Math.max(4, (int) (celula * 0.3));
        int x = offX + (larguraMapa - largura) / 2;
        int y = topo + (int) (celula * 1.55);

        g.setColor(new Color(255, 255, 255, 50));
        g.fillRoundRect(x, y, largura, altura, altura, altura);
        boolean acabando = engine.getTempoDePoder() < AVISO_FIM_DO_PODER;
        g.setColor(acabando ? new Color(239, 83, 80) : new Color(77, 208, 225));
        g.fillRoundRect(x, y, (int) Math.round(largura * fracao), altura, altura, altura);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, (int) (celula * 0.45))));
        g.setColor(Color.WHITE);
        String rotulo = "PODER";
        g.drawString(rotulo, x - g.getFontMetrics().stringWidth(rotulo) - celula / 3, y + altura);
    }

    private Avatar avatarDoPacman() {
        return opcoes.isEmpty() ? null : opcoes.get(escolhido).avatar;
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

        if (item instanceof Fruta) {
            desenharFruta(g, (Fruta) item, cx, cy, celula);
        } else {
            int d = Math.max(2, (int) Math.round(item.getTamanho() * celula));
            g.setColor(COR_PILULA);
            g.fillOval(cx - d / 2, cy - d / 2, d, d);
        }
    }

    // ------------------------------------------------------------------ frutas

    /** Fruta grande, com um brilho que pulsa para chamar atenção. O desenho muda conforme o tipo. */
    private void desenharFruta(Graphics2D g, Fruta fruta, int cx, int cy, int celula) {
        double t = System.nanoTime() / 1_000_000_000.0;
        double pulso = 1.0 + 0.07 * Math.sin(t * 6.0);
        double d = celula * 1.0 * pulso;

        g.setColor(new Color(255, 235, 120, 60));
        g.fill(new Ellipse2D.Double(cx - d * 0.75, cy - d * 0.75, d * 1.5, d * 1.5));

        switch (fruta.getTipo()) {
            case MORANGO:
                desenharMorango(g, cx, cy, d);
                break;
            case LARANJA:
                desenharLaranja(g, cx, cy, d);
                break;
            case UVA:
                desenharUva(g, cx, cy, d);
                break;
            default:
                desenharCereja(g, cx, cy, d);
                break;
        }
    }

    private static void circulo(Graphics2D g, double cx, double cy, double raio) {
        g.fill(new Ellipse2D.Double(cx - raio, cy - raio, raio * 2, raio * 2));
    }

    private void desenharCereja(Graphics2D g, int cx, int cy, double d) {
        double r = d * 0.25;
        double ax = cx - d * 0.22, ay = cy + d * 0.22;
        double bx = cx + d * 0.24, by = cy + d * 0.28;
        double topoX = cx + d * 0.06, topoY = cy - d * 0.42;

        g.setColor(new Color(60, 140, 60));
        g.setStroke(new BasicStroke(Math.max(1.5f, (float) d / 14f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Path2D.Double() {{ moveTo(ax, ay - r); quadTo(ax + d * 0.05, cy - d * 0.2, topoX, topoY); }});
        g.draw(new Path2D.Double() {{ moveTo(bx, by - r); quadTo(bx - d * 0.02, cy - d * 0.2, topoX, topoY); }});
        g.fill(new Ellipse2D.Double(topoX, topoY - d * 0.06, d * 0.3, d * 0.14));

        g.setColor(new Color(214, 32, 52));
        circulo(g, ax, ay, r);
        circulo(g, bx, by, r);
        g.setColor(new Color(255, 255, 255, 170));
        circulo(g, ax - r * 0.35, ay - r * 0.35, r * 0.22);
        circulo(g, bx - r * 0.35, by - r * 0.35, r * 0.22);
    }

    private void desenharMorango(Graphics2D g, int cx, int cy, double d) {
        Path2D corpo = new Path2D.Double();
        corpo.moveTo(cx - d * 0.38, cy - d * 0.22);
        corpo.curveTo(cx - d * 0.45, cy + d * 0.15, cx - d * 0.15, cy + d * 0.45, cx, cy + d * 0.48);
        corpo.curveTo(cx + d * 0.15, cy + d * 0.45, cx + d * 0.45, cy + d * 0.15, cx + d * 0.38, cy - d * 0.22);
        corpo.curveTo(cx + d * 0.2, cy - d * 0.34, cx - d * 0.2, cy - d * 0.34, cx - d * 0.38, cy - d * 0.22);
        g.setColor(new Color(229, 40, 60));
        g.fill(corpo);

        g.setColor(new Color(255, 235, 140));
        double[][] sementes = {{-0.18, -0.08}, {0.12, -0.12}, {0.0, 0.08}, {-0.16, 0.2}, {0.16, 0.14}, {0.0, 0.3}};
        for (double[] s : sementes) {
            circulo(g, cx + d * s[0], cy + d * s[1], Math.max(1, d * 0.03));
        }

        g.setColor(new Color(56, 150, 60));
        g.fill(new Ellipse2D.Double(cx - d * 0.34, cy - d * 0.4, d * 0.28, d * 0.16));
        g.fill(new Ellipse2D.Double(cx + d * 0.06, cy - d * 0.4, d * 0.28, d * 0.16));
        g.fill(new Ellipse2D.Double(cx - d * 0.12, cy - d * 0.46, d * 0.24, d * 0.16));
    }

    private void desenharLaranja(Graphics2D g, int cx, int cy, double d) {
        g.setColor(new Color(251, 140, 0));
        circulo(g, cx, cy + d * 0.04, d * 0.4);
        g.setColor(new Color(255, 183, 77, 200));
        circulo(g, cx - d * 0.14, cy - d * 0.1, d * 0.1);
        g.setColor(new Color(56, 150, 60));
        g.fill(new Ellipse2D.Double(cx, cy - d * 0.5, d * 0.3, d * 0.16));
        g.setColor(new Color(110, 70, 30));
        g.fill(new Ellipse2D.Double(cx - d * 0.03, cy - d * 0.44, d * 0.06, d * 0.1));
    }

    private void desenharUva(Graphics2D g, int cx, int cy, double d) {
        g.setColor(new Color(110, 70, 30));
        g.setStroke(new BasicStroke(Math.max(1.5f, (float) d / 14f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Line2D.Double(cx, cy - d * 0.3, cx + d * 0.08, cy - d * 0.48));
        g.setColor(new Color(56, 150, 60));
        g.fill(new Ellipse2D.Double(cx + d * 0.06, cy - d * 0.52, d * 0.3, d * 0.16));

        double r = d * 0.17;
        double[][] bagos = {{-0.2, -0.2}, {0.0, -0.2}, {0.2, -0.2}, {-0.1, 0.0}, {0.1, 0.0}, {0.0, 0.2}};
        for (double[] b : bagos) {
            g.setColor(new Color(123, 31, 162));
            circulo(g, cx + d * b[0], cy + d * b[1], r);
            g.setColor(new Color(255, 255, 255, 120));
            circulo(g, cx + d * b[0] - r * 0.35, cy + d * b[1] - r * 0.35, r * 0.25);
        }
    }

    // --------------------------------------------------------------- personagens

    private void desenharPacman(Graphics2D g, Pacman p, int celula, int offX, int offY) {
        Avatar rosto = avatarDoPacman();
        int cx = offX + (int) Math.round((p.getX() + 0.5) * celula);
        int cy = offY + (int) Math.round((p.getY() + 0.5) * celula);

        if (rosto == null) {
            int lado = (int) Math.round(p.getTamanho() * celula);
            g.setColor(COR_PACMAN);
            g.fillOval(cx - lado / 2, cy - lado / 2, lado, lado);
            return;
        }

        int lado = Math.max(2, (int) Math.round(TAMANHO_AVATAR * celula));
        int px = cx - lado / 2;
        int py = cy - lado / 2;
        if (CONTORNO_PACMAN) {
            int fino = Math.max(2, lado / 14);
            g.setColor(engine.isPoderAtivo() ? new Color(77, 208, 225) : COR_PACMAN);
            g.setStroke(new BasicStroke(fino));
            g.drawOval(px + fino / 2, py + fino / 2, lado - fino, lado - fino);
        }
        g.drawImage(rosto.obter(lado), px, py, null);
    }

    /** Fantasma: só a imagem PNG, sem círculo nem anel. Sem imagem, um quadrado colorido. */
    private void desenharFantasma(Graphics2D g, Fantasma f, int celula, int offX, int offY) {
        Avatar avatar = avatares.get(f.getClass());
        int cx = offX + (int) Math.round((f.getX() + 0.5) * celula);
        int cy = offY + (int) Math.round((f.getY() + 0.5) * celula);

        Graphics2D g2 = (Graphics2D) g.create();
        if (f.isRegenerando()) {
            g2.setComposite(AlphaComposite.SrcOver.derive(0.35f)); // devorado: um "fantasma" esmaecido na casa
        }

        if (avatar == null) {
            int lado = (int) Math.round(f.getTamanho() * celula);
            g2.setColor(new Color(corSemImagem(f)));
            g2.fillRoundRect(cx - lado / 2, cy - lado / 2, lado, lado, lado / 3, lado / 3);
            g2.dispose();
            return;
        }

        int lado = Math.max(2, (int) Math.round(TAMANHO_AVATAR * celula));
        int px = cx - lado / 2;
        int py = cy - lado / 2;

        BufferedImage imagem;
        if (f.isAssustado()) {
            // Sem anel para indicar o estado: assustado = imagem azulada; no fim do poder, pisca em branco.
            boolean piscando = engine.getTempoDePoder() < AVISO_FIM_DO_PODER
                    && ((int) (System.nanoTime() / 220_000_000L) % 2 == 0);
            imagem = avatar.obterTingido(lado, piscando ? COR_PISCA : COR_ASSUSTADO, FORCA_ASSUSTADO);
        } else if (f instanceof FantasmaCiano && ((FantasmaCiano) f).isFugindo()) {
            imagem = avatar.obterTingido(lado, FantasmaCiano.COR_FUGA, FORCA_FUGA);
        } else {
            imagem = avatar.obter(lado);
        }
        g2.drawImage(imagem, px, py, null);
        g2.dispose();
    }

    private static int corSemImagem(Fantasma f) {
        return f.isAssustado() ? COR_ASSUSTADO : f.getCor();
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

    // ------------------------------------------------------------------ telas

    /**
     * Tela de título. Usa a imagem de início (src/image/inicio.jpg); se ela não existir,
     * cai numa versão desenhada em código, com "Profman", os rostos e o botão INICIAR.
     */
    private void desenharTitulo(Graphics2D g) {
        if (imagemInicio != null) {
            desenharTituloComImagem(g);
            return;
        }
        int w = getWidth();
        int h = getHeight();
        g.setColor(COR_FUNDO);
        g.fillRect(0, 0, w, h);
        desenharParedesDeFundo(g, w, h);

        // Título em fonte "pixelada": desenha pequeno, sem suavização, e amplia sem suavizar.
        BufferedImage titulo = textoPixelado("Profman", new Color(255, 214, 0), new Color(140, 100, 0));
        int k = Math.max(1, (int) (w * 0.62) / titulo.getWidth());
        int tw = titulo.getWidth() * k;
        int th = titulo.getHeight() * k;
        Graphics2D px = (Graphics2D) g.create();
        px.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        px.drawImage(titulo, (w - tw) / 2, (int) (h * 0.07), tw, th, null);
        px.dispose();

        // Os rostos.
        int n = rostosDoTitulo.size();
        if (n > 0) {
            int lado = Math.min((int) (h * 0.24), (int) (w * 0.2));
            int espaco = lado / 6;
            int total = n * lado + (n - 1) * espaco;
            int x0 = (w - total) / 2;
            int y = (int) (h * 0.07) + th + (int) (h * 0.05);
            for (int i = 0; i < n; i++) {
                g.drawImage(rostosDoTitulo.get(i).obter(lado), x0 + i * (lado + espaco), y, null);
            }
        }

        // Botão.
        int bw = (int) Math.max(180, w * 0.26);
        int bh = (int) Math.max(48, h * 0.095);
        botaoIniciar.setBounds((w - bw) / 2, (int) (h * 0.74), bw, bh);
        double pulso = 0.5 + 0.5 * Math.sin(System.nanoTime() / 1_000_000_000.0 * 4.0);
        g.setColor(mouseNoBotao ? new Color(255, 236, 120) : COR_PACMAN);
        g.fillRoundRect(botaoIniciar.x, botaoIniciar.y, bw, bh, bh / 2, bh / 2);
        g.setColor(new Color(255, 255, 255, (int) (60 + 120 * pulso)));
        g.setStroke(new BasicStroke(Math.max(2f, bh / 14f)));
        g.drawRoundRect(botaoIniciar.x - 4, botaoIniciar.y - 4, bw + 8, bh + 8, bh / 2 + 6, bh / 2 + 6);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, bh / 2));
        g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();
        String rotulo = "INICIAR";
        g.drawString(rotulo, botaoIniciar.x + (bw - fm.stringWidth(rotulo)) / 2,
                botaoIniciar.y + (bh - fm.getHeight()) / 2 + fm.getAscent());

        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(12, h / 45)));
        centralizar(g, "ENTER ou clique para iniciar     M som     ESC sair", h - Math.max(20, h / 18));
    }

    /** A imagem de início ocupa a janela; o "PLAY" desenhado nela é o botão (clique ou ENTER). */
    private void desenharTituloComImagem(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        Rectangle area = posicionar(imagemInicio);
        g.drawImage(escalada(imagemInicio, area.width, area.height), area.x, area.y, null);

        botaoIniciar.setBounds(
                area.x + (int) (PLAY_NA_IMAGEM[0] * area.width),
                area.y + (int) (PLAY_NA_IMAGEM[1] * area.height),
                (int) (PLAY_NA_IMAGEM[2] * area.width),
                (int) (PLAY_NA_IMAGEM[3] * area.height));

        // Contorno que pulsa sobre o PLAY; fica mais forte quando o mouse está em cima.
        double pulso = 0.5 + 0.5 * Math.sin(System.nanoTime() / 1_000_000_000.0 * 4.0);
        int alfa = mouseNoBotao ? 255 : (int) (70 + 110 * pulso);
        int arco = Math.max(8, botaoIniciar.height / 5);
        g.setStroke(new BasicStroke(Math.max(2f, area.height / 160f)));
        g.setColor(new Color(255, 214, 0, alfa));
        g.drawRoundRect(botaoIniciar.x, botaoIniciar.y, botaoIniciar.width, botaoIniciar.height, arco, arco);
        if (mouseNoBotao) {
            g.setColor(new Color(255, 214, 0, 45));
            g.fillRoundRect(botaoIniciar.x, botaoIniciar.y, botaoIniciar.width, botaoIniciar.height, arco, arco);
        }

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(11, area.height / 45)));
        String dica = "ENTER ou clique em PLAY     M som     ESC sair";
        int y = area.y + area.height - Math.max(10, area.height / 40);
        FontMetrics fm = g.getFontMetrics();
        int x = area.x + (area.width - fm.stringWidth(dica)) / 2;
        g.setColor(Color.BLACK);
        g.drawString(dica, x + 1, y + 1);
        g.setColor(Color.WHITE);
        g.drawString(dica, x, y);
    }

    /** Onde uma imagem fica na janela para caber inteira, centralizada e sem deformar. */
    private Rectangle posicionar(BufferedImage imagem) {
        double escala = Math.min((double) getWidth() / imagem.getWidth(), (double) getHeight() / imagem.getHeight());
        int dw = Math.max(1, (int) Math.round(imagem.getWidth() * escala));
        int dh = Math.max(1, (int) Math.round(imagem.getHeight() * escala));
        return new Rectangle((getWidth() - dw) / 2, (getHeight() - dh) / 2, dw, dh);
    }

    /** A imagem no tamanho pedido, guardada em cache enquanto a janela não mudar de tamanho. */
    private BufferedImage escalada(BufferedImage imagem, int largura, int altura) {
        BufferedImage pronta = escaladas.get(imagem);
        if (pronta == null || pronta.getWidth() != largura || pronta.getHeight() != altura) {
            pronta = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
            Graphics2D gi = pronta.createGraphics();
            gi.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            gi.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            gi.drawImage(imagem, 0, 0, largura, altura, null);
            gi.dispose();
            escaladas.put(imagem, pronta);
        }
        return pronta;
    }

    /** Linhas azuis atrás do título (só na versão sem imagem). */
    private void desenharParedesDeFundo(Graphics2D g, int w, int h) {
        g.setColor(new Color(33, 66, 255, 150));
        g.setStroke(new BasicStroke(Math.max(3f, h / 150f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // Só nas bordas, para não cruzar o título, os rostos, o botão nem os textos.
        double[][] r = {
                {0.02, 0.04, 0.15, 0.10}, {0.83, 0.04, 0.15, 0.10},
                {0.02, 0.30, 0.13, 0.30}, {0.85, 0.30, 0.13, 0.30},
                {0.02, 0.72, 0.20, 0.10}, {0.78, 0.72, 0.20, 0.10},
                {0.02, 0.88, 0.10, 0.08}, {0.88, 0.88, 0.10, 0.08}
        };
        int arc = Math.max(12, h / 30);
        for (double[] q : r) {
            g.drawRoundRect((int) (q[0] * w), (int) (q[1] * h), (int) (q[2] * w), (int) (q[3] * h), arc, arc);
        }
    }

    /** Texto pequeno, sem suavização, com sombra: ao ampliar sem suavizar, parece pixel art. */
    private static BufferedImage textoPixelado(String texto, Color cor, Color sombra) {
        Font fonte = new Font(Font.MONOSPACED, Font.BOLD, 20);
        BufferedImage medida = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gm = medida.createGraphics();
        FontMetrics fm = gm.getFontMetrics(fonte);
        int largura = fm.stringWidth(texto) + 3;
        int altura = fm.getHeight() + 3;
        int linhaBase = fm.getAscent();
        gm.dispose();

        BufferedImage img = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setFont(fonte);
        g.setColor(sombra);
        g.drawString(texto, 2, linhaBase + 2);
        g.setColor(cor);
        g.drawString(texto, 0, linhaBase);
        g.dispose();
        return img;
    }

    /** Tela de escolha: o jogador escolhe quem será o Pacman. */
    private void desenharEscolha(Graphics2D g) {
        g.setColor(COR_FUNDO);
        g.fillRect(0, 0, getWidth(), getHeight());

        int base = Math.max(12, Math.min(getWidth(), getHeight()) / 22);
        g.setColor(COR_PACMAN);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, base * 2));
        centralizar(g, "ESCOLHA SEU PACMAN", base * 3);

        int n = opcoes.size();
        int carta = Math.min(getWidth() / (n + 1), (int) (getHeight() * 0.45));
        int espaco = carta / 6;
        int total = n * carta + (n - 1) * espaco;
        int x0 = (getWidth() - total) / 2;
        int y0 = (getHeight() - carta) / 2 - base / 2; // sobe um pouco para sobrar espaço para o nome

        cartas.clear();
        for (int i = 0; i < n; i++) {
            int x = x0 + i * (carta + espaco);
            cartas.add(new Rectangle(x - espaco / 3, y0 - espaco / 3, carta + 2 * espaco / 3, carta + 2 * espaco / 3));
            boolean ativo = (i == escolhido);
            if (ativo) {
                g.setColor(new Color(255, 214, 0, 40));
                g.fillRoundRect(x - espaco / 3, y0 - espaco / 3, carta + 2 * espaco / 3,
                        carta + 2 * espaco / 3, base, base);
                g.setColor(COR_PACMAN);
                g.setStroke(new BasicStroke(Math.max(3f, base / 6f)));
                g.drawRoundRect(x - espaco / 3, y0 - espaco / 3, carta + 2 * espaco / 3,
                        carta + 2 * espaco / 3, base, base);
            }
            g.drawImage(opcoes.get(i).avatar.obter(carta), x, y0, null);

            g.setFont(new Font(Font.SANS_SERIF, ativo ? Font.BOLD : Font.PLAIN, base));
            g.setColor(ativo ? COR_PACMAN : Color.LIGHT_GRAY);
            FontMetrics fm = g.getFontMetrics();
            String rotulo = (i + 1) + " - " + opcoes.get(i).nome;
            g.drawString(rotulo, x + (carta - fm.stringWidth(rotulo)) / 2, y0 + carta + espaco + base);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(12, base * 2 / 3)));
        centralizar(g, "← → ou A D para trocar     ENTER ou clique para jogar     ESC para sair",
                getHeight() - base * 2);
    }

    /** Tela de vitória: a imagem em pixel art ocupa a janela e uma faixa mostra a pontuação. */
    private void desenharVitoria(Graphics2D g) {
        int w = getWidth();
        int h = getHeight();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);

        Rectangle area = posicionar(imagemVitoria);
        int x = area.x;
        int y = area.y;
        int dw = area.width;
        int dh = area.height;
        g.drawImage(escalada(imagemVitoria, dw, dh), x, y, null);

        // Faixa na base com a pontuação e as instruções.
        int faixa = Math.max(40, (int) (dh * 0.13));
        g.setColor(new Color(0, 0, 0, 175));
        g.fillRect(x, y + dh - faixa, dw, faixa);

        int fonte = Math.max(12, faixa / 3);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fonte));
        g.setColor(COR_VITORIA);
        String pontos = "PONTUAÇÃO FINAL: " + engine.getPontuacao();
        FontMetrics fm = g.getFontMetrics();
        g.drawString(pontos, x + (dw - fm.stringWidth(pontos)) / 2, y + dh - faixa + (int) (faixa * 0.42));

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(11, (int) (fonte * 0.75))));
        g.setColor(Color.WHITE);
        fm = g.getFontMetrics();
        String instrucoes = "R - jogar novamente     ESC - sair";
        g.drawString(instrucoes, x + (dw - fm.stringWidth(instrucoes)) / 2, y + dh - faixa + (int) (faixa * 0.82));
    }

    /** Tela de fim de partida (derrota, ou vitória sem imagem): título, pontuação e instruções. */
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