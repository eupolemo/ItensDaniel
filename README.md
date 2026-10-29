# ItensDaniel

Plugin Paper de itens customizados com poderes especiais: varinha de raio, cajado de teleporte,
bola de fogo, botas de voo, espada vampira, espada e picareta do infinito. Tudo declarado no `config.yml` — item novo não
precisa de código.

- Paper API `1.21.4-R0.1-SNAPSHOT`
- Java 21
- Maven

## Build

```
mvn clean package
```

Jar em `target/ItensDaniel-1.1.1.jar`.

## Instalar no servidor

Copie o jar para `plugins/` e reinicie o servidor. Ou use o script:

```
./deploy.sh /caminho/do/servidor/plugins
./deploy.sh usuario@servidor:/caminho/do/servidor/plugins
```

Grave o destino em `.deploy.env` (`DEST=...`) para rodar só `./deploy.sh`.

Na primeira execução o plugin cria `plugins/ItensDaniel/config.yml`.

Quem já rodava a 1.0.0: o formato do config subiu para a versão 2. O plugin arquiva o antigo como
`config-v1.yml.bak` e escreve o novo — customizações precisam ser copiadas de volta à mão.

## Comandos

| Comando | Permissão | Efeito |
|---|---|---|
| `/itens` | `itens.use` (todos) | Lista os itens configurados |
| `/itens menu` | `itens.use` para ver, `itens.dar` para pegar | Abre a vitrine com todos os itens |
| `/itens info <id>` | `itens.use` | Detalhe de um item e seus poderes |
| `/itens dar <id> [jogador]` | `itens.dar` (op) | Entrega o item (a si mesmo, se omitir o jogador) |
| `/itens reload` | `itens.reload` (op) | Recarrega o `config.yml` |

Aliases: `/itensdaniel`, `/itd`.

`itens.use` também é o que libera os poderes: quem não tem a permissão carrega o item, mas ele
não faz nada.

## Itens que já vêm configurados

| id | Item | O que faz |
|---|---|---|
| `varinha-tempestade` | Varinha da Tempestade | Botão direito: raio no bloco mirado (40 blocos), 8s |
| `cajado-do-vazio` | Cajado do Vazio | Botão direito: teleporte para o bloco mirado (30 blocos), 5s |
| `chama-de-daniel` | Chama de Daniel | Botão direito: bola de fogo que não destrói terreno, 4s |
| `coracao-de-daniel` | Coração de Daniel | Botão direito: cura 3 corações, 45s |
| `botas-do-vento` | Botas do Vento | Vestidas: voo + Speed II + Jump Boost III |
| `elmo-do-corujao` | Elmo do Corujão | Vestido: visão noturna + respiração aquática + ímã de drops |
| `espada-vampira` | Espada Vampira | Ao acertar: rouba 35% do dano em vida + lentidão II |
| `peitoral-espinhoso` | Peitoral Espinhoso | Ao ser acertado: arremessa o atacante; resistência I |
| `espada-do-infinito` | Espada do Infinito | Espada+machado+picareta, quebra bedrock, rouba 50% em vida, impulso, voo, buffs da maçã encantada, 30 corações |
| `picareta-do-infinito` | Picareta do Infinito | Mina 3x3 com auto-fundição, ilumina, ímã de drops, voo, buffs da maçã encantada, 30 corações |

## Criar um item novo

Só editar `plugins/ItensDaniel/config.yml` e rodar `/itens reload`. Exemplo:

```yaml
itens:
  martelo-do-daniel:
    nome: "Martelo do Daniel"
    cor: "#FFC107"
    material: NETHERITE_AXE
    descricao:
      - "Bate forte e rouba vida."
    brilho: true
    inquebravel: true
    cooldown-segundos: 6
    # modelo: itensdaniel:martelo-do-daniel   # ver "Texturas customizadas"
    poderes:
      - tipo: RAIO
        alcance: 25
      - tipo: ROUBO_DE_VIDA
        fator: 0.5
```

Um item pode ter quantos poderes quiser, de gatilhos diferentes.

### Poderes disponíveis

| Tipo | Gatilho | Chaves |
|---|---|---|
| `RAIO` | botão direito | `alcance` |
| `TELEPORTE` | botão direito | `alcance` |
| `BOLA_DE_FOGO` | botão direito | `potencia`, `velocidade`, `incendiario`, `quebra-blocos` |
| `CURA` | botão direito | `fator` (pontos de vida; 2 = 1 coração) |
| `IMPULSO` | botão direito | `fator` (força do arremesso; já vem com queda lenta) |
| `QUEBRA_INDESTRUTIVEL` | botão **esquerdo** | — (quebra bedrock e cia.) |
| `AREA` | ao minerar | `alcance` como raio (1 = 3x3, 2 = 5x5) |
| `FUNDIR` | ao minerar | — (o drop já sai fundido) |
| `EFEITO` | passivo | `efeito` (nome do vanilla: `speed`, `night_vision`, …), `nivel` |
| `VOO` | passivo | — |
| `LUZ` | passivo | — (acende onde o jogador está) |
| `IMA` | passivo | `alcance` (raio que puxa os itens caídos) |
| `ROUBO_DE_VIDA` | ao acertar | `fator` (fração do dano virada em vida) |
| `CONGELAR` | ao acertar | `nivel`, `duracao-ticks` |
| `EMPURRAO` | ao ser acertado | `fator` (força) |

Passivo vale com o item na mão principal, na secundária ou vestido. Um tipo desconhecido, um
material inexistente ou um efeito de poção errado não derruba o plugin: a entrada é ignorada com
aviso no console.

### Multiferramenta e atributos

Duas chaves de item que não são poderes, porque quem aplica é o próprio jogo:

```yaml
    multiferramenta: true      # mina qualquer bloco rápido e com o drop certo
    atributos:
      - atributo: max_health
        valor: 2.0
        operacao: ADD_SCALAR   # soma 2x a vida base: 10 corações viram 30
        slot: hand             # any, hand, mainhand, offhand, head, chest, legs, feet, armor
```

`multiferramenta` usa as tags `mineable/*` do próprio jogo, então blocos de versões futuras já
entram junto — e uma espada passa a minerar pedra com drop normal.

**Cuidado com `atributos`:** declarar qualquer atributo substitui os do material. Uma espada de
netherita que ganhe só `max_health` perde o dano dela; por isso a Espada do Infinito repete
`attack_damage` e `attack_speed` no `config.yml`. Operações: `ADD_NUMBER` soma direto,
`ADD_SCALAR` soma uma fração do valor base, `MULTIPLY_SCALAR_1` multiplica o total.

## Pegar os itens: menu, não aba do criativo

Não é possível pôr estes itens numa aba do menu do criativo. As abas são montadas pelo cliente a
partir dos itens registrados no jogo, e os nossos são itens vanilla com componentes — não itens
novos. Nem plugin nem resource pack registram entrada lá.

O substituto é `/itens menu`: uma vitrine com todos os itens configurados. Clicar copia o item
para o inventário; nada sai da vitrine, nem por shift-clique ou troca com a hotbar. Ela é montada
na hora, então já reflete o último `/itens reload`. Funciona igual no Bedrock via Geyser, porque
é um baú comum.

Quem tem `itens.use` mas não `itens.dar` consegue abrir e olhar, mas não pegar.

Duas alternativas úteis no criativo:

- **Clique do meio** num item especial que já exista copia ele com poderes e tudo — componentes
  vão junto.
- `/itens dar <id> [jogador]` para entregar direto, inclusive do console.

## Texturas customizadas

Dá, e o esqueleto do pack já está em [`resourcepack/`](resourcepack/) — um ícone por item. As
texturas atuais são **placeholders** (formas chapadas na cor do item); troque os PNGs por arte de
verdade quando quiser, sem mexer em mais nada.

São duas metades:

1. **Servidor** — a chave `modelo:` no `config.yml` grava o componente `item_model` no item:

   ```yaml
   modelo: itensdaniel:espada-vampira
   ```

   Já existe uma linha dessas comentada em cada item do `config.yml`. Descomente **só depois** que
   o pack estiver chegando aos jogadores; sem o pack, o item vira o quadriculado roxo e preto de
   textura faltando. Para packs no formato antigo (anteriores a 1.21.4) existe `custom-model-data`.

2. **Cliente** — o pack precisa chegar no jogador. Empacote:

   ```bash
   ./empacotar.sh
   ```

   Sai `target/ItensDaniel-resourcepack.zip` e o SHA-1. Suba o zip em qualquer URL pública e
   aponte no `server.properties`:

   ```
   resource-pack=https://.../ItensDaniel-resourcepack.zip
   resource-pack-sha1=<o sha1 impresso pelo script>
   require-resource-pack=false
   ```

   O SHA-1 não é enfeite: sem ele o cliente reusa o pack em cache e não vê as texturas novas.
   Para testar sozinho, dá para jogar o zip direto em `.minecraft/resourcepacks/` e ativar nas
   opções.

### Estrutura do pack

```
resourcepack/
  pack.mcmeta                                   formato 46 (1.21.4)
  assets/itensdaniel/items/<id>.json            liga a chave 'modelo:' a um modelo
  assets/itensdaniel/models/item/<id>.json      sprite plano (item/generated)
  assets/itensdaniel/textures/item/<id>.png     16x16 RGBA
```

Item novo com textura própria: copie os três arquivos com o novo id e aponte `modelo:` para ele.

**Limite conhecido:** isto troca o **ícone** do item (inventário, mão, chão). Armadura *vestida*
continua com a aparência vanilla do material — botas de netherita seguem parecendo netherita no
corpo. Mudar isso exige o componente `equippable` com uma textura em
`assets/itensdaniel/textures/entity/equipment/humanoid/`, que o plugin ainda não expõe.

## Detalhes que importam

- **O item é reconhecido pela tag interna**, não pelo nome. Renomear na bigorna não quebra o
  poder; e um item comum renomeado igual **não** ganha poder nenhum.
- **Bola de fogo não faz grief** com `quebra-blocos: false` (padrão): a explosão acontece, o
  terreno fica. Ligue por sua conta e risco.
- **Voo**: o plugin só tira o voo de quem ele mesmo deu, e nunca mexe em criativo/espectador.
  Ao desligar o plugin, o voo concedido é revogado.
- **Cooldown** é por jogador e por item, só em memória — reiniciar o servidor zera. Só começa a
  contar quando o poder realmente dispara (mirar no vazio com o cajado não gasta o cooldown).
- **Efeitos passivos** são reaplicados a cada segundo com duração de 3s, então somem sozinhos
  pouco depois de guardar o item.
- **Quebrar bedrock** vale para blocos que o jogo trata como indestrutíveis, com uma exceção
  proposital: blocos de comando, spawner, barrier, structure block e portais ficam de fora — são
  ferramenta de mapa, e quebrar por acidente estraga o mundo. Quebrar o chão de bedrock do mundo
  abre caminho para o vazio; é o preço do poder.
- **Mineração 3x3 e bedrock disparam `BlockBreakEvent` de verdade** antes de mexer no bloco, então
  plugins de proteção conseguem barrar bloco a bloco. A área sai no plano da face que você mira.
- **Auto-fundição** cobre os casos usuais (minério bruto → lingote, areia → vidro, pedregulho →
  pedra); o resto cai normal. Quando a fornalha não mudaria nada, quem quebra o bloco é o próprio
  jogo — assim a experiência do minério continua caindo.
- **No criativo a picareta não dropa nada**, igual ao vanilla: os blocos da área somem sem item.
- **Os blocos da área somem com a física ligada**, então a água volta a correr para dentro do
  buraco, areia e cascalho caem, e grama/flor em cima do bloco quebrado soltam junto.
- **`LUZ`** põe um bloco de luz invisível onde o jogador está e o move junto, 4x por segundo. Ele é
  removido ao guardar o item, ao sair e no desligamento do plugin.
- **Triplicar corações** é um modificador `max_health` no item: o jogo aplica e remove sozinho. Ao
  largar o item a vida volta ao teto normal — se estava acima, o excesso some.

## Jogadores de Bedrock

Os poderes são todos server-side e os materiais usados existem nas duas edições, então celular,
tablet e console jogam normalmente via [Geyser](https://geysermc.org) + Floodgate.

As **texturas**, não. O pack de `resourcepack/` é formato Java, e o Geyser não converte pack Java
para Bedrock — sem mais nada, o item aparece com a textura do material base. O que resolve é o
Geyser registrar um item Bedrock próprio para cada `item_model` nosso, e isso pede dois arquivos:

```
./empacotar-bedrock.sh
```

Sai `target/ItensDaniel-bedrock.mcpack` e `target/itensdaniel-geyser-mappings.json` — os dois
também vêm anexados em cada [release](https://github.com/eupolemo/ItensDaniel/releases/latest).
No servidor:

```
plugins/Geyser-Spigot/packs/ItensDaniel-bedrock.mcpack
plugins/Geyser-Spigot/custom_mappings/itensdaniel-geyser-mappings.json
```

E em `plugins/Geyser-Spigot/config.yml`:

```yaml
enable-custom-content: true
```

Reinicie o servidor. O mapeamento casa pelo componente `item_model`, suportado pelo Geyser a
partir do 1.21.4 — a mesma versão que o plugin tem como alvo.

Os UUIDs do `manifest.json` são fixos no `empacotar-bedrock.sh` de propósito: trocar faz o cliente
tratar como outro pack e baixar tudo de novo.

**Limite:** isto troca o **ícone** do item. Armadura *vestida* continua com a aparência vanilla do
material nas duas edições — é limite do Minecraft, não do Geyser.

**Item novo seu** com textura própria não entra nos mappings sozinho: a tabela `ITENS` do
`empacotar-bedrock.sh` lista os itens que já vêm configurados. Acrescente a linha dele lá.

Se um jogador Bedrock não consegue `/itens dar`, quase sempre é permissão — o Floodgate prefixa o
nome dele (padrão `.`):

```
op .NomeDoJogador
```
