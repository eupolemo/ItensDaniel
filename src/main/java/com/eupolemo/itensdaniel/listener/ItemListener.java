package com.eupolemo.itensdaniel.listener;

import com.eupolemo.itensdaniel.ItensDanielPlugin;
import com.eupolemo.itensdaniel.item.ItemCustomizado;
import com.eupolemo.itensdaniel.poder.Gatilho;
import com.eupolemo.itensdaniel.poder.Poder;
import com.eupolemo.itensdaniel.poder.TipoPoder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ItemListener implements Listener {

    private final ItensDanielPlugin plugin;

    public ItemListener(ItensDanielPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteragir(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            // O evento dispara uma vez por mao; so a principal vale, senao o poder sai em dobro.
            return;
        }
        ItemCustomizado item = plugin.getItemManager().de(event.getItem());
        if (item == null) {
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null
                && item.temGatilho(Gatilho.CLIQUE_ESQUERDO)
                && event.getPlayer().hasPermission("itens.use")) {
            plugin.getMineracao().quebrarIndestrutivel(event.getPlayer(), event.getClickedBlock());
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!item.temGatilho(Gatilho.CLIQUE_DIREITO)) {
            return;
        }

        Player jogador = event.getPlayer();
        // Cancela antes de qualquer checagem para o item nunca ser plantado, comido ou usado
        // como o material original enquanto o poder falha.
        event.setCancelled(true);

        if (!jogador.hasPermission("itens.use")) {
            jogador.sendActionBar(Component.text("Sem permissao para usar itens especiais.",
                    NamedTextColor.RED));
            return;
        }

        long restante = plugin.getCooldowns().restanteMillis(jogador, item.id());
        if (restante > 0) {
            jogador.sendActionBar(Component.text(
                    item.nome() + " recarregando: " + String.format("%.1f", restante / 1000.0) + "s",
                    NamedTextColor.YELLOW));
            return;
        }

        if (plugin.getPoderes().cliqueDireito(jogador, item)) {
            plugin.getCooldowns().marcar(jogador, item.id(), item.cooldownSegundos());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDano(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity alvo)) {
            return;
        }
        Entity causador = event.getDamager();

        if (causador instanceof Player atacante && atacante.hasPermission("itens.use")) {
            ItemCustomizado arma = plugin.getItemManager()
                    .de(atacante.getInventory().getItemInMainHand());
            if (arma != null && arma.temGatilho(Gatilho.AO_ACERTAR)) {
                plugin.getPoderes().aoAcertar(atacante, alvo, event.getFinalDamage(), arma);
            }
        }

        if (alvo instanceof Player vitima && vitima.hasPermission("itens.use")) {
            ItemCustomizado defesa = primeiroCom(vitima, Gatilho.AO_SER_ACERTADO);
            if (defesa != null) {
                plugin.getPoderes().aoSerAcertado(vitima, causador, defesa);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onQuebrarBloco(BlockBreakEvent event) {
        if (plugin.getMineracao().ocupado()) {
            // Evento que nos mesmos disparamos para os blocos vizinhos.
            return;
        }

        Player jogador = event.getPlayer();
        ItemStack ferramenta = jogador.getInventory().getItemInMainHand();
        ItemCustomizado item = plugin.getItemManager().de(ferramenta);
        if (item == null || !item.temGatilho(Gatilho.AO_MINERAR)
                || !jogador.hasPermission("itens.use")) {
            return;
        }

        boolean fundir = temPoder(item, TipoPoder.FUNDIR);
        Poder area = poder(item, TipoPoder.AREA);

        if (fundir && event.isDropItems() && jogador.getGameMode() != GameMode.CREATIVE) {
            // Precisa ler os drops antes de o bloco virar ar. Vem null quando a fornalha nao
            // mudaria nada: ai o jogo solta o drop e a experiencia sozinho, como sempre.
            List<ItemStack> fundidos = plugin.getMineracao()
                    .dropsFundidos(jogador, event.getBlock(), ferramenta);
            if (fundidos != null) {
                event.setDropItems(false);
                plugin.getMineracao().dropar(event.getBlock().getWorld(),
                        event.getBlock().getLocation().add(0.5, 0.5, 0.5), fundidos);
            }
        }

        if (area != null) {
            plugin.getMineracao().quebrarArea(jogador, event.getBlock(), faceMirada(jogador),
                    ferramenta, Math.max(1, (int) area.alcance()), fundir);
        }
    }

    /** @return a face do bloco que o jogador esta olhando; UP quando nao da para saber */
    private BlockFace faceMirada(Player jogador) {
        RayTraceResult mira = jogador.rayTraceBlocks(6.0);
        BlockFace face = mira == null ? null : mira.getHitBlockFace();
        return face == null ? BlockFace.UP : face;
    }

    private static boolean temPoder(ItemCustomizado item, TipoPoder tipo) {
        return poder(item, tipo) != null;
    }

    private static @Nullable Poder poder(ItemCustomizado item, TipoPoder tipo) {
        for (Poder poder : item.poderes()) {
            if (poder.tipo() == tipo) {
                return poder;
            }
        }
        return null;
    }

    /** Bolas de fogo do plugin com 'quebra-blocos: false' explodem sem comer o terreno. */
    @EventHandler(ignoreCancelled = true)
    public void onExplosao(EntityExplodeEvent event) {
        Byte marca = event.getEntity().getPersistentDataContainer()
                .get(plugin.getPoderes().chaveSemGrief(), PersistentDataType.BYTE);
        if (marca != null) {
            event.blockList().clear();
        }
    }

    @EventHandler
    public void onSair(PlayerQuitEvent event) {
        plugin.getCooldowns().esquecer(event.getPlayer().getUniqueId());
        plugin.getPassivosTask().esquecer(event.getPlayer().getUniqueId());
        plugin.getLuzDinamica().definirAtivo(event.getPlayer().getUniqueId(), false);
    }

    /** @return o primeiro item equipado com esse gatilho: maos primeiro, depois a armadura */
    private ItemCustomizado primeiroCom(Player jogador, Gatilho gatilho) {
        ItemCustomizado naMao = plugin.getItemManager().de(jogador.getInventory().getItemInMainHand());
        if (naMao != null && naMao.temGatilho(gatilho)) {
            return naMao;
        }
        ItemCustomizado naOutraMao = plugin.getItemManager().de(jogador.getInventory().getItemInOffHand());
        if (naOutraMao != null && naOutraMao.temGatilho(gatilho)) {
            return naOutraMao;
        }
        for (ItemStack peca : jogador.getInventory().getArmorContents()) {
            ItemCustomizado vestido = plugin.getItemManager().de(peca);
            if (vestido != null && vestido.temGatilho(gatilho)) {
                return vestido;
            }
        }
        return null;
    }
}
