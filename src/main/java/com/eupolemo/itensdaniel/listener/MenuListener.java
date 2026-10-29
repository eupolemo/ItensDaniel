package com.eupolemo.itensdaniel.listener;

import com.eupolemo.itensdaniel.ItensDanielPlugin;
import com.eupolemo.itensdaniel.item.ItemCustomizado;
import com.eupolemo.itensdaniel.menu.MenuItens;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class MenuListener implements Listener {

    private final ItensDanielPlugin plugin;

    public MenuListener(ItensDanielPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClicar(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuItens menu)) {
            return;
        }
        // O menu e uma vitrine: nada sai, entra ou troca de lugar nele. Sem isso, shift-clique e
        // hotbar-swap tirariam a peca da vitrine em vez de copiar.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player jogador)) {
            return;
        }
        if (!event.getView().getTopInventory().equals(event.getClickedInventory())) {
            return;
        }

        ItemCustomizado item = menu.item(event.getSlot());
        if (item == null) {
            return;
        }
        if (!jogador.hasPermission("itens.dar")) {
            jogador.sendMessage(Component.text("Voce pode ver os itens, mas nao pegar.",
                    NamedTextColor.RED));
            return;
        }

        boolean caiuNoChao = plugin.getItemManager().entregar(jogador, item);
        jogador.playSound(jogador.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.2f);
        jogador.sendActionBar(Component.text(caiuNoChao
                ? item.nome() + " caiu no chao: inventario cheio."
                : item.nome() + " no inventario.", NamedTextColor.GREEN));
    }

    @EventHandler
    public void onArrastar(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuItens) {
            event.setCancelled(true);
        }
    }
}
