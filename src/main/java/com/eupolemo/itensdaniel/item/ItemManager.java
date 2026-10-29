package com.eupolemo.itensdaniel.item;

import com.eupolemo.itensdaniel.ItensDanielPlugin;
import com.eupolemo.itensdaniel.poder.Poder;
import com.eupolemo.itensdaniel.poder.TipoPoder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Le os itens do config.yml, cria as ItemStacks e reconhece um item nosso na mao do jogador. */
public final class ItemManager {

    /** Mais rapido que picareta de netherita (9), para a multiferramenta minerar quase instantaneo. */
    private static final float VELOCIDADE_MULTIFERRAMENTA = 12f;

    private final ItensDanielPlugin plugin;
    /**
     * Gravada no proprio item. E ela, e nao o nome, que identifica um item nosso: assim ele
     * continua funcionando depois de renomeado na bigorna ou de uma mudanca no config.yml.
     */
    private final NamespacedKey chaveId;

    /** id -> item, na ordem do arquivo. */
    private Map<String, ItemCustomizado> porId = Collections.emptyMap();

    public ItemManager(ItensDanielPlugin plugin) {
        this.plugin = plugin;
        this.chaveId = new NamespacedKey(plugin, "item-id");
    }

    public void carregar() {
        Map<String, ItemCustomizado> itens = new LinkedHashMap<>();

        ConfigurationSection raiz = plugin.getConfig().getConfigurationSection("itens");
        if (raiz == null) {
            plugin.getLogger().warning("Secao 'itens' ausente no config.yml. Nenhum item carregado.");
        } else {
            for (String id : raiz.getKeys(false)) {
                ConfigurationSection sec = raiz.getConfigurationSection(id);
                if (sec == null) {
                    plugin.getLogger().warning("Item '" + id + "' ignorado: entrada nao e um mapa.");
                    continue;
                }

                Material material = Material.matchMaterial(sec.getString("material", ""));
                if (material == null || !material.isItem()) {
                    plugin.getLogger().warning("Item '" + id + "' ignorado: material '"
                            + sec.getString("material") + "' nao existe ou nao pode ir para o inventario.");
                    continue;
                }

                ItemCustomizado item = new ItemCustomizado(
                        id.toLowerCase(Locale.ROOT),
                        sec.getString("nome", id),
                        ItemCustomizado.parseCor(sec.getString("cor")),
                        material,
                        sec.getStringList("descricao"),
                        sec.getBoolean("brilho", false),
                        sec.getBoolean("inquebravel", false),
                        sec.getInt("cooldown-segundos", 0),
                        lerModelo(id, sec.getString("modelo")),
                        sec.isSet("custom-model-data") ? sec.getInt("custom-model-data") : null,
                        sec.getBoolean("multiferramenta", false),
                        lerAtributos(id, sec.getMapList("atributos")),
                        lerPoderes(id, sec.getMapList("poderes"))
                );

                if (item.poderes().isEmpty()) {
                    plugin.getLogger().warning("Item '" + id + "' nao tem nenhum poder valido. "
                            + "Ele existe, mas nao faz nada.");
                }
                itens.put(item.id(), item);
            }
        }

        this.porId = Collections.unmodifiableMap(itens);
    }

    /**
     * @param bruto valor de 'modelo': "itensdaniel:varinha-tempestade" ou so "varinha-tempestade",
     *              que assume o namespace do plugin
     * @return a chave do modelo no resource pack, ou null se o item usa a textura do material
     */
    private @Nullable NamespacedKey lerModelo(String idItem, @Nullable String bruto) {
        if (bruto == null || bruto.isBlank()) {
            return null;
        }
        @SuppressWarnings("deprecation")
        NamespacedKey chave = NamespacedKey.fromString(bruto.toLowerCase(Locale.ROOT), plugin);
        if (chave == null) {
            plugin.getLogger().warning("Item '" + idItem + "': modelo '" + bruto
                    + "' nao e um identificador valido. Use letras minusculas, numeros, '.', '_' e '-'.");
        }
        return chave;
    }

    private List<Atributo> lerAtributos(String idItem, List<Map<?, ?>> entradas) {
        List<Atributo> atributos = new ArrayList<>();
        for (Map<?, ?> entrada : entradas) {
            String nome = String.valueOf(entrada.get("atributo")).toLowerCase(Locale.ROOT);
            NamespacedKey chave = NamespacedKey.fromString(nome);
            Attribute atributo = chave == null ? null : Registry.ATTRIBUTE.get(chave);
            if (atributo == null) {
                plugin.getLogger().warning("Item '" + idItem + "': atributo '" + nome
                        + "' nao existe. Ex: max_health, attack_damage, movement_speed.");
                continue;
            }

            AttributeModifier.Operation operacao;
            try {
                operacao = AttributeModifier.Operation.valueOf(
                        texto(entrada, "operacao", "ADD_NUMBER").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Item '" + idItem + "': operacao '" + entrada.get("operacao")
                        + "' invalida. Use ADD_NUMBER, ADD_SCALAR ou MULTIPLY_SCALAR_1.");
                continue;
            }

            String nomeSlot = texto(entrada, "slot", "any").toLowerCase(Locale.ROOT);
            EquipmentSlotGroup slot = EquipmentSlotGroup.getByName(nomeSlot);
            if (slot == null) {
                plugin.getLogger().warning("Item '" + idItem + "': slot '" + nomeSlot
                        + "' invalido. Use any, hand, mainhand, offhand, head, chest, legs, feet ou armor.");
                continue;
            }

            atributos.add(new Atributo(atributo, numero(entrada, "valor", 0.0), operacao, slot));
        }
        return atributos;
    }

    private List<Poder> lerPoderes(String idItem, List<Map<?, ?>> entradas) {
        List<Poder> poderes = new ArrayList<>();
        for (Map<?, ?> entrada : entradas) {
            String nomeTipo = String.valueOf(entrada.get("tipo")).toUpperCase(Locale.ROOT);
            TipoPoder tipo;
            try {
                tipo = TipoPoder.valueOf(nomeTipo);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Poder '" + nomeTipo + "' do item '" + idItem
                        + "' ignorado: tipo desconhecido. Validos: " + tiposValidos());
                continue;
            }

            PotionEffectType efeito = null;
            Object nomeEfeito = entrada.get("efeito");
            if (nomeEfeito != null) {
                efeito = buscarEfeito(String.valueOf(nomeEfeito));
                if (efeito == null) {
                    plugin.getLogger().warning("Poder " + tipo + " do item '" + idItem
                            + "' ignorado: efeito '" + nomeEfeito + "' nao existe.");
                    continue;
                }
            }
            if (tipo == TipoPoder.EFEITO && efeito == null) {
                plugin.getLogger().warning("Poder EFEITO do item '" + idItem
                        + "' ignorado: falta a chave 'efeito'.");
                continue;
            }

            poderes.add(new Poder(
                    tipo,
                    numero(entrada, "alcance", 30.0),
                    efeito,
                    Math.max(1, (int) numero(entrada, "nivel", 1)),
                    Math.max(1, (int) numero(entrada, "duracao-ticks", 60)),
                    numero(entrada, "fator", 1.0),
                    numero(entrada, "potencia", 2.0),
                    numero(entrada, "velocidade", 1.5),
                    booleano(entrada, "incendiario", false),
                    booleano(entrada, "quebra-blocos", false)
            ));
        }
        return poderes;
    }

    private static double numero(Map<?, ?> entrada, String chave, double padrao) {
        Object valor = entrada.get(chave);
        return valor instanceof Number numero ? numero.doubleValue() : padrao;
    }

    private static String texto(Map<?, ?> entrada, String chave, String padrao) {
        Object valor = entrada.get(chave);
        return valor == null ? padrao : String.valueOf(valor);
    }

    private static boolean booleano(Map<?, ?> entrada, String chave, boolean padrao) {
        Object valor = entrada.get(chave);
        return valor instanceof Boolean booleano ? booleano : padrao;
    }

    /** @return o efeito de pocao pelo nome do vanilla, ex: "speed", "night_vision" */
    private static @Nullable PotionEffectType buscarEfeito(String nome) {
        NamespacedKey chave = NamespacedKey.fromString(nome.toLowerCase(Locale.ROOT));
        return chave == null ? null : Registry.EFFECT.get(chave);
    }

    private static String tiposValidos() {
        List<String> nomes = new ArrayList<>();
        for (TipoPoder tipo : TipoPoder.values()) {
            nomes.add(tipo.name());
        }
        return String.join(", ", nomes);
    }

    /** @return a ItemStack pronta para entregar ao jogador */
    public ItemStack criar(ItemCustomizado item) {
        ItemStack stack = new ItemStack(item.material());
        ItemMeta meta = stack.getItemMeta();

        meta.displayName(Component.text(item.nome(), item.cor())
                .decoration(TextDecoration.ITALIC, false));

        if (!item.descricao().isEmpty()) {
            List<Component> lore = new ArrayList<>(item.descricao().size());
            for (String linha : item.descricao()) {
                lore.add(Component.text(linha, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
        }

        if (item.brilho()) {
            meta.setEnchantmentGlintOverride(true);
        }
        if (item.inquebravel()) {
            meta.setUnbreakable(true);
        }
        if (item.modelo() != null) {
            meta.setItemModel(item.modelo());
        }
        if (item.customModelData() != null) {
            meta.setCustomModelData(item.customModelData());
        }
        if (item.multiferramenta()) {
            meta.setTool(multiferramenta());
        }
        for (int i = 0; i < item.atributos().size(); i++) {
            Atributo atributo = item.atributos().get(i);
            // A chave precisa ser unica por modificador, senao o segundo sobrescreve o primeiro.
            NamespacedKey chave = new NamespacedKey(plugin, item.id() + "-" + i);
            meta.addAttributeModifier(atributo.atributo(), new AttributeModifier(
                    chave, atributo.valor(), atributo.operacao(), atributo.slot()));
        }
        meta.getPersistentDataContainer().set(chaveId, PersistentDataType.STRING, item.id());

        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Monta o componente 'tool' que faz o item minerar qualquer bloco rapido e com o drop certo,
     * seja ele espada, picareta ou cenoura.
     *
     * <p>As regras vem das tags mineable/* do proprio jogo em vez de uma lista de materiais: sao
     * quatro entradas no item em vez de mil, e blocos novos de futuras versoes ja entram junto.
     */
    private ToolComponent multiferramenta() {
        ToolComponent tool = new ItemStack(Material.STICK).getItemMeta().getTool();
        tool.setDefaultMiningSpeed(VELOCIDADE_MULTIFERRAMENTA);
        tool.setDamagePerBlock(0);

        for (String nome : List.of("mineable/pickaxe", "mineable/axe", "mineable/shovel", "mineable/hoe")) {
            Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS,
                    NamespacedKey.minecraft(nome), Material.class);
            if (tag == null) {
                plugin.getLogger().warning("Tag '" + nome + "' nao existe nesta versao do servidor. "
                        + "A multiferramenta pode nao dropar tudo.");
                continue;
            }
            tool.addRule(tag, VELOCIDADE_MULTIFERRAMENTA, true);
        }
        return tool;
    }

    /**
     * Coloca o item no inventario do jogador, jogando no chao o que nao couber.
     *
     * @return true se algo caiu no chao por falta de espaco
     */
    public boolean entregar(Player jogador, ItemCustomizado item) {
        Map<Integer, ItemStack> sobra = jogador.getInventory().addItem(criar(item));
        for (ItemStack naoCoube : sobra.values()) {
            jogador.getWorld().dropItemNaturally(jogador.getLocation(), naoCoube);
        }
        return !sobra.isEmpty();
    }

    /** @return o item customizado que esta stack representa, ou null se for um item comum */
    public @Nullable ItemCustomizado de(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return null;
        }
        String id = stack.getItemMeta().getPersistentDataContainer()
                .get(chaveId, PersistentDataType.STRING);
        return id == null ? null : porId.get(id);
    }

    public @Nullable ItemCustomizado porId(String id) {
        return porId.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<ItemCustomizado> todos() {
        return porId.values();
    }

    public Collection<String> ids() {
        return porId.keySet();
    }
}
