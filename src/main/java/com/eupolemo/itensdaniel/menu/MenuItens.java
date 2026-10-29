package com.eupolemo.itensdaniel.menu;

import com.eupolemo.itensdaniel.ItensDanielPlugin;
import com.eupolemo.itensdaniel.item.ItemCustomizado;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Bau virtual com todos os itens do plugin, no lugar de uma aba do criativo: o menu do criativo
 * e montado pelo cliente a partir dos itens registrados no jogo, e os nossos sao itens vanilla
 * com componentes, entao nao existe aba para eles.
 *
 * <p>Cada instancia e o holder do proprio inventario. E assim que o listener sabe que o clique
 * veio daqui, sem depender do titulo da janela, que o jogador pode ver traduzido ou alterado.
 */
public final class MenuItens implements InventoryHolder {

    private static final int MAX_SLOTS = 54;

    private final Inventory inventario;
    /** Itens na ordem dos slots; o indice do slot e o indice nesta lista. */
    private final List<ItemCustomizado> ordem;

    public MenuItens(ItensDanielPlugin plugin) {
        List<ItemCustomizado> itens = new ArrayList<>(plugin.getItemManager().todos());
        if (itens.size() > MAX_SLOTS) {
            plugin.getLogger().warning("O menu mostra no maximo " + MAX_SLOTS + " itens; "
                    + (itens.size() - MAX_SLOTS) + " ficaram de fora. Use /itens dar para eles.");
            itens = itens.subList(0, MAX_SLOTS);
        }

        int linhas = Math.max(1, (itens.size() + 8) / 9);
        this.inventario = Bukkit.createInventory(this, linhas * 9,
                Component.text("Itens especiais", NamedTextColor.DARK_PURPLE));
        for (int slot = 0; slot < itens.size(); slot++) {
            inventario.setItem(slot, plugin.getItemManager().criar(itens.get(slot)));
        }
        this.ordem = List.copyOf(itens);
    }

    /** @return o item daquele slot, ou null se o slot esta vazio */
    public @Nullable ItemCustomizado item(int slot) {
        return slot >= 0 && slot < ordem.size() ? ordem.get(slot) : null;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventario;
    }
}
