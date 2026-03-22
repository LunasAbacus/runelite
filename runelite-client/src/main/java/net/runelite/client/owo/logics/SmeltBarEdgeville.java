package net.runelite.client.owo.logics;

import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.Point;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WorldChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.owo.utils.BankUtils;
import net.runelite.client.owo.utils.ItemAmount;
import net.runelite.client.owo.OwoLogic;
import net.runelite.client.owo.instruction.InstructionFactory;
import net.runelite.client.owo.utils.InventoryUtils;
import net.runelite.client.owo.utils.OwoUtils;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SmeltBarEdgeville extends OwoLogic<DummyState> {
    private enum State {
        OPEN_BANK,
        DEPOSIT_ITEMS,
        WITHDRAW_SUPPLIES,
        SMELT_BARS,
    }
    private State state = State.OPEN_BANK;

    public enum BarType {
        STEEL,
        MITHRIL
    }

    private final int oreId;
    private final int catalystId;
    private final int barId;

    private final List<ItemAmount> withdrawValidationInventory;
    private final List<ItemAmount> smeltValidationInventory;

    private final int FURNACE_ID = 16469;
    private final int BANK_ID = 10355;

    private final List<GameObject> activeFurnaces = new ArrayList<>();
    private final List<GameObject> activeBanks = new ArrayList<>();

    public SmeltBarEdgeville(OwoPlugin plugin, BarType barType) {
        super(plugin, DummyState.NO_OP);

        if (Objects.requireNonNull(barType) == BarType.MITHRIL) {
            this.oreId = ItemID.MITHRIL_ORE;
            this.catalystId = ItemID.COAL;
            this.barId = ItemID.MITHRIL_BAR;
            this.withdrawValidationInventory = List.of(ItemAmount.ofCount(ItemID.MITHRIL_ORE, 5), ItemAmount.ofCount(ItemID.COAL, 20));
            this.smeltValidationInventory = List.of(ItemAmount.ofCount(ItemID.MITHRIL_BAR, 5));
        } else {
            this.oreId = ItemID.IRON_ORE;
            this.catalystId = ItemID.COAL;
            this.barId = ItemID.STEEL_BAR;
            this.withdrawValidationInventory = List.of(ItemAmount.ofCount(ItemID.IRON_ORE, 9), ItemAmount.ofCount(ItemID.COAL, 18));
            this.smeltValidationInventory = List.of(ItemAmount.ofCount(ItemID.STEEL_BAR, 9));
        }

        server.updateCommand(InstructionFactory.createDefaultIdle());
        plugin.setDebugText("Loaded SmeltGoldBracelet");
    }

    @Override
    public void onGameTick(GameTick t) {
        super.onGameTick(t);
        if (isPerformingAction(2)) {
            idle(state.name());
            return;
        }
        updateState();
    }

    private void updateState() {
        if (checkForLevelUpMessage()) {
            actionLevelUpConfirm();
            return;
        }
//
//        if (shouldHandleRandomEvent()) {
//            actionHandleRandomEvent();
//            return;
//        }
//
//        if (shouldTakeRandomPause()) {
//            idle();
//            return;
//        }

        switch (state) {
            case OPEN_BANK:
                if (isBankOpen()) {
                    state = State.DEPOSIT_ITEMS;
                } else {
                    actionOpenBank();
                }
                break;

            case DEPOSIT_ITEMS:
                if (isInventoryDeposited()) {
                    state = State.WITHDRAW_SUPPLIES;
                } else {
                    actionDepositItems();
                }
                break;

            case WITHDRAW_SUPPLIES:
                if (hasSmeltingSuppliesInInventory()) {
                    state = State.SMELT_BARS;
                } else {
                    actionWithdrawSupplies();
                }
                break;

            case SMELT_BARS:
                if (isSmeltingComplete()) {
                    state = State.OPEN_BANK;
                } else {
                    actionClickFurnace(9);
                }
                break;
        }
    }

    private void actionOpenBank() {
        Optional<GameObject> closestBank = findClosestBank();
        if (closestBank.isEmpty()) {
            plugin.setDebugText("No bank object tracked");
            server.updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }

        Optional<Point> point = OwoUtils.getGameObjectClickPoint(closestBank.get());
        if (point.isEmpty()) {
            plugin.setDebugText("Bank is off screen");
            server.updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }

        server.updateCommand(InstructionFactory.createClickCommand(point.get().getX(), point.get().getY()));
        plugin.setDebugText("Opening bank");
        plugin.setDebugTargetPoint(point.get());
    }

    private void actionDepositItems() {
        Widget depositInv = client.getWidget(WidgetInfo.BANK_DEPOSIT_INVENTORY);
        if (depositInv != null && !depositInv.isHidden()) {
            Rectangle bounds = depositInv.getBounds();
            int cx = bounds.x + bounds.width / 2;
            int cy = bounds.y + bounds.height / 2;

            server.updateCommand(InstructionFactory.createClickCommand(cx, cy));
            plugin.setDebugText("Depositing inventory in bank");
        } else {
            plugin.setDebugText("Failed to find deposit button");
        }
    }

    private void actionWithdrawSupplies() {
        Optional<Point> point1 = BankUtils.findBankItemPoint(client, oreId);
        Optional<Point> point2 = BankUtils.findBankItemPoint(client, catalystId);
        if (point1.isEmpty() || point2.isEmpty()) {
            plugin.setDebugText("Missing items in bank");
            server.updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }

        server.updateCommand(
                new Command(List.of(
                        InstructionFactory.createClickInstruction(point1.get().getX(), point1.get().getY()),
                        InstructionFactory.createIdleByTicksInstruction(0, 1),
                        InstructionFactory.createShiftClickInstruction(point2.get().getX(), point2.get().getY()),
                        InstructionFactory.createIdleByTicksInstruction(1, 2)
                ))
        );
        plugin.setDebugText("Withdrawing supplies from bank");
    }

    private void actionClickFurnace(int tickWait) {
        Optional<GameObject> closestFurnace = findClosestFurnace();
        if (closestFurnace.isEmpty()) {
            plugin.setDebugText("No furnace object tracked");
            server.updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }

        Optional<Point> point = OwoUtils.getGameObjectClickPoint(closestFurnace.get());
        if (point.isEmpty()) {
            plugin.setDebugText("Furnace is off screen");
            server.updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }

        server.updateCommand(InstructionFactory.createClickAndConfirmCommand(point.get().getX(), point.get().getY(), tickWait));
        plugin.setDebugText("Clicking furnace - " + state.name());
        plugin.setDebugTargetPoint(point.get());
    }

    private boolean isBankOpen() {
        return client.getItemContainer(InventoryID.BANK) != null;
    }

    private boolean isInventoryDeposited() {
        if (inventoryItems == null) {
            return false;
        }

        for (Item item : inventoryItems) {
            if (item == null || item.getId() <= 0) {
                continue;
            }
            return false;
        }
        return true;
    }

    private boolean hasSmeltingSuppliesInInventory() {
        return InventoryUtils.doesInventoryContainItems(inventoryItems, withdrawValidationInventory);
    }


    private boolean isSmeltingComplete() {
        return InventoryUtils.doesInventoryContainItems(inventoryItems, smeltValidationInventory);
    }

    @Override
    public void onWorldChanged(WorldChanged worldChanged) {
        activeFurnaces.clear();
        activeBanks.clear();
    }

    @Override
    public void onGameObjectSpawned(GameObjectSpawned event) {
        GameObject object = event.getGameObject();
        if (object.getId() == FURNACE_ID) {
            activeFurnaces.add(object);
        }
        if (object.getId() == BANK_ID) {
            activeBanks.add(object);
        }
    }

    @Override
    public void onGameObjectDespawned(GameObjectDespawned event) {
        GameObject object = event.getGameObject();
        if (object.getId() == FURNACE_ID) {
            activeFurnaces.remove(object);
        }
        if (object.getId() == BANK_ID) {
            activeBanks.remove(object);
        }
    }

    private Optional<GameObject> findClosestBank() {
        if (client.getLocalPlayer() == null) {
            return Optional.empty();
        }
        return OwoUtils.findClosestGameObject(activeBanks, client.getLocalPlayer().getWorldLocation());
    }

    private Optional<GameObject> findClosestFurnace() {
        if (client.getLocalPlayer() == null) {
            return Optional.empty();
        }
        return OwoUtils.findClosestGameObject(activeFurnaces, client.getLocalPlayer().getWorldLocation());
    }

    private boolean checkForLevelUpMessage() {
        Widget levelUpWidget = client.getWidget(WidgetInfo.LEVEL_UP);
        return levelUpWidget != null && !levelUpWidget.isHidden();
    }

    private void actionLevelUpConfirm() {
        server.updateCommand(InstructionFactory.createTypeCommand(" "));
        plugin.setDebugText("Handling level up confirmation");
    }

    private boolean shouldTakeRandomPause() {
        // TODO Implement randomized pause cadence.
        return false;
    }

    private boolean shouldHandleRandomEvent() {
        // TODO Implement random event detection.
        return false;
    }

    private void actionHandleRandomEvent() {
        server.updateCommand(InstructionFactory.createDefaultIdle());
    }
}
