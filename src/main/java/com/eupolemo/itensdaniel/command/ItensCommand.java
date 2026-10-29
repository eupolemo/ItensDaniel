package com.eupolemo.itensdaniel.command;

import com.eupolemo.itensdaniel.ItensDanielPlugin;
import com.eupolemo.itensdaniel.item.ItemCustomizado;
import com.eupolemo.itensdaniel.menu.MenuItens;
import com.eupolemo.itensdaniel.poder.Poder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ItensCommand implements CommandExecutor, TabCompleter {

    private final ItensDanielPlugin plugin;

    public ItensCommand(ItensDanielPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("itens.use")) {
            sender.sendMessage(Component.text("Sem permissao.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            listar(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (!sender.hasPermission("itens.reload")) {
                    sender.sendMessage(Component.text("Sem permissao para recarregar.", NamedTextColor.RED));
                    return true;
                }
                plugin.recarregar();
                sender.sendMessage(Component.text("Config recarregada: "
                        + plugin.getItemManager().todos().size() + " item(ns).", NamedTextColor.GREEN));
            }
            case "info" -> {
                ItemCustomizado item = exigirItem(sender, label, "info", args);
                if (item != null) {
                    info(sender, item);
                }
            }
            case "dar" -> dar(sender, label, args);
            case "menu" -> menu(sender);
            default -> ajuda(sender, label);
        }
        return true;
    }

    private void listar(CommandSender sender) {
        if (plugin.getItemManager().todos().isEmpty()) {
            sender.sendMessage(Component.text("Nenhum item configurado.", NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text("Itens especiais:", NamedTextColor.GOLD));
        for (ItemCustomizado item : plugin.getItemManager().todos()) {
            sender.sendMessage(Component.text(" - " + item.id() + " ", NamedTextColor.GRAY)
                    .append(Component.text(item.nome(), item.cor()))
                    .append(Component.text(" (" + item.poderes().size() + " poder(es))",
                            NamedTextColor.DARK_GRAY)));
        }
    }

    private void info(CommandSender sender, ItemCustomizado item) {
        sender.sendMessage(Component.text(item.nome(), item.cor()));
        for (String linha : item.descricao()) {
            sender.sendMessage(Component.text("  " + linha, NamedTextColor.GRAY));
        }
        sender.sendMessage(Component.text("  material: " + item.material().name(), NamedTextColor.DARK_GRAY));
        if (item.cooldownSegundos() > 0) {
            sender.sendMessage(Component.text("  cooldown: " + item.cooldownSegundos() + "s",
                    NamedTextColor.DARK_GRAY));
        }
        for (Poder poder : item.poderes()) {
            sender.sendMessage(Component.text("  " + poder.gatilho() + " -> " + poder.tipo(),
                    NamedTextColor.DARK_GRAY));
        }
    }

    private void dar(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("itens.dar")) {
            sender.sendMessage(Component.text("Sem permissao para entregar itens.", NamedTextColor.RED));
            return;
        }

        ItemCustomizado item = exigirItem(sender, label, "dar", args);
        if (item == null) {
            return;
        }

        Player alvo;
        if (args.length >= 3) {
            alvo = plugin.getServer().getPlayerExact(args[2]);
            if (alvo == null) {
                sender.sendMessage(Component.text("Jogador '" + args[2] + "' nao esta online.",
                        NamedTextColor.RED));
                return;
            }
        } else if (sender instanceof Player jogador) {
            alvo = jogador;
        } else {
            sender.sendMessage(Component.text("Do console, informe o jogador: /" + label
                    + " dar " + item.id() + " <jogador>", NamedTextColor.RED));
            return;
        }

        boolean caiuNoChao = plugin.getItemManager().entregar(alvo, item);

        alvo.sendMessage(Component.text("Voce recebeu ", NamedTextColor.GREEN)
                .append(Component.text(item.nome(), item.cor())));
        if (!alvo.equals(sender)) {
            sender.sendMessage(Component.text("Entregue a " + alvo.getName() + ": " + item.id(),
                    NamedTextColor.GREEN));
        }
        if (caiuNoChao) {
            sender.sendMessage(Component.text("Inventario cheio: o item caiu no chao.",
                    NamedTextColor.YELLOW));
        }
    }

    private void menu(CommandSender sender) {
        if (!(sender instanceof Player jogador)) {
            sender.sendMessage(Component.text("So um jogador pode abrir o menu. Do console, use "
                    + "/itens dar <id> <jogador>.", NamedTextColor.RED));
            return;
        }
        if (plugin.getItemManager().todos().isEmpty()) {
            sender.sendMessage(Component.text("Nenhum item configurado.", NamedTextColor.YELLOW));
            return;
        }
        // O menu e montado na hora: assim ele ja nasce com o que o ultimo /itens reload carregou.
        jogador.openInventory(new MenuItens(plugin).getInventory());
    }

    private @Nullable ItemCustomizado exigirItem(CommandSender sender, String label,
                                                 String sub, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /" + label + " " + sub + " <id>", NamedTextColor.RED));
            return null;
        }
        ItemCustomizado item = plugin.getItemManager().porId(args[1]);
        if (item == null) {
            sender.sendMessage(Component.text("Item '" + args[1] + "' nao existe. Veja /" + label + ".",
                    NamedTextColor.RED));
        }
        return item;
    }

    private void ajuda(CommandSender sender, String label) {
        sender.sendMessage(Component.text("/" + label + " - lista os itens", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/" + label + " menu - abre a vitrine de itens",
                NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/" + label + " info <id> - detalhes do item", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/" + label + " dar <id> [jogador] - entrega o item",
                NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/" + label + " reload - recarrega o config.yml",
                NamedTextColor.GRAY));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("itens.use")) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("info", "menu"));
            if (sender.hasPermission("itens.dar")) {
                subs.add("dar");
            }
            if (sender.hasPermission("itens.reload")) {
                subs.add("reload");
            }
            return filtrar(subs, args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("dar"))) {
            return filtrar(new ArrayList<>(plugin.getItemManager().ids()), args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("dar")) {
            List<String> nomes = new ArrayList<>();
            for (Player jogador : plugin.getServer().getOnlinePlayers()) {
                nomes.add(jogador.getName());
            }
            return filtrar(nomes, args[2]);
        }

        return List.of();
    }

    private static List<String> filtrar(List<String> opcoes, String prefixo) {
        String alvo = prefixo.toLowerCase(Locale.ROOT);
        List<String> resultado = new ArrayList<>();
        for (String opcao : opcoes) {
            if (opcao.toLowerCase(Locale.ROOT).startsWith(alvo)) {
                resultado.add(opcao);
            }
        }
        return resultado;
    }
}
