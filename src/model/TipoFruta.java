package model;

/** Os tipos de fruta do jogo. Só mudam a aparência: todas dão o mesmo poder. */
public enum TipoFruta {
    CEREJA,
    MORANGO,
    LARANJA,
    UVA;

    /** O tipo da fruta de número {@code indice} (repete a sequência se houver mais de quatro). */
    public static TipoFruta doIndice(int indice) {
        TipoFruta[] todos = values();
        return todos[Math.floorMod(indice, todos.length)];
    }
}