package net.runelite.client.owo.logics;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.owo.OwoLogic;
import net.runelite.client.owo.utils.BankUtils;
import net.runelite.client.owo.utils.ItemAmount;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.util.List;

@Slf4j
public class ShiloSmelter extends OwoLogic<ShiloSmelter.State> {
    protected enum State {
        START,
        BANK_OPEN,
        BANK_OPEN_INVENTORY,
        BANK_OPEN_ITEMS_WITHDRAWN,
        WALKING_TO_FURNACE,
        FURNACE_OPEN,
        SMELTING
    }

    private static final List<Integer> BANKER_IDS = List.of(NpcID.SHILOBANKER);
    private static final List<Integer> FURNACE_IDS = List.of(29662);
    private static final int CATALYST_ID = ItemID.COAL;
    private static final int ORE_ID = ItemID.IRON_ORE;
    private static final int BAR_ID = ItemID.STEEL_BAR;

    private static final List<ItemAmount> ORE_INVENTORY = List.of(
            ItemAmount.ofCount(CATALYST_ID, 18),
            ItemAmount.ofCount(ORE_ID, 9)
    );
    private static final List<ItemAmount> BAR_INVENTORY = List.of(ItemAmount.ofCount(BAR_ID, 9));

    public ShiloSmelter(OwoPlugin plugin) {
        super(plugin, State.START, BANKER_IDS, FURNACE_IDS);
        plugin.setDebugText("Loaded Shilo Smelter");
    }

    private int idleDuration = 0;
    @Override
    public void onGameTick(GameTick t) {
        super.onGameTick(t);

        if (idleDuration > 0) {
            idleDuration--;
            return;
        }

        int randomIdle = shouldRandomIdle(TaskIntensity.LOW);
        if (randomIdle > 0) {
            log.debug("Taking a idle break for {} ticks", randomIdle);
            idleDuration = randomIdle;
            return;
        }

        updateState();
    }

    // TODO Refactor this with registered state objects. To avoid missing breaks etc

    private void updateState() {
        switch (state) {
            case START:
                if (BankUtils.isBankInterfaceOpen(client)) {
                    setState(State.BANK_OPEN_INVENTORY);
                    break;
                }
                if (canAct()) {
                    plugin.setDebugText("Opening bank");
                    interactionManager.clickClosestNpc(BANKER_IDS, "Banker");
                    debounce(10000);
                }
                break;
            case BANK_OPEN_INVENTORY:
                if (!BankUtils.isBankInterfaceOpen(client) && playerModule.doesInventoryContainAllItems(ORE_INVENTORY)) {
                    setState(State.BANK_OPEN_ITEMS_WITHDRAWN);
                    break;
                }
                if (canAct()) {
                    plugin.setDebugText("Withdrawing inventory");
                    interactionManager.performBankTransaction(List.of(BAR_ID), List.of(CATALYST_ID, ORE_ID));
                    debounce(5000);
                }
                break;
            case BANK_OPEN_ITEMS_WITHDRAWN:
                if (interactionManager.isWidgetVisible(InterfaceID.Skillmulti.QUANTITIES)) {
                    setState(State.FURNACE_OPEN);
                    break;
                }
                if (canAct()) {
                    plugin.setDebugText("Opening furnace");
                    interactionManager.clickClosestGameObject(FURNACE_IDS, "Furnace");
                    debounce(10000);
                }
                break;
            case FURNACE_OPEN:
                if (!interactionManager.isWidgetVisible(InterfaceID.Skillmulti.QUANTITIES)) {
                    setState(State.SMELTING);
                    break;
                }
                if (canAct()) {
                    plugin.setDebugText("Smelting bars");
                    interactionManager.confirmSelection();
                    debounce(2000);
                }
                break;
            case SMELTING:
                if (playerModule.doesInventoryContainAllItems(BAR_INVENTORY)) {
                    setState(State.START);
                    break;
                }
                break;
        }
    }
}
