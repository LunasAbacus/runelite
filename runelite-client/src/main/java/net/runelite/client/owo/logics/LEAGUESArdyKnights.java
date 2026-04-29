package net.runelite.client.owo.logics;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.owo.OwoLogic;
import net.runelite.client.owo.utils.BankUtils;
import net.runelite.client.owo.utils.ItemAmount;
import net.runelite.client.owo.utils.PlayerUtils;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.util.List;
import java.util.Optional;

@Slf4j
public class LEAGUESArdyKnights extends OwoLogic<LEAGUESArdyKnights.State> {
    protected enum State {
        HURT,
        POUCHES_FULL,
        READY_TO_PICKPOCKET,
        START,
        STUNNED
    }

    // World 302 is the usual world
    private static final int POUCH_ID = ItemID.PICKPOCKET_COIN_POUCH_KNIGHT;
    private static final List<Integer> knightIds = List.of(NpcID.KNIGHT_OF_ARDOUGNE, NpcID.KNIGHT_OF_ARDOUGNE_F, NpcID.KNIGHT_OF_ARDOUGNE2);

    private static final int FOOD_ID = ItemID.SHARK;

    public LEAGUESArdyKnights(OwoPlugin plugin) {
        super(plugin, State.START, knightIds, List.of());
        plugin.setDebugText("Loaded LEAGUES Ardy Knights");
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
        // Emergency heal if very low health, otherwise eat during stuns
        if (PlayerUtils.needsHealingByPercent(client, 0.3)) {
            setState(State.HURT);
            return;
        }

        // Pouches are full
        if (playerModule.doesInventoryContainAllItems(List.of(ItemAmount.ofStack(POUCH_ID, 25)))) {
            setState(State.POUCHES_FULL);
            return;
        }

        if (playerModule.isStunned()) {
            if (playerModule.isHurt(20)) {
                setState(State.HURT);
            } else if (playerModule.doesInventoryContainAllItems(List.of(ItemAmount.ofStack(POUCH_ID, 10)))) {
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
            case READY_TO_PICKPOCKET:
                plugin.setDebugText("Pickpocketing");
                interactionManager.clickClosestNpc(knightIds, "Knight");
                setState(State.START);
                break;
            case STUNNED:
                plugin.setDebugText("Stunned");
                interactionManager.hoverClosestNpc(knightIds, "Knight");
                setState(State.START);
                break;
        }
    }
}
