package com.eupolemo.itensdaniel.poder;

import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

/**
 * Um poder ja resolvido do config.yml. Nem todo campo vale para todo tipo: cada tipo le so os
 * seus, e os demais ficam no valor padrao.
 *
 * @param tipo         qual poder
 * @param alcance      distancia de mira, em blocos (RAIO, TELEPORTE)
 * @param efeito       efeito de pocao aplicado (EFEITO); null nos outros tipos
 * @param nivel        nivel do efeito, comecando em 1 (EFEITO, CONGELAR)
 * @param duracaoTicks duracao do efeito no alvo (CONGELAR)
 * @param fator        multiplicador ou quantidade, conforme o tipo
 * @param potencia     raio da explosao (BOLA_DE_FOGO)
 * @param velocidade   velocidade do projetil (BOLA_DE_FOGO)
 * @param incendiario  se a explosao acende fogo (BOLA_DE_FOGO)
 * @param quebraBlocos se a explosao destroi o terreno (BOLA_DE_FOGO)
 */
public record Poder(
        TipoPoder tipo,
        double alcance,
        @Nullable PotionEffectType efeito,
        int nivel,
        int duracaoTicks,
        double fator,
        double potencia,
        double velocidade,
        boolean incendiario,
        boolean quebraBlocos
) {

    public Gatilho gatilho() {
        return tipo.gatilho();
    }
}
