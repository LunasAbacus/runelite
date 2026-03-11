package net.runelite.client.owo.logics;

import net.runelite.api.ItemComposition;
import net.runelite.api.Point;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.owo.OwoLogic;
import net.runelite.client.owo.instruction.InstructionFactory;
import net.runelite.client.owo.utils.InventoryUtils;
import net.runelite.client.owo.utils.MagicUtils;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.util.Optional;

public class Alcher extends OwoLogic {
    private int tickCount = 0;

    public Alcher(OwoPlugin plugin) {
        super(plugin);

        server.updateCommand(InstructionFactory.createDefaultIdle());
        plugin.setDebugText("Loaded Alcher");
    }

    @Override
    public void onGameTick(GameTick t) {
        super.onGameTick(t);
        tickCount = (tickCount + 1) % 5;
        updateState();
    }

    private boolean clickedItem = false;
    private void updateState() {
        // TODO Add in delays and variances

        // Handle Spell Cast
        if (tickCount == 0) {
            if (!MagicUtils.isSpellbookOpen(client)) {
                plugin.setDebugText("Opening spellbook");
                server.updateCommand(InstructionFactory.createTypeCommand("{F3}"));
            } else {
                plugin.setDebugText("Click hi-alch spell");
                Optional<Point> point = MagicUtils.findSpellPoint(client);
                point.ifPresent(p -> server.updateCommand(InstructionFactory.createClickCommand(p)));
                clickedItem = false;
            }
        } else if (!clickedItem) {
            // Click point
            int itemId = ItemID.JEWL_GOLD_BRACELET;
            ItemComposition item = plugin.getItemManager().getItemComposition(itemId);
            int notedId = item.getLinkedNoteId();
            Optional<Point> point = InventoryUtils.findInventoryItemPoint(client, inventoryItems, notedId);
            if (point.isPresent()) {
                plugin.setDebugText("Clicking alchable item");
                server.updateCommand(InstructionFactory.createClickCommand(point.get()));
                clickedItem = true;
            } else {
                plugin.setDebugText("Could not find alchable item");
            }
        }
    }
}
