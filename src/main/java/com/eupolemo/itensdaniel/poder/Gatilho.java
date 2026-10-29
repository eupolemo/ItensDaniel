package com.eupolemo.itensdaniel.poder;

/** Quando um poder dispara. */
public enum Gatilho {
    /** Botao direito com o item na mao principal. */
    CLIQUE_DIREITO,
    /** Botao esquerdo em um bloco, com o item na mao principal. */
    CLIQUE_ESQUERDO,
    /** Ao quebrar um bloco com o item na mao principal. */
    AO_MINERAR,
    /** Enquanto o item esta na mao (principal ou secundaria) ou vestido. */
    PASSIVO,
    /** Ao acertar uma criatura com o item na mao. */
    AO_ACERTAR,
    /** Ao levar dano de uma criatura com o item na mao ou vestido. */
    AO_SER_ACERTADO
}
