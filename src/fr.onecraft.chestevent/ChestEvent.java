package fr.onecraft.chestevent;

import fr.onecraft.chestevent.commands.CmdChestEvent;
import fr.onecraft.chestevent.core.helpers.Configs;
import fr.onecraft.chestevent.core.listeners.EventListener;
import fr.onecraft.chestevent.core.objects.Chest;
import fr.onecraft.chestevent.core.objects.Model;
import fr.onecraft.chestevent.core.objects.Pager;
import fr.onecraft.chestevent.tabCompleter.CompleterChestEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChestEvent extends JavaPlugin {
    public static String PREFIX = "§9Récompenses > §7";
    public static String ERROR = "§cRécompenses > §7";

    private Map<UUID, Pager> PAGER_CACHE = new HashMap<>();

    @Override
    public void onEnable() {
        // register event
        Bukkit.getPluginManager().registerEvents(new EventListener(this), this);

        // register command
        PluginCommand command = this.getCommand("chestevent");
        command.setExecutor(new CmdChestEvent(this));
        command.setTabCompleter(new CompleterChestEvent());

        // prepare files
        createDefaultFiles();
        removeExpiredChests();

        // load models
        Model.reloadAll(this);

        getLogger().info(this.getName() + " has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info(this.getName() + " has been disabled.");
    }

    public Map<UUID, Pager> getPagers() {
        return PAGER_CACHE;
    }

    private void createDefaultFiles() {
        if (Configs.get(this, "data") == null) {
            Configuration conf = new YamlConfiguration();
            conf.set("id", 0);
            Configs.save(this, conf, "data");
        }

        // noinspection ResultOfMethodCallIgnored
        new File(this.getDataFolder() + "/" + Model.DIRECTORY).mkdirs();
    }

    private void removeExpiredChests() {
        File[] files = new File(this.getDataFolder() + "/" + Chest.DIRECTORY).listFiles();
        if (files == null) return;

        // noinspection ResultOfMethodCallIgnored
        Arrays.stream(files)
                .filter(file -> file.getName().endsWith(".yml"))
                .filter(file -> {
                    Configuration conf = Configs.get(this, Chest.DIRECTORY, file.getName());
                    // delete the file if it has expired
                    return conf != null && System.currentTimeMillis() > conf.getLong("expire-date");
                })
                .forEach(File::delete);
    }
}
