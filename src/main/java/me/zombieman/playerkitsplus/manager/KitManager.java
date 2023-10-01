package me.zombieman.playerkitsplus.manager;

import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.jvm.Items;
import me.zombieman.playerkitsplus.PlayerKitsPlus;
import me.zombieman.playerkitsplus.utils.SoundUtil;
import me.zombieman.playerkitsplus.utils.TimerUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.stream.Collectors;

public class KitManager {

    public static boolean checkKit(String kit, PlayerKitsPlus plugin) {
        ConfigurationSection kitSection = plugin.getKitConfig().getConfigurationSection("kit");
        if (kitSection != null && kitSection.contains(kit)) return true;
        return false;
    }

    public static void savePlayerInventory(Player player, String kitName, int cooldown, PlayerKitsPlus plugin) {
        if (player.getInventory().isEmpty()) {
            player.sendMessage(ChatColor.RED + "You need to have items in your inventory to create a kit!");
            SoundUtil.sound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        ImmutableList.Builder<Object> itemsListBuilder = ImmutableList.builder();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) item = new ItemStack(Material.AIR);
            itemsListBuilder.add(item);
        }

        plugin.getKitConfig().set("kit." + kitName + ".items", itemsListBuilder.build());
        plugin.getKitConfig().set("kit." + kitName + ".cooldown", cooldown);
        plugin.saveKitConfig();
    }


    public static void setKit(PlayerKitsPlus plugin, String kitName, Player player) {
        FileConfiguration kitConfig = plugin.getKitConfig();

        int cooldown = kitConfig.getInt("kit." + kitName + ".cooldown");

        kitConfig.set("kit." + kitName, null);

        plugin.saveKitConfig();

        savePlayerInventory(player, kitName, cooldown, plugin);

        if (player.getInventory().isEmpty()) return;

        player.sendMessage(ChatColor.GREEN + "You edited the " + kitName + " kit.");

    }

    public static void removeKit(String kitName, PlayerKitsPlus plugin) {
        FileConfiguration kitConfig = plugin.getKitConfig();

        kitConfig.set("kit." + kitName, null);

        plugin.saveKitConfig();
    }

    public static void givePlayerKit(PlayerKitsPlus plugin, Player player, String kitName, boolean cooldown) {
        plugin.reloadKitConfig();
        FileConfiguration kitConfig = plugin.getKitConfig();
        if (!kitConfig.contains("kit." + kitName + ".items")) return;

        PlayerInventory inv = player.getInventory();
        // noinspection unchecked
        List<ItemStack> itemList = (List<ItemStack>) kitConfig.getList("kit." + kitName + ".items");
        if (itemList == null) return;

        int emptySlots = 0;

        // Checking for empty slots.

        for (int i = 0; i < inv.getSize() - 6; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                emptySlots++;
            }
        }

        if (PlayerKitsPlus.DEBUGMODE) {
            System.out.println("Empty slots: " + emptySlots);
            System.out.println("Items To Place: " + removeAir(itemList).size());

            for (ItemStack item : removeAir(itemList)) {
                System.out.println("Item Type: " + item.getType());
                System.out.println("Item Quantity: " + item.getAmount());
            }
        }

        // Checking if player has enough space for the items.

        if (emptySlots < removeAir(itemList).size()) {
            if (PlayerKitsPlus.DEBUGMODE) {
                System.out.println("You need " + removeAir(itemList).size() + " empty slots, but you have " + emptySlots);
            }
            player.sendMessage(ChatColor.YELLOW + "You don't have enough space in your inventory! Please empty out some items from your inventory.");
            SoundUtil.sound(player, Sound.ENTITY_VILLAGER_TRADE);
            return;
        }

        // Try to place the items where they should go
        List<ItemStack> couldNotBePlaced = new ArrayList<>();

        for (int i = 0; i < itemList.size(); i++) {
            ItemStack currItem = inv.getItem(i);
            if (currItem == null || currItem.getType() == Material.AIR) {
                if (PlayerKitsPlus.DEBUGMODE) {
                    System.out.println("Set " + itemList.get(i) + " to slot " + i + " of player's inv.");
                }
                inv.setItem(i, itemList.get(i));
            } else {
                if (PlayerKitsPlus.DEBUGMODE) {
                    System.out.println("Added " + itemList.get(i) + " to couldNotBePlaced list.");
                }
                couldNotBePlaced.add(itemList.get(i));
            }
        }

        // Adding items that couldn't be placed in the right slot.

        for (ItemStack item : removeAir(couldNotBePlaced)) {
            if (PlayerKitsPlus.DEBUGMODE) {
                System.out.println("Added " + item.getType() + " x " + item.getAmount() + " to " + player.getName());
            }
            inv.addItem(item);
        }

        player.sendActionBar(ChatColor.GREEN + "Equipped Kit '" + kitName + "'");
        SoundUtil.sound(player, Sound.ENTITY_ENDER_DRAGON_FLAP);
    }

    public static Collection<String> getKitNames(PlayerKitsPlus plugin) {
        ConfigurationSection kitSection = plugin.getKitConfig().getConfigurationSection("kit");
        if (kitSection == null) return new ArrayList<>();
        return kitSection.getKeys(false);
    }

    private static List<ItemStack> removeAir(List<ItemStack> itemList) {

        itemList = itemList.stream()
                .filter(item -> item != null && item.getType() != Material.AIR)
                .collect(Collectors.toList());

        return itemList;
    }

}
