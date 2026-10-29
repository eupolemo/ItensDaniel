package com.eupolemo.itensdaniel.item;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Espera entre usos, por jogador e por item. Vive so em memoria: reiniciar o servidor zera. */
public final class Cooldowns {

    private final Map<UUID, Map<String, Long>> proximoUso = new HashMap<>();

    /** @return quantos milissegundos faltam, ou 0 se o item ja pode ser usado */
    public long restanteMillis(Player jogador, String idItem) {
        Map<String, Long> doJogador = proximoUso.get(jogador.getUniqueId());
        if (doJogador == null) {
            return 0;
        }
        Long quando = doJogador.get(idItem);
        if (quando == null) {
            return 0;
        }
        return Math.max(0, quando - System.currentTimeMillis());
    }

    public void marcar(Player jogador, String idItem, int segundos) {
        if (segundos <= 0) {
            return;
        }
        proximoUso.computeIfAbsent(jogador.getUniqueId(), id -> new HashMap<>())
                .put(idItem, System.currentTimeMillis() + segundos * 1000L);
    }

    public void esquecer(UUID jogador) {
        proximoUso.remove(jogador);
    }

    public void limpar() {
        proximoUso.clear();
    }
}
