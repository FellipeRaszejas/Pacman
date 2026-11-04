package view;

import audio.SomDoJogo;
import controller.GameEngine;
import model.Labirinto;

import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.Toolkit;

/** Janela em tela cheia, sem bordas, que hospeda o jogo. ESC fecha. */
public class GameWindow extends JFrame {

    private final GameEngine engine;
    private final GamePanel painel;

    public GameWindow() {
        super("Pacman");

        this.engine = new GameEngine(Labirinto.padrao());
        this.painel = new GamePanel(engine);

        SomDoJogo som = new SomDoJogo();
        engine.setEventos(som);
        painel.setAoAlternarSom(som::alternarMudo);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);

        Dimension tela = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(0, 0, tela.width, tela.height);
        setContentPane(painel);
    }

    public void start() {
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
        painel.requestFocusInWindow();
        engine.iniciar(painel::repaint);
    }
}