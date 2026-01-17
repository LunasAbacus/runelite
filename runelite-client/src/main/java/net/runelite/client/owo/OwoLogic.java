package net.runelite.client.owo;

import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.plugins.owo.OwoPlugin;

public abstract class OwoLogic {
    protected final OwoServer server;
    protected final Client client;
    protected final OwoPlugin plugin;

    protected Item[] inventoryItems = new Item[28];

    public OwoLogic(OwoPlugin plugin) {
        this.server = plugin.getServer();
        this.client = plugin.getClient();
        this.plugin = plugin;
    }

    public void startUp() { }

    public void  shutdown() { }

    private int ticksSinceAction = 0;

    protected boolean isPerformingAction() {
        return ticksSinceAction < 10;
    }

    protected void idle() {
        // Wait for action to complete
        Command command = InstructionFactory.createDefaultIdle();
        server.updateCommand(command);
        plugin.setDebugText("Performing action");
    }

    public void onGameTick(GameTick e) {
        Player local = client.getLocalPlayer();
        if (local == null) {
            return;
        }

        boolean performingAction = local.getAnimation() != -1 || local.getInteracting() != null;

        if (performingAction) {
            ticksSinceAction = 0;
        } else {
            ticksSinceAction++;
        }
    }

    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() != InventoryID.INV) {
            return;
        }

        inventoryItems = event.getItemContainer().getItems();
    }

    public void onGameObjectSpawned(GameObjectSpawned event) {}

    public void onGameObjectDespawned(GameObjectDespawned event) {}

    public void onNpcSpawned(NpcSpawned npcSpawned) {}

    public void onNpcDespawned(NpcDespawned npcDespawned) {}

    public void onAnimationChanged(AnimationChanged event) {}


}
