package view;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * Imagem recortada em círculo, escalada para o tamanho pedido. O resultado fica
 * em cache e só é recalculado quando o tamanho muda (ex.: a janela foi redimensionada).
 * <p>
 * A redução é feita em etapas (cada passo reduz pela metade no máximo), porque
 * reduzir uma imagem grande de uma vez só serrilha o desenho.
 */
public class Avatar {

    private final BufferedImage original;
    private int ladoEmCache = -1;
    private BufferedImage cache;

    public Avatar(BufferedImage original) {
        if (original == null) {
            throw new IllegalArgumentException("A imagem não pode ser nula.");
        }
        this.original = original;
    }

    /** Imagem circular com {@code lado} x {@code lado} pixels, com bordas suavizadas. */
    public BufferedImage obter(int lado) {
        if (lado < 1) {
            throw new IllegalArgumentException("O tamanho deve ser positivo.");
        }
        if (cache == null || lado != ladoEmCache) {
            cache = recortarEmCirculo(reduzir(original, lado));
            ladoEmCache = lado;
        }
        return cache;
    }

    private static BufferedImage reduzir(BufferedImage imagem, int lado) {
        BufferedImage atual = imagem;
        while (atual.getWidth() / 2 >= lado) {
            atual = redimensionar(atual, atual.getWidth() / 2);
        }
        return redimensionar(atual, lado);
    }

    private static BufferedImage redimensionar(BufferedImage imagem, int lado) {
        BufferedImage saida = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = saida.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(imagem, 0, 0, lado, lado, null);
        g.dispose();
        return saida;
    }

    /** Mantém só o que está dentro do círculo, com a borda antialiased. */
    private static BufferedImage recortarEmCirculo(BufferedImage imagem) {
        int lado = imagem.getWidth();
        BufferedImage saida = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = saida.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.fill(new Ellipse2D.Double(0, 0, lado, lado)); // máscara
        g.setComposite(AlphaComposite.SrcIn);           // só o que cai dentro da máscara
        g.drawImage(imagem, 0, 0, null);
        g.dispose();
        return saida;
    }
}