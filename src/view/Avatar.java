package view;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Imagem com fundo transparente (PNG), escalada para o tamanho pedido. Nada é
 * desenhado em volta: o que aparece é só o que está opaco no PNG.
 * <p>
 * O resultado fica em cache e só é recalculado quando o tamanho (ou a cor do
 * tingimento) muda. A redução é feita em etapas, porque reduzir uma imagem
 * grande de uma vez só serrilha o desenho.
 */
public class Avatar {

    private final BufferedImage original;

    private int ladoEmCache = -1;
    private BufferedImage cache;

    private int ladoTingido = -1;
    private int corTingida = -1;
    private BufferedImage cacheTingido;

    public Avatar(BufferedImage original) {
        if (original == null) {
            throw new IllegalArgumentException("A imagem não pode ser nula.");
        }
        this.original = original;
    }

    /** Imagem {@code lado} x {@code lado} pixels, com a transparência preservada. */
    public BufferedImage obter(int lado) {
        if (lado < 1) {
            throw new IllegalArgumentException("O tamanho deve ser positivo.");
        }
        if (cache == null || lado != ladoEmCache) {
            cache = reduzir(original, lado);
            ladoEmCache = lado;
        }
        return cache;
    }

    /**
     * A mesma imagem com uma camada de cor por cima, só nos pixels opacos (o
     * fundo transparente continua transparente). Serve para mostrar um estado,
     * como o fantasma fugindo.
     */
    public BufferedImage obterTingido(int lado, int rgb, float forca) {
        int chave = rgb ^ Float.floatToIntBits(forca);
        if (cacheTingido == null || lado != ladoTingido || chave != corTingida) {
            BufferedImage base = obter(lado);
            BufferedImage saida = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = saida.createGraphics();
            g.drawImage(base, 0, 0, null);
            g.setComposite(AlphaComposite.SrcAtop.derive(forca)); // só sobre o que já é opaco
            g.setColor(new Color(rgb));
            g.fillRect(0, 0, lado, lado);
            g.dispose();
            cacheTingido = saida;
            ladoTingido = lado;
            corTingida = chave;
        }
        return cacheTingido;
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
}