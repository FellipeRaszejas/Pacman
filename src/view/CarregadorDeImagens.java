package view;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Carrega imagens do classpath ou do disco. Aceita caminhos como
 * {@code "image/fantasma1.png"}, {@code "./image/x.png"} ou {@code "src/image/x.png"}.
 */
public final class CarregadorDeImagens {

    private CarregadorDeImagens() {
    }

    /** Devolve a imagem, ou {@code null} se não existir ou não puder ser lida (o jogo segue sem ela). */
    public static BufferedImage carregar(String caminho) {
        if (caminho == null || caminho.isEmpty()) {
            return null;
        }
        String limpo = caminho.replaceFirst("^\\./", "").replaceFirst("^src/", "");

        try {
            // 1. Classpath (ideal quando os recursos são copiados junto das classes)
            URL url = CarregadorDeImagens.class.getResource("/" + limpo);
            if (url != null) {
                return ImageIO.read(url);
            }
            // 2. Disco, a partir da pasta do projeto
            for (String tentativa : new String[] {"src/" + limpo, limpo}) {
                File arquivo = new File(tentativa);
                if (arquivo.isFile()) {
                    return ImageIO.read(arquivo);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao carregar a imagem " + caminho + ": " + e.getMessage());
            return null;
        }
        System.err.println("Imagem não encontrada: " + caminho + " (será usado o desenho simples)");
        return null;
    }
}