package net.runelite.client.owo.logics;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.owo.OwoLogic;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.owo.instruction.InstructionFactory;
import net.runelite.client.owo.utils.*;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class WineBuyer extends OwoLogic<WineBuyer.State> {
    protected enum State {
        SHOP_OPEN,
        ITEMS_BOUGHT,
        WALKING_TO_BANK,
        BANK_OPEN,
        ITEMS_DEPOSITED,
        WALKING_TO_SHOP
    }

    private final int BANK_ID = 57330;

    private final List<NPC> activeShopNPCs = new ArrayList<>();
    private final List<GameObject> activeBanks = new ArrayList<>();

    private final List<ItemAmount> wineInventory = List.of(new ItemAmount(ItemID.JUG_WINE, 27));

    public WineBuyer(OwoPlugin plugin) {
        super(plugin, State.WALKING_TO_SHOP);

        server.updateCommand(InstructionFactory.createDefaultIdle());
        plugin.setDebugText("Loaded Wine Buyer");
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

    @Override
    public void onWorldChanged(WorldChanged worldChanged) {
        activeShopNPCs.clear();
        activeBanks.clear();
    }

    @Override
    public void onGameObjectSpawned(GameObjectSpawned event) {
        GameObject object = event.getGameObject();
        if (object.getId() == BANK_ID) {
            activeBanks.add(object);
        }
    }

    @Override
    public void onGameObjectDespawned(GameObjectDespawned event) {
        GameObject object = event.getGameObject();
        if (object.getId() == BANK_ID) {
            activeBanks.remove(object);
        }
    }

    @Override
    public void onNpcSpawned(NpcSpawned npcSpawned) {
        NPC npc = npcSpawned.getNpc();
        if (npc.getId() == NpcID.AUBURN_BARTENDER) {
            activeShopNPCs.add(npc);
        }
    }

    @Override
    public void onNpcDespawned(NpcDespawned npcDespawned) {
        NPC npc = npcDespawned.getNpc();
        activeShopNPCs.remove(npc);
    }

    private void updateState() {
        switch (state) {
            case SHOP_OPEN:
                if (InventoryUtils.doesInventoryContainItems(inventoryItems, wineInventory)) {
                    setState(State.ITEMS_BOUGHT);
                    break;
                }
                if (canAct()) {
                    actionBuyItem();
                    debounce(1800);
                }
                break;
            case ITEMS_BOUGHT:
                if (isPerformingAction(2)) {
                    setState(State.WALKING_TO_BANK);
                    break;
                }
                if (canAct()) {
                    actionOpenBank();
                    debounce(800);
                }
                break;
            case WALKING_TO_BANK:
                if (BankUtils.isBankInterfaceOpen(client)) {
                    setState(State.BANK_OPEN);
                    break;
                }
                break;
            case BANK_OPEN:
                if (!BankUtils.isBankInterfaceOpen(client)) {
                    setState(State.ITEMS_DEPOSITED);
                    break;
                }
                if (canAct()) {
                    actionDepositItems();
                    debounce(800);
                }
                break;
            case ITEMS_DEPOSITED:
                if (isPerformingAction(2)) {
                    setState(State.WALKING_TO_SHOP);
                    break;
                }
                if (canAct()) {
                    actionWalkTowardsShop();
                    debounce(1400);
                }
                break;
            case WALKING_TO_SHOP:
                if (isShopInterfaceOpen()) {
                    setState(State.SHOP_OPEN);
                    break;
                }
                if (!isPerformingAction(4) && canAct()) {
                    actionOpenShop();
                    debounce(1400);
                }
                break;
        }
    }

    private boolean isShopInterfaceOpen() {
        return ShopUtils.isShopInterfaceOpen(client, InventoryID.PUB_AUBURNVALE);
    }

    private void actionOpenShop() {
        ShopUtils.openShop(plugin, activeShopNPCs);
    }

    private void actionBuyItem() {
        Optional<Point> point = ShopUtils.findShopItemPoint(client, ItemID.JUG_WINE);

        if (point.isEmpty()) {
            plugin.setDebugText("Couldn't find item in shop");
            server.updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }

        server.updateCommand(
                new Command(List.of(
                        InstructionFactory.createClickInstruction(point.get().getX(), point.get().getY()),
                        InstructionFactory.createClickInstruction(point.get().getX(), point.get().getY()),
                        InstructionFactory.createClickInstruction(point.get().getX(), point.get().getY())
                ))
        );
        plugin.setDebugText("Buying item");
        plugin.setDebugTargetPoint(point.get());
    }

    private void actionWalkTowardsShop() {
        int x = 1394 + ThreadLocalRandom.current().nextInt(-1, 2);
        int y = 3354 + ThreadLocalRandom.current().nextInt(-1, 2);
        WorldUtils.clickTile(plugin, x, y);
    }

    private void actionOpenBank() {
        BankUtils.clickClosestBank(plugin, activeBanks);
    }

    private void actionDepositItems() {
        BankUtils.depositItemInBank(plugin, inventoryItems, ItemID.JUG_WINE);
    }
}
