package net.runelite.client.owo;

import net.runelite.api.events.GameTick;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.plugins.owo.OwoPlugin;

public class SmeltGoldBracelet extends OwoLogic {
    private enum State {
        OPEN_BANK,
        DEPOSIT_ITEMS,
        WITHDRAW_SUPPLIES,
        SMELT_GOLD,
        CONFIRM_GOLD,
        SMELT_BRACELETS,
        CONFIRM_BRACELETS
    }

    private State state = State.OPEN_BANK;

    private int FURNACE_ID = 0;
    private int BANK_ID = 0;

    public SmeltGoldBracelet(OwoPlugin plugin) {
        super(plugin);

        Command command = InstructionFactory.createDefaultIdle();
        server.updateCommand(command);
        plugin.setDebugText("Loaded SmeltGoldBracelet");
    }

    @Override
    public void onGameTick(GameTick t) {
        super.onGameTick(t);
        updateState();
    }

    private void updateState() {
        // TODO Safety check for level confirm message
        // TODO Random pauses every x to y ticks?
        // TODO Random event detection?

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
                if (isAtFurnace()) {
                    state = State.CONFIRM_GOLD;
                } else {
                    actionClickFurnace();
                }
                break;

            case CONFIRM_GOLD:
                if (isSmeltingInProgress()) {
                    state = State.SMELT_BRACELETS;
                } else {
                    actionConfirmChoice();
                }
                break;

            case SMELT_BRACELETS:
                if (isAtFurnace()) {
                    state = State.CONFIRM_BRACELETS;
                } else {
                    actionClickFurnace();
                }
                break;

            case CONFIRM_BRACELETS:
                if (isSmeltingInProgress()) {
                    state = State.OPEN_BANK;
                } else {
                    actionConfirmChoice();
                }
                break;
        }
    }

    private void actionOpenBank() {
        // TODO Click bank/banker
    }

    private void actionDepositItems() {
        // TODO Shift click bracelet in inventory
    }

    private void actionWithdrawSupplies() {
        // TODO Shift click gold bar in bank
    }

    private void actionClickFurnace() {
        // TODO Click furnace
    }

    private void actionConfirmChoice() {
        // TODO Send space command
    }

    private boolean isBankOpen() {
        // TODO Check bank screen open
        return false;
    }

    private boolean isInventoryDeposited() {
        // TODO Check inventory only has mold
        return false;
    }

    private boolean hasSmeltingSuppliesInInventory() {
        // TODO Check inventory has supplies
        return false;
    }

    private boolean isAtFurnace() {
        // TODO Check that smelting box is open
        return false;
    }

    private boolean isSmeltingInProgress() {
        // TODO Check that not idle
        return false;
    }
}
