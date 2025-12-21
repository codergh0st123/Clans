package org.ghost.clansystem.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.ghost.clansystem.ClanSystem;
import java.util.ArrayList;
import java.util.List;

public class PacketInterceptor {

    private final ClanSystem plugin;

    public PacketInterceptor(ClanSystem plugin) {
        this.plugin = plugin;
        registerPacketListener();
    }

    private void registerPacketListener() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(plugin, PacketType.Play.Server.ENTITY_EQUIPMENT) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        Player observer = event.getPlayer();
                        PacketContainer packet = event.getPacket();
                        Entity entity = packet.getEntityModifier(event).read(0);

                        if (!(entity instanceof Player target)) {
                            return;
                        }

                        if (observer.getEntityId() == target.getEntityId()) {
                            return;
                        }
                        Integer color = PacketInterceptor.this.plugin.getClanManager().getPlayerGlow(target.getUniqueId());
                        if (color == null) {
                            return;
                        }

                        String targetClan = PacketInterceptor.this.plugin.getClanManager().getPlayerClan(target.getUniqueId());
                        String observerClan = PacketInterceptor.this.plugin.getClanManager().getPlayerClan(observer.getUniqueId());

                        if (observerClan == null || !observerClan.equals(targetClan)) {
                            return;
                        }

                        List<Pair<EnumWrappers.ItemSlot, ItemStack>> slotStackPairs = packet.getSlotStackPairLists().read(0);
                        List<Pair<EnumWrappers.ItemSlot, ItemStack>> newPairs = new ArrayList<>();
                        boolean modified = false;

                        for (Pair<EnumWrappers.ItemSlot, ItemStack> pair : slotStackPairs) {
                            EnumWrappers.ItemSlot slot = pair.getFirst();
                            if (isArmorSlot(slot)) {
                                ItemStack coloredArmor = createColoredArmor(slot, color);
                                newPairs.add(new Pair<>(slot, coloredArmor));
                                modified = true;
                            } else {
                                newPairs.add(pair);
                            }
                        }

                        if (modified) {
                            packet.getSlotStackPairLists().write(0, newPairs);
                        }
                    }
                }
        );
    }

    private boolean isArmorSlot(EnumWrappers.ItemSlot slot) {
        return slot == EnumWrappers.ItemSlot.HEAD ||
                slot == EnumWrappers.ItemSlot.CHEST ||
                slot == EnumWrappers.ItemSlot.LEGS ||
                slot == EnumWrappers.ItemSlot.FEET;
    }

    private ItemStack createColoredArmor(EnumWrappers.ItemSlot slot, int color) {
        Material mat;
        switch (slot) {
            case HEAD: mat = Material.LEATHER_HELMET; break;
            case CHEST: mat = Material.LEATHER_CHESTPLATE; break;
            case LEGS: mat = Material.LEATHER_LEGGINGS; break;
            case FEET: mat = Material.LEATHER_BOOTS; break;
            default: return new ItemStack(Material.AIR);
        }
        ItemStack item = new ItemStack(mat);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(Color.fromRGB(color));
            item.setItemMeta(meta);
        }
        return item;
    }
}
