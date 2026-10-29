package com.eupolemo.itensdaniel;

import com.eupolemo.itensdaniel.command.ItensCommand;
import com.eupolemo.itensdaniel.item.Cooldowns;
import com.eupolemo.itensdaniel.item.ItemManager;
import com.eupolemo.itensdaniel.listener.ItemListener;
import com.eupolemo.itensdaniel.listener.MenuListener;
import com.eupolemo.itensdaniel.poder.LuzDinamica;
import com.eupolemo.itensdaniel.poder.Mineracao;
import com.eupolemo.itensdaniel.poder.PassivosTask;
import com.eupolemo.itensdaniel.poder.Poderes;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class ItensDanielPlugin extends JavaPlugin {

    /**
     * Versao do formato de config.yml. Subir este numero faz o plugin arquivar a config antiga e
     * escrever a nova no proximo boot, porque o Bukkit nunca sobrescreve um config.yml existente.
     */
    private static final int VERSAO_CONFIG = 2;

    private ItemManager itemManager;
    private Poderes poderes;
    private Cooldowns cooldowns;
    private Mineracao mineracao;
    private LuzDinamica luzDinamica;
    private PassivosTask passivosTask;
    private BukkitTask tarefaPassivos;
    private BukkitTask tarefaLuz;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        migrarConfig();

        this.itemManager = new ItemManager(this);
        this.itemManager.carregar();
        this.poderes = new Poderes(this);
        this.cooldowns = new Cooldowns();
        this.mineracao = new Mineracao();
        this.luzDinamica = new LuzDinamica(this);
        this.passivosTask = new PassivosTask(this);

        getServer().getPluginManager().registerEvents(new ItemListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        this.tarefaPassivos = getServer().getScheduler().runTaskTimer(
                this, passivosTask, PassivosTask.PERIODO_TICKS, PassivosTask.PERIODO_TICKS);
        this.tarefaLuz = getServer().getScheduler().runTaskTimer(
                this, luzDinamica, LuzDinamica.PERIODO_TICKS, LuzDinamica.PERIODO_TICKS);

        PluginCommand comando = getCommand("itens");
        if (comando == null) {
            getLogger().severe("Comando 'itens' nao declarado no plugin.yml. Desabilitando.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        ItensCommand executor = new ItensCommand(this);
        comando.setExecutor(executor);
        comando.setTabCompleter(executor);

        getLogger().info("ItensDaniel ativo com " + itemManager.todos().size() + " item(ns).");
    }

    @Override
    public void onDisable() {
        if (tarefaPassivos != null) {
            tarefaPassivos.cancel();
        }
        if (tarefaLuz != null) {
            tarefaLuz.cancel();
        }
        if (passivosTask != null) {
            // Sem isso quem ganhou voo de um item continuaria voando com o plugin desligado.
            passivosTask.desligar();
        }
        if (luzDinamica != null) {
            luzDinamica.desligar();
        }
        if (cooldowns != null) {
            cooldowns.limpar();
        }
        getLogger().info("ItensDaniel desligado.");
    }

    /**
     * Arquiva um config.yml de formato antigo e gera um novo a partir do jar.
     *
     * @return true se houve migracao
     */
    private boolean migrarConfig() {
        int versao = getConfig().getInt("versao-config", 1);
        if (versao >= VERSAO_CONFIG) {
            return false;
        }

        File atual = new File(getDataFolder(), "config.yml");
        File backup = new File(getDataFolder(), "config-v" + versao + ".yml.bak");
        try {
            Files.move(atual.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            getLogger().severe("Nao consegui arquivar o config.yml antigo: " + e.getMessage()
                    + ". Apague plugins/ItensDaniel/ e reinicie.");
            return false;
        }

        saveDefaultConfig();
        reloadConfig();
        getLogger().warning("config.yml da versao " + versao + " foi arquivado como "
                + backup.getName() + " e substituido pelo formato " + VERSAO_CONFIG + ".");
        return true;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public Poderes getPoderes() {
        return poderes;
    }

    public Cooldowns getCooldowns() {
        return cooldowns;
    }

    public PassivosTask getPassivosTask() {
        return passivosTask;
    }

    public Mineracao getMineracao() {
        return mineracao;
    }

    public LuzDinamica getLuzDinamica() {
        return luzDinamica;
    }

    /** Recarrega config.yml do disco e reconstroi a lista de itens. */
    public void recarregar() {
        reloadConfig();
        itemManager.carregar();
        cooldowns.limpar();
    }
}
