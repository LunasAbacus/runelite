package net.runelite.client.owo.logics;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.owo.OwoLogic;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.owo.instruction.Instruction;
import net.runelite.client.owo.instruction.InstructionFactory;
import net.runelite.client.owo.modules.InteractionManager;
import net.runelite.client.owo.utils.*;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class LEAGUESAldarinGem extends OwoLogic<LEAGUESAldarinGem.State> {
    protected enum State {
        START,
        SHOP_OPEN_EMPTY_INVENTORY,
        SHOP_OPEN_CUT_INVENTORY,
        SHOP_OPEN_UNCUT_INVENTORY,
        CUTS_IN_INVENTORY,
        UNCUTS_IN_INVENTORY
    }

    private static final List<Integer> SHOP_KEEPER_IDS = List.of(NpcID.ALDARIN_GEM_STORE);
    private static final int CHISEL_ID = ItemID.CHISEL;
    private static final int UNCUT_RUBY_ID = ItemID.UNCUT_RUBY;
    private static final int CUT_RUBY_ID = ItemID.RUBY;

    private static final List<ItemAmount> UNCUT_INVENTORY = List.of(ItemAmount.ofCount(UNCUT_RUBY_ID, 26));
    private static final List<ItemAmount> CUT_INVENTORY = List.of(ItemAmount.ofCount(CUT_RUBY_ID, 26));

    public LEAGUESAldarinGem(OwoPlugin plugin) {
        super(plugin, State.START, SHOP_KEEPER_IDS, List.of());
        plugin.setDebugText("Loaded League Aldarin Gem");
    }

    private int idleDuration = 0;
    @Override
    public void onGameTick(GameTick t) {
        super.onGameTick(t);

        if (idleDuration > 0) {
            idleDuration--;
            return;
        }

        int randomIdle = shouldRandomIdle(TaskIntensity.MEDIUM);
        if (randomIdle > 0) {
            log.debug("Taking a idle break for {} ticks", randomIdle);
            idleDuration = randomIdle;
            return;
        }

        updateState();
    }


    private void updateState() {
        switch (state) {
            case START:
                if (!playerModule.doesInventoryContainItem(CHISEL_ID)) {
                    plugin.setDebugText("MISSING CHISEL!!!! Cannot start.");
                    break;
                }
                if (ShopUtils.isShopInterfaceOpen(client, InventoryID.ALDARIN_GEM_STORE)) {
                    if (playerModule.doesInventoryContainAllItems(CUT_INVENTORY)) {
                        setState(State.SHOP_OPEN_CUT_INVENTORY);
                    } else {
                        setState(State.SHOP_OPEN_EMPTY_INVENTORY);
                    }
                    break;
                }
                if (canAct()) {
                    plugin.setDebugText("Opening shop");
                    interactionManager.clickClosestNpc(SHOP_KEEPER_IDS, "Gemshop owner");
                    debounce(2000);
                }
                break;
            case SHOP_OPEN_EMPTY_INVENTORY:
                if (playerModule.doesInventoryContainAllItems(UNCUT_INVENTORY)) {
                    setState(State.SHOP_OPEN_UNCUT_INVENTORY);
                    break;
                }
                if (canAct()) {
                    plugin.setDebugText("Buying uncuts");
                    actionBuyGems();
                    debounce(6000);
                }
                break;
            case SHOP_OPEN_CUT_INVENTORY:
                if (!playerModule.doesInventoryContainItem(CUT_RUBY_ID)) {
                    setState(State.SHOP_OPEN_EMPTY_INVENTORY);
                    break;
                }
                if (canAct()) {
                    plugin.setDebugText("Selling cut gems");
                    actionSellGems();
                    debounce(2000);
                }
                break;
            case SHOP_OPEN_UNCUT_INVENTORY:
                if (!ShopUtils.isShopInterfaceOpen(client, InventoryID.ALDARIN_GEM_STORE)) {
                    setState(State.UNCUTS_IN_INVENTORY);
                    break;
                }
                if (canAct()) {
                    plugin.setDebugText("Closing shop");
                    interactionManager.closeInterface("Gemshop");
                    debounce(600);
                }
            case UNCUTS_IN_INVENTORY:
                if (playerModule.doesInventoryContainAllItems(CUT_INVENTORY)) {
                    setState(State.START);
                    break;
                }
                if (canAct()) {
                    plugin.setDebugText("Chiseling gems");
                    actionChiselGems();
                    debounce(33000);
                }
                break;
        }
    }

    private void actionBuyGems() {
        Optional<Point> point = ShopUtils.findShopItemPoint(client, UNCUT_RUBY_ID);

        if (point.isEmpty()) {
            plugin.setDebugText("Couldn't find item in shop");
            server.updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }

        int clicks = ThreadLocalRandom.current().nextInt(25, 33);
        server.updateCommand(InstructionFactory.createClickStormCommand(point.get(), clicks));
        plugin.setDebugText("Buying uncuts");
    }


    private void actionSellGems() {
        Optional<Point> point = InventoryUtils.findInventorySlotPoint(client, 27);

        if (point.isEmpty()) {
            plugin.setDebugText("Couldn't find inventory slot");
            log.debug("Couldn't find inventory slot");
            server.updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }

        int clicks = ThreadLocalRandom.current().nextInt(6, 8);
        server.updateCommand(InstructionFactory.createClickStormCommand(point.get(), clicks));
        plugin.setDebugText("Selling cuts");
    }

    private void actionChiselGems() {
        interactionManager.useItemOnAnotherAndConfirm(CHISEL_ID, UNCUT_RUBY_ID, "cut gems");
    }
}
