# Pacman – Fase 1: fundação e campo fechado

Projeto em Java puro (Swing + AWT), sem bibliotecas externas.

## O que a Fase 1 entrega

- Labirinto em grade definido por matriz de caracteres (`#` parede, `.` corredor, `P` Pacman, `G` fantasma).
- Pacman controlado por **W A S D** ou **setas**; segue na direção até bater em uma parede.
- 4 fantasmas coloridos que andam sozinhos: em cada interseção sorteiam se mudam de direção ou seguem reto.
- Ninguém atravessa paredes nem sai do labirinto (a borda do mapa é toda fechada).
- Pacman encostou em um fantasma = **GAME OVER**. `R` reinicia, `ESC` sai.

## Estrutura

```
src/
├── App.java
├── model/        regras e entidades (sem dependência de tela)
│   ├── Direction, Posicao, EstadoJogo
│   ├── Labirinto                      mapa em grade, validado na construção
│   ├── Entidade (abstract)            posição e colisão
│   ├── Personagem (abstract)          movimento em grade (template method)
│   ├── Pacman                         direção vem do teclado
│   └── Fantasma (abstract) → Vermelho, Rosa, Ciano, Laranja
├── controller/
│   └── GameEngine                     loop, regras, colisão e estado
└── view/
    ├── GameWindow                     janela
    └── GamePanel                      só desenha e repassa o teclado
test/
└── Fase1Verificacao.java              verificações sem janela
```

## Como compilar e executar

```
javac -encoding UTF-8 -d bin -sourcepath src src/App.java
java -cp bin App
```

## Como rodar as verificações

```
javac -encoding UTF-8 -d out -sourcepath src test/Fase1Verificacao.java
java -cp out Fase1Verificacao
```

## Observação sobre o visual

Pacman e fantasmas são desenhados com formas geométricas simples (marcadores de posição).
Para usar sprites próprios, altere `desenharPacman` e `desenharFantasma` em `view/GamePanel.java`.
