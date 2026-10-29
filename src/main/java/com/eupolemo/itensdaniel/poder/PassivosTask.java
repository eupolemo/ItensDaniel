package com.eupolemo.itensdaniel.poder;

import com.eupolemo.itensdaniel.ItensDanielPlugin;
import com.eupolemo.itensdaniel.item.ItemCustomizado;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Reaplica os poderes passivos de quem esta segurando ou vestindo um item nosso.
 *
 * <p>Roda a cada segundo e os efeitos duram tres. Assim o efeito some sozinho pouco depois de
 * o jogador guardar o item, sem precisar detectar cada troca de slot ou de armadura.
 */
public final class PassivosTask implements Runnable {

    public static final long PERIODO_TICKS = 20L;
    private static final int DURACAO_EFEITO_TICKS = 60;

    private final ItensDanielPlugin plugin;
    /** Quem esta voando por causa de um item nosso. Sem isso nao daria para saber a quem tirar o voo. */
    private final Set<UUID> vooConcedido = new HashSet<>();

    public PassivosTask(ItensDanielPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player jogador : plugin.getServer().getOnlinePlayers()) {
            List<Poder> passivos = passivosDe(jogador);

            boolean querVoo = false;
            boolean querLuz = false;
            for (Poder poder : passivos) {
                if (poder.tipo() == TipoPoder.VOO) {
                    querVoo = true;
                } else if (poder.tipo() == TipoPoder.LUZ) {
                    querLuz = true;
                }
            }

            plugin.getPoderes().passivos(jogador, passivos, DURACAO_EFEITO_TICKS);
            aplicarVoo(jogador, querVoo);
            plugin.getLuzDinamica().definirAtivo(jogador.getUniqueId(), querLuz);
        }
    }

    private List<Poder> passivosDe(Player jogador) {
        List<Poder> poderes = new ArrayList<>();
        PlayerInventory inventario = jogador.getInventory();

        somar(poderes, inventario.getItemInMainHand());
        somar(poderes, inventario.getItemInOffHand());
        for (ItemStack peca : inventario.getArmorContents()) {
            somar(poderes, peca);
        }
        return poderes;
    }

    private void somar(List<Poder> poderes, ItemStack stack) {
        ItemCustomizado item = plugin.getItemManager().de(stack);
        if (item != null) {
            poderes.addAll(item.poderesDe(Gatilho.PASSIVO));
        }
    }

    private void aplicarVoo(Player jogador, boolean querVoo) {
        GameMode modo = jogador.getGameMode();
        if (modo == GameMode.CREATIVE || modo == GameMode.SPECTATOR) {
            // Nesses modos o voo e do proprio jogo; nao e nosso para dar nem para tirar.
            vooConcedido.remove(jogador.getUniqueId());
            return;
        }

        if (querVoo) {
            jogador.setAllowFlight(true);
            vooConcedido.add(jogador.getUniqueId());
        } else if (vooConcedido.remove(jogador.getUniqueId())) {
            jogador.setFlying(false);
            jogador.setAllowFlight(false);
        }
    }

    public void esquecer(UUID jogador) {
        vooConcedido.remove(jogador);
    }

    /** Devolve o voo ao estado normal antes de o plugin descarregar. */
    public void desligar() {
        for (Player jogador : plugin.getServer().getOnlinePlayers()) {
            aplicarVoo(jogador, false);
        }
        vooConcedido.clear();
    }
}
