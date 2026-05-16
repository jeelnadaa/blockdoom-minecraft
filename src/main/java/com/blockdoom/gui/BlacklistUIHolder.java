package com.blockdoom.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class BlacklistUIHolder implements InventoryHolder {
    private Inventory inventory;
    private final int page;

    public BlacklistUIHolder(int page) {
        this.page = page;
    }

    public int getPage() { return page; }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
