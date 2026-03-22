package net.runelite.client.owo.modules;

import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.client.owo.utils.InventoryUtils;
import net.runelite.client.owo.utils.ItemAmount;
import net.runelite.client.owo.utils.PlayerUtils;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.util.List;

public class PlayerModule {
    private final Client client;

    @Setter
    private Item[] inventoryItems = new Item[28];

    public PlayerModule(OwoPlugin plugin) {
        this.client = plugin.getClient();
    }

    private int stunnedTick = 0;
    public boolean isStunned() {
        return client.getTickCount() <= stunnedTick + 5;
    }

    public void reportStunned() {
        stunnedTick = client.getTickCount();
    }

    public boolean isHurt(final int hpLost) {
        return PlayerUtils.needsHealingByThreshold(client, hpLost);
    }

    public boolean doesInventoryContainItem(final int itemId) {
        return InventoryUtils.doesInventoryContainItems(inventoryItems, List.of(ItemAmount.ofCount(itemId, 1)));
    }

    public boolean doesInventoryContainAllItems(List<ItemAmount> items) {
        return InventoryUtils.doesInventoryContainItems(inventoryItems, items);
    }
}
