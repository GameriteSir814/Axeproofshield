package com.axeproofshield;

import io.papermc.paper.event.player.PlayerShieldDisableEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class AxeProofShield extends JavaPlugin implements Listener, TabExecutor {

    private NamespacedKey key;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        key = new NamespacedKey(this, "axe_proof");
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("aps").setExecutor(this);
        getCommand("aps").setTabCompleter(this);
    }

    private boolean isProof(ItemStack item) {
        if (item == null || item.getType() != Material.SHIELD) return false;
        ItemMeta m = item.getItemMeta();
        return m != null && m.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private void mark(ItemStack shield) {
        ItemMeta m = shield.getItemMeta();
        m.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        if (m instanceof Damageable d) {
            int max = getConfig().getInt("durability", 120);
            d.setMaxDamage(max);
            if (d.getDamage() >= max) d.setDamage(max - 1);
        }
        m.displayName(Component.text("Axe-Proof Shield", NamedTextColor.AQUA));
        shield.setItemMeta(m);
    }

    private boolean unmark(ItemStack shield) {
        ItemMeta m = shield.getItemMeta();
        if (m == null || !m.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) return false;
        m.getPersistentDataContainer().remove(key);
        if (m instanceof Damageable d) d.setMaxDamage(null);
        m.displayName(null);
        shield.setItemMeta(m);
        return true;
    }

    /** Axe hits normally disable a blocking shield for 5s - cancel that for marked shields. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDisable(PlayerShieldDisableEvent e) {
        Player p = e.getPlayer();
        ItemStack active = p.getActiveItem();
        if (isProof(active) || isProof(p.getInventory().getItemInMainHand()) || isProof(p.getInventory().getItemInOffHand())) {
            e.setCancelled(true);
        }
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player p)) { s.sendMessage("Players only."); return true; }
        if (!p.isOp()) { p.sendMessage(Component.text("Operators only.", NamedTextColor.RED)); return true; }
        if (a.length < 1) { p.sendMessage(Component.text("/aps give | add | remove", NamedTextColor.RED)); return true; }

        switch (a[0].toLowerCase()) {
            case "give" -> {
                ItemStack shield = new ItemStack(Material.SHIELD);
                mark(shield);
                p.getInventory().addItem(shield);
                p.sendMessage(Component.text("Gave an axe-proof shield (" + getConfig().getInt("durability", 120) + " durability).", NamedTextColor.GREEN));
            }
            case "add" -> {
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType() != Material.SHIELD) { p.sendMessage(Component.text("Hold a shield first.", NamedTextColor.RED)); return true; }
                mark(hand);
                p.getInventory().setItemInMainHand(hand); // write back or the changes are lost
                p.sendMessage(Component.text("Shield is now axe-proof with " + getConfig().getInt("durability", 120) + " durability.", NamedTextColor.GREEN));
            }
            case "remove" -> {
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType() == Material.SHIELD && unmark(hand)) {
                    p.getInventory().setItemInMainHand(hand);
                    p.sendMessage(Component.text("Shield is back to normal.", NamedTextColor.YELLOW));
                } else p.sendMessage(Component.text("That isn't an axe-proof shield.", NamedTextColor.RED));
            }
            default -> p.sendMessage(Component.text("/aps give | add | remove", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        return a.length == 1 ? List.of("give", "add", "remove").stream().filter(x -> x.startsWith(a[0].toLowerCase())).toList() : List.of();
    }
}
