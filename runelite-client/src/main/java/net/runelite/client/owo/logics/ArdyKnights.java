package net.runelite.client.owo.logics;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.owo.OwoLogic;
import net.runelite.client.owo.utils.*;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.util.List;
import java.util.Optional;

@Slf4j
public class ArdyKnights extends OwoLogic<ArdyKnights.State> {
    protected enum State {
        HURT,
        POUCHES_FULL,
        OUT_OF_FOOD, BANK_OPEN,
        KNIGHT_MOVED,
        READY_TO_PICKPOCKET,
        START,
        STUNNED
    }

    // World 302 is the usual world

    private static final int POUCH_ID = ItemID.PICKPOCKET_COIN_POUCH_KNIGHT;
    // 10355 is the top ardy bank, 10356 is the middle
    private static final List<Integer> bankIds = List.of(10355);
    private static final List<Integer> knightIds = List.of(NpcID.KNIGHT_OF_ARDOUGNE, NpcID.KNIGHT_OF_ARDOUGNE_F, NpcID.KNIGHT_OF_ARDOUGNE2);

    private static final int FOOD_ID = ItemID.JUG_WINE;

    public ArdyKnights(OwoPlugin plugin) {
        super(plugin, State.START, knightIds, bankIds);
        plugin.setDebugText("Loaded Ardy Knights");
//        postDiscordMessage("I am starting");
    }

    private int idleDuration = 0;
    @Override
    public void onGameTick(GameTick t) {
        super.onGameTick(t);

        if (idleDuration > 0) {
            idleDuration--;
            return;
        }

        int randomIdle = shouldRandomIdle(TaskIntensity.HIGH);
        if (randomIdle > 0) {
            log.debug("Taking a idle break for {} ticks", randomIdle);
            idleDuration = randomIdle;
            return;
        }

        if (state == State.START || state == State.STUNNED) {
            initialStateSelector();
        }
        updateState();
    }

    private void initialStateSelector() {
        // Has available food
        if (!InventoryUtils.doesInventoryContainItems(inventoryItems, List.of(ItemAmount.ofCount(FOOD_ID, 1)))) {
            setState(State.OUT_OF_FOOD);
            return;
        }

        // Emergency heal if very low health, otherwise eat during stuns
        if (PlayerUtils.needsHealingByPercent(client, 0.3)) {
            setState(State.HURT);
            return;
        }

        // Pouches are full
        if (playerModule.doesInventoryContainAllItems(List.of(ItemAmount.ofStack(POUCH_ID, 20)))) {
            setState(State.POUCHES_FULL);
            return;
        }

        if (!isKnightWithinBounds()) {
            setState(State.KNIGHT_MOVED);
            return;
        }

        if (playerModule.isStunned()) {
            if (playerModule.isHurt(20)) {
                setState(State.HURT);
            } else if (playerModule.doesInventoryContainItem(POUCH_ID)) {
                setState(State.POUCHES_FULL);
            } else {
                setState(State.STUNNED);
            }
            return;
        }

        setState(State.READY_TO_PICKPOCKET);
    }

    private void updateState() {
        switch (state) {
            case HURT:
                if (!playerModule.isHurt(20)) {
                    setState(State.START);
                    break;
                }
                if (canAct()) {
                    plugin.setDebugText("Eating food");
                    interactionManager.clickItemInInventory(FOOD_ID, "Wine");
                    debounce(2000);
                }
                break;
            case POUCHES_FULL:
                if (!playerModule.doesInventoryContainItem(POUCH_ID)) {
                    setState(State.START);
                    break;
                }
                if (canAct()) {
                    plugin.setDebugText("Emptying pouches");
                    interactionManager.clickItemInInventory(POUCH_ID, "Knight pouch");
                    debounce(2000);
                }
                break;
            case OUT_OF_FOOD:
                if (BankUtils.isBankInterfaceOpen(client)) {
                    setState(State.BANK_OPEN);
                    break;
                }
                if (canAct()) {
                    plugin.setDebugText("Banking for more food");
                    interactionManager.clickClosestGameObject(bankIds, "Bank");
                    debounce(2000);
                }
                break;
            case BANK_OPEN:
                if (!BankUtils.isBankInterfaceOpen(client)) {
                    setState(State.START);
                    break;
                }
                if (canAct()) {
                    interactionManager.performBankTransaction(List.of(ItemID.JUG_EMPTY), List.of(FOOD_ID));
                    debounce(2000);
                }
                break;
            case KNIGHT_MOVED:
                // Wait and hope resets in 120 seconds
                idleDuration = 200;
                setState(State.START);
                postDiscordMessage("I have lost the target. Knight has escaped containment.");
                break;
            case READY_TO_PICKPOCKET:
                plugin.setDebugText("Pickpocketing");
                interactionManager.clickClosestNpc(knightIds, "Knight");
                idleDuration = 1;
                setState(State.START);
                break;
            case STUNNED:
                plugin.setDebugText("Stunned");
                interactionManager.hoverClosestNpc(knightIds, "Knight");
                idleDuration = 8;
                setState(State.START);
                break;
        }
    }

    /*
        Bank bounds
        |-------|---
        | a | b |
        |---|===|---
        | c | d | B
        |-------|---
        a = 2654, 3287
        b = 2655, 3287
        c = 2654, 3286
        d = 2655, 3286
     */
    private boolean isKnightWithinBounds() {
        Optional<NPC> ardyKnight = interactionManager.findClosestNPC(knightIds);
        if (ardyKnight.isEmpty()) {
            return false;
        }

        WorldPoint knightPosition = ardyKnight.get().getWorldLocation();
        return knightPosition.getX() == 2655 && knightPosition.getY() == 3287;
    }
}
