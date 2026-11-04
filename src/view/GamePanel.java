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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Só desenha o estado do {@link GameEngine} e repassa o teclado a ele.
 * Nenhuma regra do jogo vive aqui. O labirinto é escalado e centralizado para
 * caber na janela, com uma faixa no topo para o HUD (pontos, vidas e itens restantes).
 * <p>
 * Os fantasmas e o Pacman são desenhados só com as imagens PNG (fundo transparente).
 * Antes da partida há uma tela para escolher qual personagem será o Pacman. Se uma
 * imagem não for encontrada, o personagem cai num desenho simples.
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

    /** Tamanho das imagens dos personagens, como fração da célula. */
    private static final double TAMANHO_AVATAR = 1.1;
    /** Contorno amarelo no Pacman, para ele não se confundir com os fantasmas. */
    private static final boolean CONTORNO_PACMAN = true;
    /** Intensidade da camada azulada sobre o fantasma quando ele foge. */
    private static final float FORCA_FUGA = 0.55f;

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
    private final List<Opcao> opcoes = new ArrayList<>();
    private int escolhido;
    private boolean escolhendo;
    private Runnable aoAlternarSom;
    private Runnable aoIniciarPartida;

    public GamePanel(GameEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("A engine não pode ser nula.");
        }
        this.engine = engine;
        setBackground(COR_FUNDO);
        setFocusable(true);
        carregarAvatares();
        carregarOpcoesDePacman();
        escolhendo = !opcoes.isEmpty();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int tecla = e.getKeyCode();
                if (tecla == KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                } else if (escolhendo) {
                    teclaNaEscolha(tecla);
                } else if (tecla == KeyEvent.VK_R) {
                    GamePanel.this.engine.reiniciar();
                } else if (tecla == KeyEvent.VK_M) {
                    if (aoAlternarSom != null) {
                        aoAlternarSom.run();
                    }
                } else {
                    Direction d = TECLAS.get(tecla);
                    if (d != null) {
                        GamePanel.this.engine.direcionarPacman(d);
                    }
                }
            }
        });
    }

    /** O jogador ainda está na tela de escolha do Pacman? */
    public boolean isEscolhendo() {
        return escolhendo;
    }

    /** Ação executada quando a escolha termina (normalmente, iniciar o loop da engine). */
    public void setAoIniciarPartida(Runnable aoIniciarPartida) {
        this.aoIniciarPartida = aoIniciarPartida;
    }

    /** Ação executada ao apertar M (ligar/desligar o som). */
    public void setAoAlternarSom(Runnable aoAlternarSom) {
        this.aoAlternarSom = aoAlternarSom;
    }

    /** Uma imagem para cada fantasma. Para trocar quem é quem, mude as linhas abaixo. */
    private void carregarAvatares() {
        registrar(FantasmaVermelho.class, criarAvatar("image/fantasma1.png"));
        registrar(FantasmaRosa.class, criarAvatar("image/fantasma2.png"));
        registrar(FantasmaCiano.class, criarAvatar("image/deiv.png"));
        registrar(FantasmaLaranja.class, criarAvatar("image/glauco.png"));
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
        } else if (tecla == KeyEvent.VK_M && aoAlternarSom != null) {
            aoAlternarSom.run();
        }
        repaint();
    }

    private void confirmarEscolha() {
        escolhendo = false;
        if (aoIniciarPartida != null) {
            aoIniciarPartida.run();
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (escolhendo) {
            Graphics2D tela = (Graphics2D) g.create();
            tela.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            desenharEscolha(tela);
            tela.dispose();
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
            g.setColor(COR_PACMAN);
            g.setStroke(new BasicStroke(fino));
            g.drawOval(px + fino / 2, py + fino / 2, lado - fino, lado - fino);
        }
        g.drawImage(rosto.obter(lado), px, py, null);
    }

    /** Fantasma: só a imagem PNG, sem círculo nem anel. Sem imagem, um quadrado colorido. */
    private void desenharFantasma(Graphics2D g, Fantasma f, int celula, int offX, int offY) {
        Avatar avatar = avatares.get(f.getClass());

        if (avatar == null) {
            Color cor = new Color(f.getCor());
            int lado = (int) Math.round(f.getTamanho() * celula);
            int px = offX + (int) Math.round((f.getX() + 0.5) * celula) - lado / 2;
            int py = offY + (int) Math.round((f.getY() + 0.5) * celula) - lado / 2;
            g.setColor(cor);
            g.fillRoundRect(px, py, lado, lado, lado / 3, lado / 3);
            return;
        }

        int lado = Math.max(2, (int) Math.round(TAMANHO_AVATAR * celula));
        int px = offX + (int) Math.round((f.getX() + 0.5) * celula) - lado / 2;
        int py = offY + (int) Math.round((f.getY() + 0.5) * celula) - lado / 2;

        // Sem anel para indicar o estado: quando o Ciano foge, a imagem fica azulada.
        boolean fugindo = (f instanceof FantasmaCiano) && ((FantasmaCiano) f).isFugindo();
        g.drawImage(fugindo ? avatar.obterTingido(lado, FantasmaCiano.COR_FUGA, FORCA_FUGA)
                            : avatar.obter(lado), px, py, null);
    }

    /** Tela inicial: o jogador escolhe quem será o Pacman. */
    private void desenharEscolha(Graphics2D g) {
        g.setColor(COR_FUNDO);
        g.fillRect(0, 0, getWidth(), getHeight());

        int base = Math.max(12, Math.min(getWidth(), getHeight()) / 22);
        g.setColor(COR_PACMAN);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, base * 2));
        centralizar(g, "ESCOLHA SEU PACMAN", base * 3);

        int n = opcoes.size();
        int carta = Math.min(getWidth() / (n + 1), (int) (getHeight() * 0.5));
        int espaco = carta / 4;
        int total = n * carta + (n - 1) * espaco;
        int x0 = (getWidth() - total) / 2;
        int y0 = (getHeight() - carta) / 2;

        for (int i = 0; i < n; i++) {
            int x = x0 + i * (carta + espaco);
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
            g.drawString(rotulo, x + (carta - fm.stringWidth(rotulo)) / 2, y0 + carta + espaco + base / 2);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(12, base * 2 / 3)));
        centralizar(g, "← → ou A D para trocar     ENTER para jogar     ESC para sair",
                getHeight() - base * 2);
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

    /** Tela de fim de partida (derrota ou vitória): título, pontuação final e instruções. */
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