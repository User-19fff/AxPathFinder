package com.artillexstudios.axpathfinder.item;

import com.artillexstudios.axpathfinder.AxPathFinder;
import com.artillexstudios.axpathfinder.managers.PathManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class WandItem {
    private final AxPathFinder plugin = AxPathFinder.getInstance();
    private final PathManager pathManager;

    public WandItem(PathManager pathManager) {
        this.pathManager = pathManager;
    }

    public ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();

        meta.setDisplayName("§d§lPath Wand");
        List<String> lore = new ArrayList<>();
        lore.add("§7Jobb klikk: Első pont kijelölése");
        lore.add("§7Shift + Jobb klikk: Második pont kijelölése");
        meta.setLore(lore);

        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        wand.setItemMeta(meta);
        return wand;
    }

    public boolean isWand(@NotNull ItemStack item) {
        if (item.getType() != Material.BLAZE_ROD) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.hasDisplayName() && meta.getDisplayName().equals("§d§lPath Wand");
    }

    public void handleFirstPoint(@NotNull Player player, @NotNull Location location) {
        pathManager.setWandFirstPoint(player, location);
        player.sendMessage("Első pont siker!");
    }

    public void handleSecondPoint(@NotNull Player player, @NotNull Location location, @NotNull String pathName) {
        boolean success = pathManager.processWandSecondPoint(player, pathName, location);

        if (success) player.sendMessage(pathName + "!");
    }
}
