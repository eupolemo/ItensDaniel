package com.eupolemo.itensdaniel.poder;

/**
 * Catalogo de poderes. Cada tipo ja sabe em que gatilho roda, entao o config.yml so precisa
 * declarar o tipo; o listener certo encontra o poder sozinho.
 */
public enum TipoPoder {

    /** Cai um raio no bloco mirado. Usa 'alcance'. */
    RAIO(Gatilho.CLIQUE_DIREITO),
    /** Teleporta para o bloco mirado. Usa 'alcance'. */
    TELEPORTE(Gatilho.CLIQUE_DIREITO),
    /** Lanca uma bola de fogo. Usa 'potencia', 'velocidade', 'incendiario', 'quebra-blocos'. */
    BOLA_DE_FOGO(Gatilho.CLIQUE_DIREITO),
    /** Cura quem usou. Usa 'fator' como pontos de vida (2 = um coracao). */
    CURA(Gatilho.CLIQUE_DIREITO),
    /** Arremessa quem usou na direcao em que olha. Usa 'fator' como forca. */
    IMPULSO(Gatilho.CLIQUE_DIREITO),

    /** Quebra blocos que nem o criativo quebra, como a bedrock. */
    QUEBRA_INDESTRUTIVEL(Gatilho.CLIQUE_ESQUERDO),

    /** Quebra tambem os blocos vizinhos. Usa 'alcance' como raio (1 = 3x3). */
    AREA(Gatilho.AO_MINERAR),
    /** Ja entrega o que caiu fundido: minerio vira lingote, areia vira vidro. */
    FUNDIR(Gatilho.AO_MINERAR),

    /** Efeito de pocao continuo. Usa 'efeito' e 'nivel'. */
    EFEITO(Gatilho.PASSIVO),
    /** Permite voar como no criativo. */
    VOO(Gatilho.PASSIVO),
    /** Acende o lugar onde o jogador esta. */
    LUZ(Gatilho.PASSIVO),
    /** Puxa os itens caidos por perto. Usa 'alcance' como raio. */
    IMA(Gatilho.PASSIVO),

    /** Converte parte do dano causado em vida. Usa 'fator'. */
    ROUBO_DE_VIDA(Gatilho.AO_ACERTAR),
    /** Deixa o alvo lento. Usa 'nivel' e 'duracao-ticks'. */
    CONGELAR(Gatilho.AO_ACERTAR),

    /** Arremessa para longe quem atacou. Usa 'fator' como forca. */
    EMPURRAO(Gatilho.AO_SER_ACERTADO);

    private final Gatilho gatilho;

    TipoPoder(Gatilho gatilho) {
        this.gatilho = gatilho;
    }

    public Gatilho gatilho() {
        return gatilho;
    }
}
