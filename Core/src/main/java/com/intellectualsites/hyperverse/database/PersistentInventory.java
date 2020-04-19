package com.intellectualsites.hyperverse.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

@DatabaseTable(tableName = "inventory")
public final class PersistentInventory {

    @DatabaseField(generatedId = true)
    private int id;
    @DatabaseField(uniqueCombo = true)
    private String worldUID;
    @DatabaseField(uniqueCombo = true)
    private String ownerUUID;
    @DatabaseField
    private String b64data;
    @DatabaseField
    private int heldSlot;

    public PersistentInventory() {

    }

    public PersistentInventory(@NotNull final String worldUID, @NotNull final PlayerInventory playerInventory) {
        this.worldUID = worldUID;
        this.b64data = serialise(playerInventory);
        this.ownerUUID = Objects.requireNonNull(playerInventory.getHolder()).getUniqueId().toString();
        this.heldSlot = playerInventory.getHeldItemSlot();
    }

    @NotNull public static PersistentInventory fromPlayer(Player player) {
        return new PersistentInventory(player.getWorld().getName(), player.getInventory());
    }

    /**
     * Serialize a {@link PlayerInventory} contents into a {@link Base64} string.
     * @param inventory The Inventory object to serialize.
     * @return Returns a Bas64 Encoded string which represents the state of this inventory's
     *         {@link Inventory#getStorageContents()} and {@link PlayerInventory#getArmorContents()}
     */
    @NotNull private static String serialise(@NotNull final PlayerInventory inventory) {
        YamlConfiguration configuration = new YamlConfiguration();
        int index = 0;
        for (final ItemStack itemStack : inventory.getStorageContents()) {
            configuration.set(String.valueOf(index++), itemStack);
        }
        //Set the special slots in a player inventory.
        configuration.set("offhand", inventory.getItemInOffHand());
        configuration.set("helmet", inventory.getHelmet());
        configuration.set("chestplate", inventory.getChestplate());
        configuration.set("leggings", inventory.getLeggings());
        configuration.set("boots", inventory.getBoots());
        return Base64.getEncoder().encodeToString(configuration.saveToString().getBytes()); //Convert to Base64
    }

    @NotNull public String getOwnerUUID() {
        return ownerUUID;
    }

    public int getHeldSlot() {
        return heldSlot;
    }

    @NotNull public String getWorldUID() {
        return worldUID;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Derserialize this object back into an {@link PlayerInventory}
     *
     * @return Returns a never-null PlayerInventory object based on the saved data.
     */
    @NotNull public PlayerInventory toInventory() {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(new String(Base64.getDecoder().decode(b64data.getBytes())));
        } catch (InvalidConfigurationException ex) {
            throw new IllegalStateException("Error Deserializing inventory", ex);
        }
        PlayerInventory inventory = (PlayerInventory) Bukkit.createInventory(ownerUUID == null ? null : Bukkit.getPlayer(UUID.fromString(ownerUUID)), InventoryType.PLAYER);
        for (String key : configuration.getKeys(false)) {
            ItemStack is = configuration.getItemStack(key);
            switch (key.toLowerCase()) {
                case "helmet": inventory.setHelmet(is); break;
                case "chestplate": inventory.setChestplate(is); break;
                case "leggings": inventory.setLeggings(is); break;
                case "boots": inventory.setBoots(is); break;
                case "offhand": inventory.setItemInOffHand(is); break;
                default:
                    inventory.setItem(Integer.parseInt(key), is);
            }
        }
        //inventory.setHeldItemSlot(heldSlot); //Set to the held slot when saved
        return inventory;
    }

    public void setToPlayer(@NotNull Player player) {
        PlayerInventory playerInventory = toInventory();
        PlayerInventory current = player.getInventory();
        current.setStorageContents(playerInventory.getStorageContents());
        //current.setHeldItemSlot(playerInventory.getHeldItemSlot());
        current.setArmorContents(playerInventory.getArmorContents());
    }
}
