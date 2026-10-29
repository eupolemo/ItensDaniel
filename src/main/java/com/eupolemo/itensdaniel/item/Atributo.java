package com.eupolemo.itensdaniel.item;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;

/**
 * Um modificador de atributo gravado no item, como vida maxima ou dano de ataque. Quem aplica e
 * o proprio jogo, entao o valor entra e sai sozinho quando o jogador pega ou larga o item.
 *
 * @param atributo o atributo afetado
 * @param valor    quanto somar; o significado depende da operacao
 * @param operacao ADD_NUMBER soma direto, ADD_SCALAR soma uma fracao do valor base
 *                 (2.0 = triplica), MULTIPLY_SCALAR_1 multiplica o total
 * @param slot     onde o item precisa estar para valer: hand, mainhand, head, armor, any...
 */
public record Atributo(Attribute atributo, double valor,
                       AttributeModifier.Operation operacao, EquipmentSlotGroup slot) {
}
