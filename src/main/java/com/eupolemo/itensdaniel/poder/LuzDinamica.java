package com.eupolemo.itensdaniel.poder;

import com.eupolemo.itensdaniel.ItensDanielPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Faz o jogador andar dentro de um bloco de luz invisivel, que muda de lugar junto com ele.
 *
 * <p>Guarda onde cada luz foi posta para apagar exatamente aquele bloco depois. Sem esse registro
 * o mundo ficaria salpicado de blocos de luz que ninguem ve nem consegue achar.
 */
public final class LuzDinamica implements Runnable {

    /**
     * Cinco ticks (quatro vezes por segundo). A task de passivos roda a cada segundo, e nesse
     * ritmo a luz ficaria pulando varios blocos atras do jogador que anda.
     */
    public static final long PERIODO_TICKS = 5L;

    private final ItensDanielPlugin plugin;
    private final Map<UUID, Location> acesas = new HashMap<>();
    /** Quem esta com um item de LUZ equipado, segundo a ultima passada da task de passivos. */
    private final Set<UUID> ativos = new HashSet<>();

    public LuzDinamica(ItensDanielPlugin plugin) {
        this.plugin = plugin;
    }

    /** Liga ou desliga a luz de um jogador. Chamado pela task de passivos. */
    public void definirAtivo(UUID jogador, boolean ativo) {
        if (ativo) {
            ativos.add(jogador);
        } else if (ativos.remove(jogador)) {
            apagar(jogador);
        }
    }

    @Override
    public void run() {
        for (UUID id : List.copyOf(ativos)) {
            Player jogador = plugin.getServer().getPlayer(id);
            if (jogador == null) {
                definirAtivo(id, false);
            } else {
                acender(jogador);
            }
        }
    }

    public void acender(Player jogador) {
        Location alvo = jogador.getLocation().getBlock().getLocation();
        Location atual = acesas.get(jogador.getUniqueId());
        if (alvo.equals(atual)) {
            return;
        }

        apagar(jogador.getUniqueId());

        Block bloco = alvo.getBlock();
        if (!vazio(bloco)) {
            // Pes dentro de escada, grama alta ou agua: tenta a altura da cabeca.
            bloco = jogador.getEyeLocation().getBlock();
            if (!vazio(bloco)) {
                return;
            }
        }

        BlockData dados = Material.LIGHT.createBlockData();
        if (dados instanceof Levelled luz) {
            luz.setLevel(luz.getMaximumLevel());
            dados = luz;
        }
        bloco.setBlockData(dados, false);
        acesas.put(jogador.getUniqueId(), bloco.getLocation());
    }

    public void apagar(UUID jogador) {
        Location onde = acesas.remove(jogador);
        if (onde != null && onde.getBlock().getType() == Material.LIGHT) {
            onde.getBlock().setType(Material.AIR, false);
        }
    }

    /** Apaga tudo antes de o plugin descarregar, senao as luzes ficam no mundo para sempre. */
    public void desligar() {
        for (UUID jogador : List.copyOf(acesas.keySet())) {
            apagar(jogador);
        }
        ativos.clear();
    }

    private static boolean vazio(Block bloco) {
        return bloco.getType() == Material.AIR || bloco.getType() == Material.CAVE_AIR;
    }
}
