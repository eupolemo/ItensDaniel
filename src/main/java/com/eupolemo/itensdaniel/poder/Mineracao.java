package com.eupolemo.itensdaniel.poder;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Quebra em area, quebra de bloco indestrutivel e fundicao do que cair. */
public final class Mineracao {

    /**
     * Blocos que ficam de fora mesmo com QUEBRA_INDESTRUTIVEL: sao ferramentas de mapa e de
     * administracao, e quebrar um deles por acidente estraga o mundo em vez de divertir.
     */
    private static final Set<Material> PROTEGIDOS = Set.of(
            Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
            Material.STRUCTURE_BLOCK, Material.STRUCTURE_VOID, Material.JIGSAW, Material.BARRIER,
            Material.LIGHT, Material.END_PORTAL, Material.END_GATEWAY, Material.NETHER_PORTAL,
            Material.MOVING_PISTON, Material.SPAWNER
    );

    /** O que a fornalha faria com cada drop. */
    private static final Map<Material, Material> FORNO = Map.ofEntries(
            Map.entry(Material.RAW_IRON, Material.IRON_INGOT),
            Map.entry(Material.RAW_GOLD, Material.GOLD_INGOT),
            Map.entry(Material.RAW_COPPER, Material.COPPER_INGOT),
            Map.entry(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP),
            Map.entry(Material.SAND, Material.GLASS),
            Map.entry(Material.RED_SAND, Material.GLASS),
            Map.entry(Material.COBBLESTONE, Material.STONE),
            Map.entry(Material.STONE, Material.SMOOTH_STONE),
            Map.entry(Material.COBBLED_DEEPSLATE, Material.DEEPSLATE),
            Map.entry(Material.NETHERRACK, Material.NETHER_BRICK),
            Map.entry(Material.CLAY_BALL, Material.BRICK),
            Map.entry(Material.CLAY, Material.TERRACOTTA),
            Map.entry(Material.SANDSTONE, Material.SMOOTH_SANDSTONE),
            Map.entry(Material.QUARTZ_BLOCK, Material.SMOOTH_QUARTZ),
            Map.entry(Material.WET_SPONGE, Material.SPONGE)
    );

    /**
     * Os eventos que este codigo dispara passam pelo nosso proprio listener. Sem esta trava, cada
     * bloco vizinho abriria outra area 3x3, e um clique viraria um buraco no mundo inteiro.
     */
    private boolean emAndamento;

    public boolean ocupado() {
        return emAndamento;
    }

    /**
     * Quebra um bloco que nem o criativo quebra, como a bedrock.
     *
     * @return true se o bloco foi quebrado
     */
    public boolean quebrarIndestrutivel(Player jogador, Block bloco) {
        Material tipo = bloco.getType();
        if (tipo.isAir() || PROTEGIDOS.contains(tipo)) {
            return false;
        }
        if (tipo.getHardness() >= 0) {
            // Bloco comum: o proprio jogo ja sabe quebrar, nao ha o que fazer aqui.
            return false;
        }

        emAndamento = true;
        try {
            if (!avisarEQuebrar(jogador, bloco)) {
                return false;
            }
        } finally {
            emAndamento = false;
        }

        quebrarSemDrop(bloco);
        if (tipo.isItem() && jogador.getGameMode() != GameMode.CREATIVE) {
            bloco.getWorld().dropItemNaturally(centro(bloco), new ItemStack(tipo));
        }
        bloco.getWorld().playSound(centro(bloco), Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 1f, 0.8f);
        return true;
    }

    /**
     * Quebra os vizinhos do bloco minerado, no plano da face atingida.
     *
     * @param raio 1 gera 3x3, 2 gera 5x5
     */
    public void quebrarArea(Player jogador, Block centro, BlockFace face, ItemStack ferramenta,
                            int raio, boolean fundir) {
        emAndamento = true;
        try {
            for (Block vizinho : vizinhos(centro, face, raio)) {
                Material tipo = vizinho.getType();
                if (tipo.isAir() || PROTEGIDOS.contains(tipo) || tipo.getHardness() < 0) {
                    continue;
                }
                if (!avisarEQuebrar(jogador, vizinho)) {
                    continue;
                }
                colher(jogador, vizinho, ferramenta, fundir);
            }
        } finally {
            emAndamento = false;
        }
    }

    /**
     * @return os drops ja fundidos, ou null se a fornalha nao mudaria nada. O null importa: nesse
     *         caso quem quebra o bloco e o proprio jogo, que tambem solta a experiencia — coisa
     *         que este plugin nao consegue reproduzir.
     */
    public @Nullable List<ItemStack> dropsFundidos(Player jogador, Block bloco, ItemStack ferramenta) {
        Collection<ItemStack> originais = bloco.getDrops(ferramenta, jogador);
        boolean mudou = false;
        List<ItemStack> saida = new ArrayList<>(originais.size());
        for (ItemStack drop : originais) {
            ItemStack fundido = fundido(drop);
            mudou |= fundido.getType() != drop.getType();
            saida.add(fundido);
        }
        return mudou ? saida : null;
    }

    public void dropar(World mundo, Location onde, List<ItemStack> itens) {
        for (ItemStack item : itens) {
            mundo.dropItemNaturally(onde, item);
        }
    }

    private void colher(Player jogador, Block bloco, ItemStack ferramenta, boolean fundir) {
        if (jogador.getGameMode() == GameMode.CREATIVE) {
            // No criativo o jogo nao dropa nada, e a area tem que se comportar igual.
            quebrarSemDrop(bloco);
            return;
        }
        List<ItemStack> fundidos = fundir ? dropsFundidos(jogador, bloco, ferramenta) : null;
        if (fundidos == null) {
            // Deixa o proprio jogo cuidar de drop, experiencia, particula, som e fisica.
            bloco.breakNaturally(ferramenta, true, true);
            return;
        }

        Location onde = centro(bloco);
        quebrarSemDrop(bloco);
        dropar(bloco.getWorld(), onde, fundidos);
    }

    /**
     * Tira o bloco do mundo com som e particula, sem drop.
     *
     * <p>A fisica precisa ficar ligada: e ela que faz a agua voltar a correr, a areia cair e a
     * grama que estava em cima soltar. Com ela desligada o mundo fica com blocos flutuando e
     * agua parada em volta do buraco.
     */
    public void quebrarSemDrop(Block bloco) {
        bloco.getWorld().playEffect(bloco.getLocation(), Effect.STEP_SOUND, bloco.getType());
        bloco.setType(Material.AIR, true);
    }

    private ItemStack fundido(ItemStack drop) {
        Material resultado = FORNO.get(drop.getType());
        return resultado == null ? drop : new ItemStack(resultado, drop.getAmount());
    }

    /**
     * Dispara um BlockBreakEvent normal antes de mexer no bloco, para que plugins de protecao
     * possam barrar. Se ninguem barrar, quem quebra somos nos.
     *
     * @return true se pode quebrar
     */
    private boolean avisarEQuebrar(Player jogador, Block bloco) {
        BlockBreakEvent evento = new BlockBreakEvent(bloco, jogador);
        evento.setDropItems(false);
        Bukkit.getPluginManager().callEvent(evento);
        return !evento.isCancelled();
    }

    /** @return os blocos do plano perpendicular a face, sem o centro */
    private List<Block> vizinhos(Block centro, BlockFace face, int raio) {
        List<Block> blocos = new ArrayList<>();
        for (int a = -raio; a <= raio; a++) {
            for (int b = -raio; b <= raio; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                blocos.add(switch (face) {
                    case UP, DOWN -> centro.getRelative(a, 0, b);
                    case NORTH, SOUTH -> centro.getRelative(a, b, 0);
                    default -> centro.getRelative(0, a, b);
                });
            }
        }
        return blocos;
    }

    private static Location centro(Block bloco) {
        return bloco.getLocation().add(0.5, 0.5, 0.5);
    }
}
