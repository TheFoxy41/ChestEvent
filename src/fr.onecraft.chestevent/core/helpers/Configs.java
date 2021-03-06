package fr.onecraft.chestevent.core.helpers;

import fr.onecraft.chestevent.core.objects.ChestItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

public class Configs {

    public static Configuration get(JavaPlugin plugin, String folder, String filename) {
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            File file = new File(
                    plugin.getDataFolder() + (folder.isEmpty() ? "" : "/") + folder,
                    filename + (filename.endsWith(".yml") ? "" : ".yml")
            );
            if (!file.exists()) return null;
            FileInputStream fileinputstream = new FileInputStream(file);
            configuration.load(new InputStreamReader(fileinputstream, Charset.forName("UTF-8")));
            return configuration;
        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Configuration get(JavaPlugin plugin, String filename) {
        return get(plugin, "", filename);
    }

    public static Configuration get(JavaPlugin plugin, String folder, int filename) {
        return get(plugin, folder, String.valueOf(filename));
    }

    /**
     * @noinspection WeakerAccess
     */
    public static boolean save(JavaPlugin plugin, Configuration config, String folder, String filename) {
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            for (String key : config.getKeys(false)) configuration.set(key, config.get(key));

            configuration.save(new File(
                    plugin.getDataFolder() + (folder.isEmpty() ? "" : "/") + folder,
                    filename + (filename.endsWith(".yml") ? "" : ".yml")
            ));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean save(JavaPlugin plugin, Configuration config, String filename) {
        return save(plugin, config, "", filename);
    }

    public static boolean save(JavaPlugin plugin, Configuration config, String folder, int configName) {
        return save(plugin, config, folder, String.valueOf(configName));
    }

    public static void dumpItems(Configuration conf, List<ChestItem> items) {
        conf.set("items", null);

        int slot = 1;
        for (ChestItem chestItem : items) {
            ConfigurationSection section = conf.createSection("items.slot" + slot);
            ItemStack item = chestItem.getOriginal();
            ItemMeta meta = item.getItemMeta();

            // save item type and metadata
            String itemType = item.getType().toString();
            // noinspection deprecation
            short metadata = item.getData().getData();
            section.set("type", itemType + ":" + metadata);

            // save item amount if bigger than 1
            if (item.getAmount() > 1) {
                section.set("amount", item.getAmount());
            }

            // save item name if any
            if (meta != null && meta.getDisplayName() != null) {
                section.set("name", meta.getDisplayName().replace("§", "&").substring(2));
            }

            // save lore if any
            List<String> lore = meta == null || meta.getLore() == null
                    ? null
                    : meta.getLore().stream()
                    .map(string -> string.replace("§", "&").substring(2))
                    .collect(Collectors.toList());

            if (lore != null && !lore.isEmpty()) {
                section.set("lore", lore);
            }

            // translate enchants to string
            List<String> enchants = meta == null
                    ? null
                    : meta.getEnchants().entrySet().stream()
                    .map(entry -> entry.getKey().getName() + ":" + entry.getValue())
                    .collect(Collectors.toList());

            // save enchants if exists
            if (enchants != null && !enchants.isEmpty()) {
                section.set("enchant", enchants);
            }

            conf.set("items.slot" + slot, section);
            slot++;
        }
    }

    public static List<ChestItem> loadItems(ConfigurationSection conf) {
        return conf.getConfigurationSection("items").getKeys(false).stream().map((item) -> {
            // get section
            ConfigurationSection slot = conf.getConfigurationSection("items." + item);

            String[] itemData = slot.getString("type").split(":");

            // get item type
            Material type = Material.valueOf(itemData[0]);

            // get metadata
            short metadata = itemData.length > 1 ? Short.parseShort(itemData[1]) : 0;

            // get item amount (1 or more)
            int amount = Math.max(1, slot.getInt("amount"));

            // create item stack
            ItemStack itemStack = new ItemStack(type, amount, metadata);
            ItemMeta meta = itemStack.getItemMeta();

            // set name
            String name = slot.getString("name");
            if (name != null) {
                meta.setDisplayName("§f" + ChatColor.translateAlternateColorCodes('&', name));
            }

            // add lore
            meta.setLore(
                    slot.getStringList("lore")
                            .stream()
                            .map(lore -> "§7" + ChatColor.translateAlternateColorCodes('&', lore))
                            .collect(Collectors.toList())
            );

            // add enchants
            slot.getStringList("enchant")
                    .forEach(enchant -> meta.addEnchant(
                            Enchantment.getByName(enchant.split(":")[0]),
                            Integer.parseInt(enchant.split(":")[1]),
                            true
                    ));

            // update meta
            itemStack.setItemMeta(meta);

            return new ChestItem(itemStack);
        }).collect(Collectors.toList());
    }
}
