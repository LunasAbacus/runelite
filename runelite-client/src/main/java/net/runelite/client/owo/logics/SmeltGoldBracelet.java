package net.runelite.client.owo.logics;

import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.Point;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SmeltGoldBracelet extends OwoLogic {
    private enum State {
        OPEN_BANK,
        DEPOSIT_ITEMS,
        WITHDRAW_SUPPLIES,
        SMELT_GOLD,
        SMELT_BRACELETS,
        TRANSITION
    }

    private State state = State.OPEN_BANK;

    private final int FURNACE_ID = 16469;
    private final int BANK_ID = 10355;

    private static final int GOLD_ORE_ID = ItemID.GOLD_ORE;
    private static final int GOLD_BAR_ID = ItemID.GOLD_BAR;
    private static final int GOLD_BRACELET_ID = ItemID.JEWL_GOLD_BRACELET;
    private static final int BRACELET_MOULD_ID = ItemID.JEWL_BRACELET_MOULD;

    private static final List<ItemAmount> FINISHED_BAR_INVENTORY = List.of(new ItemAmount(GOLD_BAR_ID, 27));
    private static final List<ItemAmount> FINISHED_BRACELET_INVENTORY = List.of(new ItemAmount(GOLD_BRACELET_ID, 27));

    private final List<GameObject> activeFurnaces = new ArrayList<>();
    private final List<GameObject> activeBanks = new ArrayList<>();

    public SmeltGoldBracelet(OwoPlugin plugin) {
        super(plugin);

        Command command = InstructionFactory.createDefaultIdle();
        server.updateCommand(command);
        plugin.setDebugText("Loaded SmeltGoldBracelet");
    }

    @Override
    public void onGameTick(GameTick t) {
        super.onGameTick(t);
        if (isPerformingAction()) {
            idle(state.name());
            return;
        }
        updateState();
    }

    private void updateState() {
        if (checkForLevelUpMessage()) {
            actionHandleSafetyConfirm();
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
                    state = State.SMELT_GOLD;
                } else {
                    actionWithdrawSupplies();
                }
                break;

            case SMELT_GOLD:
                if (isSmeltingComplete(FINISHED_BAR_INVENTORY)) {
                    state = State.SMELT_BRACELETS;
                } else {
                    actionClickFurnace(9);
                }
                break;

            case SMELT_BRACELETS:
                if (isSmeltingComplete(FINISHED_BRACELET_INVENTORY)) {
                    state = State.OPEN_BANK;
                } else {
                    actionClickFurnace(1);
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
        Optional<Point> point = InventoryUtils.findInventoryItemPoint(client, inventoryItems, GOLD_BRACELET_ID);
        if (point.isEmpty()) {
            plugin.setDebugText("No bracelets found in inventory to deposit");
            server.updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }

        server.updateCommand(InstructionFactory.createClickCommand(point.get().getX(), point.get().getY()));
        plugin.setDebugText("Depositing item with shift-click");
        plugin.setDebugTargetPoint(point.get());
    }

    private void actionWithdrawSupplies() {
        Optional<Point> point = BankUtils.findBankItemPoint(client, GOLD_ORE_ID);
        if (point.isEmpty()) {
            plugin.setDebugText("No gold bars found in bank");
            server.updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }

        server.updateCommand(InstructionFactory.createClickCommand(point.get().getX(), point.get().getY(), 2));
        plugin.setDebugText("Withdrawing supplies with shift-click");
        plugin.setDebugTargetPoint(point.get());
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

            if (item.getId() != BRACELET_MOULD_ID) {
                return false;
            }
        }
        return true;
    }

    private boolean hasSmeltingSuppliesInInventory() {
        if (inventoryItems == null) {
            return false;
        }

        boolean hasMould = false;
        boolean hasGoldBars = false;
        for (Item item : inventoryItems) {
            if (item == null || item.getId() <= 0) {
                continue;
            }

            if (item.getId() == BRACELET_MOULD_ID) {
                hasMould = true;
            }
            if (item.getId() == GOLD_ORE_ID) {
                hasGoldBars = true;
            }
        }
        return hasMould && hasGoldBars;
    }


    private boolean isSmeltingComplete(List<ItemAmount> requiredItems) {
        return InventoryUtils.doesInventoryContainItems(inventoryItems, requiredItems);
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

    private void actionHandleSafetyConfirm() {
        server.updateCommand(InstructionFactory.createTypeCommand(" "));
        plugin.setDebugText("Handling safety confirm");
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
