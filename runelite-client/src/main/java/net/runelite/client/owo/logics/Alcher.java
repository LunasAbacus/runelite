package net.runelite.client.owo.logics;

import net.runelite.api.ItemComposition;
import net.runelite.api.Point;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.owo.OwoLogic;
import net.runelite.client.owo.instruction.InstructionFactory;
import net.runelite.client.owo.utils.InventoryUtils;
import net.runelite.client.owo.utils.MagicUtils;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.util.Optional;

public class Alcher extends OwoLogic<DummyState> {
    private int tickCount = 0;

    public Alcher(OwoPlugin plugin) {
        super(plugin, DummyState.NO_OP);

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
                Optional<Point> point = MagicUtils.findSpellPoint(client, InterfaceID.MagicSpellbook.HIGH_ALCHEMY);
                if (point.isPresent()) {
                    plugin.setDebugText("Click hi-alch spell");
                    server.updateCommand(InstructionFactory.createClickCommand(point.get()));
                    clickedItem = false;
                } else {
                    plugin.setDebugText("Could not find hi-alch spell");
                }

            }
        } else if (!clickedItem) {
            interactionManager.clickItemInInventory(ItemID.JEWL_GOLD_BRACELET, "Gold Bracelet");
        }
    }
}
