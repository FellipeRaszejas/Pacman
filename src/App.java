import javax.swing.SwingUtilities;

import view.GameWindow;

public class App {
    public static void main(String[] args) {
        // Cria a interface na thread de eventos do Swing
        SwingUtilities.invokeLater(() -> new GameWindow().start());
    }
}
