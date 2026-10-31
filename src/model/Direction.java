package model;

/**
 * Direções possíveis de movimento no labirinto.
 * Substitui as antigas Strings "UP", "DOWN", "LEFT" e "RIGHT":
 * um valor inválido deixa de ser possível e o compilador passa a ajudar.
 */
public enum Direction {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /** Deslocamento em colunas (-1, 0 ou 1). */
    public int getDx() {
        return dx;
    }

    /** Deslocamento em linhas (-1, 0 ou 1). */
    public int getDy() {
        return dy;
    }

    /** Direção contrária (meia-volta). */
    public Direction opposite() {
        switch (this) {
            case UP:
                return DOWN;
            case DOWN:
                return UP;
            case LEFT:
                return RIGHT;
            default:
                return LEFT;
        }
    }
}
