package com.eupolemo.itensdaniel.poder;

import com.eupolemo.itensdaniel.ItensDanielPlugin;
import com.eupolemo.itensdaniel.item.ItemCustomizado;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;

/** Executa os poderes. Um metodo por gatilho; quem chama e o listener. */
public final class Poderes {

    /** Marca as bolas de fogo que este plugin lancou e que nao podem destruir o terreno. */
    private final NamespacedKey chaveSemGrief;

    public Poderes(ItensDanielPlugin plugin) {
        this.chaveSemGrief = new NamespacedKey(plugin, "bola-sem-grief");
    }

    public NamespacedKey chaveSemGrief() {
        return chaveSemGrief;
    }

    /**
     * @return true se pelo menos um poder disparou. So nesse caso o cooldown comeca, para o
     *         jogador nao ficar esperando por um clique que nao fez nada.
     */
    public boolean cliqueDireito(Player jogador, ItemCustomizado item) {
        boolean disparou = false;
        for (Poder poder : item.poderesDe(Gatilho.CLIQUE_DIREITO)) {
            disparou |= switch (poder.tipo()) {
                case RAIO -> raio(jogador, poder);
                case TELEPORTE -> teleporte(jogador, poder);
                case BOLA_DE_FOGO -> bolaDeFogo(jogador, poder);
                case CURA -> cura(jogador, poder);
                case IMPULSO -> impulso(jogador, poder);
                default -> false;
            };
        }
        return disparou;
    }

    public void aoAcertar(Player atacante, LivingEntity alvo, double dano, ItemCustomizado item) {
        for (Poder poder : item.poderesDe(Gatilho.AO_ACERTAR)) {
            switch (poder.tipo()) {
                case ROUBO_DE_VIDA -> curar(atacante, dano * poder.fator());
                case CONGELAR -> alvo.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS, poder.duracaoTicks(), poder.nivel() - 1));
                default -> { }
            }
        }
    }

    public void aoSerAcertado(Player vitima, Entity atacante, ItemCustomizado item) {
        for (Poder poder : item.poderesDe(Gatilho.AO_SER_ACERTADO)) {
            if (poder.tipo() == TipoPoder.EMPURRAO) {
                empurrar(vitima, atacante, poder);
            }
        }
    }

    /** Aplica os efeitos continuos. Chamado pela task a cada segundo. */
    public void passivos(Player jogador, List<Poder> poderes, int duracaoTicks) {
        for (Poder poder : poderes) {
            switch (poder.tipo()) {
                case EFEITO -> {
                    if (poder.efeito() != null) {
                        // ambient=true e particulas=false para nao cobrir o jogador de particulas
                        // o tempo todo.
                        jogador.addPotionEffect(new PotionEffect(poder.efeito(), duracaoTicks,
                                poder.nivel() - 1, true, false, true));
                    }
                }
                case IMA -> puxarItens(jogador, poder.alcance());
                default -> { }
            }
        }
    }

    private void puxarItens(Player jogador, double raio) {
        for (Entity perto : jogador.getNearbyEntities(raio, raio, raio)) {
            if (!(perto instanceof Item item)) {
                continue;
            }
            // 32767 e o atraso de itens que ninguem pode pegar, como os de vitrine de plugin.
            if (item.getPickupDelay() >= 32767 || !item.getWorld().equals(jogador.getWorld())) {
                continue;
            }
            Vector puxao = jogador.getLocation().add(0, 0.5, 0).toVector()
                    .subtract(item.getLocation().toVector());
            if (puxao.lengthSquared() < 1.0e-4) {
                continue;
            }
            item.setVelocity(puxao.normalize().multiply(0.4));
        }
    }

    private boolean impulso(Player jogador, Poder poder) {
        Vector empurrao = jogador.getEyeLocation().getDirection().multiply(poder.fator());
        empurrao.setY(Math.max(empurrao.getY(), 0.35));
        jogador.setVelocity(empurrao);
        // Queda lenta para o pulo nao terminar em dano; some sozinha em 5 segundos.
        jogador.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, true, false, true));
        jogador.getWorld().playSound(jogador.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1f, 1.2f);
        return true;
    }

    private boolean raio(Player jogador, Poder poder) {
        Location alvo = mirar(jogador, poder.alcance());
        jogador.getWorld().strikeLightning(alvo);
        return true;
    }

    private boolean teleporte(Player jogador, Poder poder) {
        Block bloco = jogador.getTargetBlockExact((int) Math.max(1, poder.alcance()));
        if (bloco == null) {
            avisar(jogador, "Mire em um bloco dentro de " + (int) poder.alcance() + " blocos.");
            return false;
        }

        Location destino = bloco.getLocation().add(0.5, 1.0, 0.5);
        destino.setYaw(jogador.getLocation().getYaw());
        destino.setPitch(jogador.getLocation().getPitch());

        // Mirar no teto ou na lateral de uma parede daria um destino dentro de bloco solido.
        for (int tentativa = 0; tentativa < 4 && !cabe(destino); tentativa++) {
            destino.add(0, 1, 0);
        }
        if (!cabe(destino)) {
            avisar(jogador, "Nao ha espaco para aparecer ali.");
            return false;
        }

        jogador.getWorld().playSound(jogador.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        jogador.teleport(destino);
        jogador.getWorld().playSound(destino, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        return true;
    }

    private boolean bolaDeFogo(Player jogador, Poder poder) {
        Vector direcao = jogador.getEyeLocation().getDirection().multiply(poder.velocidade());
        Fireball bola = jogador.launchProjectile(Fireball.class, direcao);
        bola.setYield((float) poder.potencia());
        bola.setIsIncendiary(poder.incendiario());
        if (!poder.quebraBlocos()) {
            bola.getPersistentDataContainer().set(chaveSemGrief, PersistentDataType.BYTE, (byte) 1);
        }
        return true;
    }

    private boolean cura(Player jogador, Poder poder) {
        double max = maximoDeVida(jogador);
        if (jogador.getHealth() >= max) {
            avisar(jogador, "Sua vida ja esta cheia.");
            return false;
        }
        curar(jogador, poder.fator());
        jogador.getWorld().playSound(jogador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.6f);
        return true;
    }

    private void empurrar(Player vitima, Entity atacante, Poder poder) {
        Vector direcao = atacante.getLocation().toVector().subtract(vitima.getLocation().toVector());
        if (direcao.lengthSquared() < 1.0e-4) {
            // Atacante em cima da vitima: sem direcao horizontal util, joga para cima.
            direcao = new Vector(0, 1, 0);
        } else {
            direcao = direcao.normalize().multiply(poder.fator());
        }
        direcao.setY(Math.max(0.4, direcao.getY()));
        atacante.setVelocity(direcao);
    }

    private void curar(Player jogador, double pontos) {
        if (pontos <= 0) {
            return;
        }
        double max = maximoDeVida(jogador);
        jogador.setHealth(Math.min(max, jogador.getHealth() + pontos));
    }

    private static double maximoDeVida(Player jogador) {
        AttributeInstance atributo = jogador.getAttribute(Attribute.MAX_HEALTH);
        return atributo != null ? atributo.getValue() : 20.0;
    }

    /** @return o bloco mirado, ou o ponto no fim do alcance quando nao ha bloco na mira */
    private static Location mirar(Player jogador, double alcance) {
        Block bloco = jogador.getTargetBlockExact((int) Math.max(1, alcance));
        if (bloco != null) {
            return bloco.getLocation().add(0.5, 0, 0.5);
        }
        Location olhos = jogador.getEyeLocation();
        return olhos.add(olhos.getDirection().multiply(alcance));
    }

    private static boolean cabe(Location local) {
        return local.getBlock().isPassable() && local.clone().add(0, 1, 0).getBlock().isPassable();
    }

    private static void avisar(Player jogador, String texto) {
        jogador.sendActionBar(Component.text(texto, NamedTextColor.RED));
    }
}
