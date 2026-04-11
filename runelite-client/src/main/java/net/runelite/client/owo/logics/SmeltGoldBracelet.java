package net.runelite.client.owo.logics;

import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.owo.utils.BankUtils;
import net.runelite.client.owo.utils.ItemAmount;
import net.runelite.client.owo.OwoLogic;
import net.runelite.client.owo.instruction.InstructionFactory;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.util.List;

public class SmeltGoldBracelet extends OwoLogic<SmeltGoldBracelet.State> {
    protected enum State {
        OPEN_BANK,
        BANK_OPEN,
        DEPOSIT_ITEMS,
        WITHDRAW_SUPPLIES,
        SMELT_GOLD,
        SMELT_BRACELETS,
        TRANSITION
    }

    private State state = State.OPEN_BANK;

    private static final int FURNACE_ID = 16469;
    private static final int BANK_ID = 10355;

    private static final int GOLD_ORE_ID = ItemID.GOLD_ORE;
    private static final int GOLD_BAR_ID = ItemID.GOLD_BAR;
    private static final int GOLD_BRACELET_ID = ItemID.JEWL_GOLD_BRACELET;

    private static final List<ItemAmount> FINISHED_BAR_INVENTORY = List.of(ItemAmount.ofCount(GOLD_BAR_ID, 27));
    private static final List<ItemAmount> FINISHED_BRACELET_INVENTORY = List.of(ItemAmount.ofCount(GOLD_BRACELET_ID, 27));

    public SmeltGoldBracelet(OwoPlugin plugin) {
        super(plugin, State.OPEN_BANK, List.of(), List.of(FURNACE_ID, BANK_ID));
        plugin.setDebugText("Loaded SmeltGoldBracelet");
    }

    // TODO Use built in state management
    private int idleDuration = 0;
    @Override
    public void onGameTick(GameTick t) {
        super.onGameTick(t);

        if (idleDuration > 0) {
            idleDuration--;
            return;
        }

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

        switch (state) {
            // TODO Replace with bank transaction
            case OPEN_BANK:
                if (BankUtils.isBankInterfaceOpen(client)) {
                    state = State.DEPOSIT_ITEMS;
                } else {
                    interactionManager.clickClosestGameObject(List.of(BANK_ID), "Bank");
                }
                break;

            case BANK_OPEN:
                if (!BankUtils.isBankInterfaceOpen(client)) {
                    setState(State.SMELT_GOLD);
                    break;
                }
                if (canAct()) {
                    interactionManager.performBankTransaction(List.of(GOLD_BRACELET_ID), List.of(GOLD_ORE_ID));
                    debounce(2000);
                }
                break;

            case SMELT_GOLD:
                if (playerModule.doesInventoryContainAllItems(FINISHED_BAR_INVENTORY)) {
                    state = State.SMELT_BRACELETS;
                } else {
                    interactionManager.clickClosestGameObject(List.of(FURNACE_ID), "Furnace");
                    idleDuration = 9;
                }
                break;

            case SMELT_BRACELETS:
                if (playerModule.doesInventoryContainAllItems(FINISHED_BRACELET_INVENTORY)) {
                    state = State.OPEN_BANK;
                } else {
                    interactionManager.clickClosestGameObject(List.of(FURNACE_ID), "Furnace");
                    idleDuration = 2;
                }
                break;
        }
    }

    // TODO Move to playermodule
    private boolean checkForLevelUpMessage() {
        Widget levelUpWidget = client.getWidget(WidgetInfo.LEVEL_UP);
        return levelUpWidget != null && !levelUpWidget.isHidden();
    }

    private void actionHandleSafetyConfirm() {
        server.updateCommand(InstructionFactory.createTypeCommand(" "));
        plugin.setDebugText("Handling safety confirm");
    }

}
