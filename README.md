# Pac-Man em Java — um projeto para aprender Programação Orientada a Objetos

Este projeto é um jogo de Pac-Man feito em **Java puro** (Swing + AWT, sem bibliotecas externas).

> **O objetivo não é só o jogo: é aprender POO.** O Pac-Man foi escolhido porque tem
> personagens, regras e comportamentos diferentes que se encaixam bem nos quatro
> pilares: **abstração, encapsulamento, herança e polimorfismo**. Este README mostra
> onde cada pilar aparece no código.

---

## O jogo

- Escolha seu Pacman na tela inicial: **Fellipe** ou **Ana Júlia**.
- Coma todas as pílulas (10 pontos) e frutas (100 pontos) do labirinto para vencer.
- Quatro fantasmas (**Deiv**, **Glauco** e mais dois) perseguem você, cada um com um comportamento diferente.
- Você tem **3 vidas**. Tocar em um fantasma custa uma vida; sem vidas, é *game over*.

| Tecla | Ação |
|---|---|
| `W A S D` ou setas | mover o Pacman |
| `← →` / `A D`, `ENTER` (ou `1` / `2`) | escolher o personagem na tela inicial |
| `R` | jogar novamente (após vitória ou game over) |
| `M` | ligar/desligar o som |
| `ESC` | sair |

### Os fantasmas

| Fantasma | Comportamento | Classe |
|---|---|---|
| Vermelho | persegue o Pacman pelo caminho mais curto | `FantasmaVermelho` |
| Rosa | tenta emboscar, mirando 4 células à frente do Pacman | `FantasmaRosa` |
| Ciano | alterna: persegue por 8 s e foge por 5 s | `FantasmaCiano` |
| Laranja | anda de forma aleatória | `FantasmaLaranja` |

---

## Como rodar

Na pasta raiz do projeto (a que contém `src/`):

```bash
# compilar
javac -encoding UTF-8 -d bin $(find src -name '*.java')

# jogar
java -cp bin App
```

As imagens (`fantasma1.png`, `fantasma2.png`, `deiv.png`, `glauco.png`, `fellipe.png`,
`anajulia.png`) ficam em `src/image/` (ou soltas em `src/`). Se alguma faltar, o jogo
continua funcionando com um desenho simples no lugar. Os sons são sintetizados pelo
próprio código, então não há arquivos de áudio.

Os testes não usam JUnit; são classes com `main` que imprimem as verificações:

```bash
javac -encoding UTF-8 -d bin $(find src test -name '*.java')
java -cp bin model.Verificacao
java -cp bin audio.VerificacaoSom
```

---

## Estrutura

```
src/
├── App.java
├── model/          regras e entidades (não conhecem tela nem som)
│   ├── Direction, Posicao, EstadoJogo
│   ├── Labirinto                          mapa em grade, validado na construção
│   ├── Entidade (abstract)                posição, tamanho e colisão
│   │   ├── Personagem (abstract)          movimento em grade
│   │   │   ├── Pacman                     direção vem do teclado
│   │   │   └── Fantasma (abstract)        escolha de caminho nas interseções
│   │   │       └── Vermelho, Rosa, Ciano, Laranja
│   │   └── Item (abstract)                algo que o Pacman coleta
│   │       └── Pilula, Fruta
├── controller/
│   ├── GameEngine                         loop, colisões, pontos, vidas, estados
│   └── EventosDoJogo (interface)          avisos: coletou item, perdeu vida...
├── audio/
│   ├── SomDoJogo (implements EventosDoJogo)
│   └── Sintetizador, Efeitos              sons gerados em código
└── view/
    ├── GameWindow, GamePanel              só desenham e repassam o teclado
    └── Avatar, CarregadorDeImagens        imagens PNG
```

A separação **model / controller / view** é intencional: o `model` não importa
nada de `javax.swing`, então toda a lógica do jogo pode ser testada sem abrir janela.

---

## Os 4 pilares da POO, com exemplos do projeto

### 1. Abstração

**Ideia:** representar só o que importa, escondendo os detalhes e definindo *o que*
algo faz sem dizer *como*.

**Onde aparece:**

- **Classes abstratas** que modelam conceitos, não coisas concretas. Não existe uma
  "entidade genérica" nem um "fantasma genérico" no jogo, então elas não podem ser
  instanciadas: `Entidade`, `Personagem`, `Fantasma` e `Item`.
- **Métodos abstratos** que declaram o contrato e deixam o "como" para as subclasses:

```java
// model/Fantasma.java — todo fantasma TEM que saber decidir, mas cada um decide a seu modo
protected abstract Direction decidirDirecao(Labirinto labirinto, List<Direction> opcoes);

// model/Item.java — todo item vale pontos, mas quantos depende do item
public abstract int getPontos();
```

- **Interface** `EventosDoJogo` (`controller/`): a `GameEngine` só sabe que "alguém será
  avisado" quando o jogador coleta um item ou perde uma vida. Ela não sabe que existe
  som. Quem quiser reagir implementa a interface (o `SomDoJogo` faz isso).
- **A `GameEngine` esconde a complexidade** do jogo atrás de poucos métodos
  (`atualizar`, `direcionarPacman`, `reiniciar`). O `GamePanel` apenas lê o estado.

### 2. Encapsulamento

**Ideia:** proteger o estado interno do objeto. O mundo de fora só mexe nos dados por
meio de métodos que garantem que nada fique inválido.

**Onde aparece:**

- **Atributos privados** com acesso por getters. A posição de uma `Entidade` não é
  pública; só pode ser lida por `getX()`/`getY()` e alterada por dentro da hierarquia:

```java
// model/Entidade.java
private double x;
private double y;

public double getX() { return x; }

protected void setPosicao(double x, double y) {   // só as subclasses podem mover
    if (Double.isNaN(x) || Double.isNaN(y) || x < 0 || y < 0) {
        throw new IllegalArgumentException("Posição inválida: (" + x + ", " + y + ").");
    }
    this.x = x;
    this.y = y;
}
```

- **Validação nos construtores e setters**: não dá para criar um personagem com
  velocidade negativa, um fantasma sem alvo ou uma entidade com tamanho fora de
  `(0, 1]`; o objeto nasce sempre em um estado válido.
- **Listas protegidas**: a `GameEngine` devolve `Collections.unmodifiableList(...)`
  para fantasmas e itens. A `GamePanel` consegue ler, mas não consegue remover um item.
- **Modificadores com intenção**: `protected` para o que só as subclasses usam
  (`escolherDirecao`, `setPosicao`), `final` para o que não pode ser sobrescrito
  (`atualizar` em `Personagem`) e `private` para os detalhes internos
  (`progresso`, `coluna`, `linha` do movimento em grade).
- O **`Labirinto`** valida o próprio mapa ao ser construído (borda fechada, tudo
  conectado) e depois fica imutável.

### 3. Herança

**Ideia:** reaproveitar código criando classes mais específicas a partir de outras
mais gerais ("é um").

**Onde aparece** (duas árvores):

```
Entidade
├── Personagem
│   ├── Pacman
│   └── Fantasma
│       ├── FantasmaVermelho
│       ├── FantasmaRosa
│       ├── FantasmaCiano
│       └── FantasmaLaranja
└── Item
    ├── Pilula
    └── Fruta
```

- `Entidade` guarda posição, tamanho e `colideCom(...)`. **Tudo** que existe no
  labirinto herda isso, de graça: um Pacman, um fantasma e uma pílula colidem pela
  mesma regra.
- `Personagem` acrescenta o movimento em grade (velocidade, direção, passos entre
  células). `Pacman` e `Fantasma` reaproveitam todo esse código.
- `Fantasma` acrescenta o que é comum a todos (cor, atraso de saída, alvo, como
  listar as opções em uma interseção). Cada subclasse só escreve o que é diferente:

```java
// model/FantasmaVermelho.java — a classe inteira é quase só a decisão
public class FantasmaVermelho extends Fantasma {
    public FantasmaVermelho(int coluna, int linha, Pacman alvo, Random random) {
        super(coluna, linha, VELOCIDADE, COR, ATRASO_DE_SAIDA, alvo, random);  // reaproveita o construtor
    }

    @Override
    protected Direction decidirDirecao(Labirinto labirinto, List<Direction> opcoes) {
        int[][] ate = labirinto.calcularDistancias(colunaDoAlvo(), linhaDoAlvo());
        return aproximar(ate, opcoes);
    }
}
```

- **Template Method** (herança bem usada): `Personagem.atualizar(...)` é `final` e
  define o *esqueleto* do movimento; dentro dele chama `escolherDirecao(...)`, que é
  abstrato. O Pacman responde com o teclado, o fantasma com sua estratégia. O
  esqueleto nunca é duplicado.

### 4. Polimorfismo

**Ideia:** tratar objetos diferentes pela mesma interface, e cada um responde do seu
jeito.

**Onde aparece:**

- **A `GameEngine` trata todos os fantasmas igualmente**, sem saber qual é qual:

```java
// controller/GameEngine.java
for (Fantasma f : fantasmas) {
    f.atualizar(labirinto, passo);   // cada fantasma persegue, embosca, foge ou vaga
}
```

  O mesmo `atualizar` produz quatro comportamentos diferentes, porque cada subclasse
  sobrescreve `decidirDirecao`.

- **Pontuação sem `if`/`instanceof`**: a engine soma pontos de qualquer item sem saber
  se é pílula ou fruta.

```java
// controller/GameEngine.java
pontuacao += item.getPontos();   // 10 se for Pilula, 100 se for Fruta
```

- **Sobrescrita de comportamento**: o `FantasmaCiano` sobrescreve `getCor()` e muda
  conforme o estado.

```java
// model/FantasmaCiano.java
@Override
public int getCor() {
    return fugindo ? COR_FUGA : super.getCor();
}

@Override
protected Direction decidirDirecao(Labirinto labirinto, List<Direction> opcoes) {
    int[][] ate = labirinto.calcularDistancias(colunaDoAlvo(), linhaDoAlvo());
    return fugindo ? afastar(ate, opcoes) : aproximar(ate, opcoes);
}
```

- **Polimorfismo por interface**: o `SomDoJogo` implementa `EventosDoJogo`. A engine
  chama `eventos.aoColetarItem(item)` sem saber se quem escuta toca um som, grava um
  log ou não faz nada (o padrão é uma implementação vazia).
- **Polimorfismo de `Entidade`**: `colideCom(Entidade outra)` aceita qualquer
  entidade; `pacman.colideCom(fantasma)` e `pacman.colideCom(item)` usam o mesmo método.

---

## Extra: padrões de projeto que apareceram

| Padrão | Onde |
|---|---|
| **Template Method** | `Personagem.atualizar` (esqueleto) e `Fantasma.escolherDirecao` (o que varia é `decidirDirecao`) |
| **Observer** | `EventosDoJogo` (engine) ← `SomDoJogo` |
| **MVC** | `model` (regras) / `view` (desenho) / `controller` (`GameEngine`) |

## Ideias para continuar estudando

- Criar um **power-up** (pílula especial que deixa os fantasmas vulneráveis) como uma nova subclasse de `Item`.
- Criar um quinto fantasma só herdando de `Fantasma` e implementando `decidirDirecao`.
- Trocar o som por um `EventosDoJogo` que grava logs, sem mexer na engine.