package com.eupolemo.itensdaniel.item;

import com.eupolemo.itensdaniel.poder.Gatilho;
import com.eupolemo.itensdaniel.poder.Poder;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Um item declarado no config.yml.
 *
 * @param id                identificador usado no comando e gravado no proprio item
 * @param nome              nome exibido
 * @param cor               cor do nome
 * @param material          material base
 * @param descricao         linhas de lore
 * @param brilho            aplica o brilho de encantamento sem encantar
 * @param inquebravel       o item nao perde durabilidade
 * @param cooldownSegundos  espera entre usos dos poderes de clique direito
 * @param modelo            modelo do resource pack (componente item_model), ou null para usar a
 *                          textura do material base
 * @param customModelData   valor de custom_model_data, para packs no formato antigo; null se nao usa
 * @param multiferramenta   mina qualquer bloco rapido e com drop certo, seja qual for o material
 * @param atributos         modificadores de atributo aplicados pelo proprio jogo
 * @param poderes           poderes do item
 */
public record ItemCustomizado(
        String id,
        String nome,
        TextColor cor,
        Material material,
        List<String> descricao,
        boolean brilho,
        boolean inquebravel,
        int cooldownSegundos,
        @Nullable NamespacedKey modelo,
        @Nullable Integer customModelData,
        boolean multiferramenta,
        List<Atributo> atributos,
        List<Poder> poderes
) {

    public ItemCustomizado {
        descricao = List.copyOf(descricao);
        atributos = List.copyOf(atributos);
        poderes = List.copyOf(poderes);
    }

    /** @return os poderes que rodam neste gatilho, na ordem do config */
    public List<Poder> poderesDe(Gatilho gatilho) {
        List<Poder> filtrados = new ArrayList<>(poderes.size());
        for (Poder poder : poderes) {
            if (poder.gatilho() == gatilho) {
                filtrados.add(poder);
            }
        }
        return filtrados;
    }

    public boolean temGatilho(Gatilho gatilho) {
        for (Poder poder : poderes) {
            if (poder.gatilho() == gatilho) {
                return true;
            }
        }
        return false;
    }

    public static TextColor parseCor(String hex) {
        if (hex == null || hex.isBlank()) {
            return NamedTextColor.WHITE;
        }
        TextColor cor = TextColor.fromCSSHexString(hex.startsWith("#") ? hex : "#" + hex);
        return cor != null ? cor : NamedTextColor.WHITE;
    }
}
