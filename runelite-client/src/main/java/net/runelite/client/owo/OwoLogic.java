package net.runelite.client.owo;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.*;

public abstract class OwoLogic {
    protected final OwoServer server;
    protected final Client client;

    public OwoLogic(OwoServer server, Client client) {
        this.server = server;
        this.client = client;
    }

    public void startUp() { }

    public void  shutdown() { }

    private int ticksSinceCombat = 0;

    protected boolean isIdle() {
        return ticksSinceCombat >= 10;
    }

    public void onGameTick(GameTick e) {
        Player local = client.getLocalPlayer();
        if (local == null) {
            return;
        }

        boolean inCombat = local.getAnimation() != -1 || local.getInteracting() != null;

        if (inCombat) {
            ticksSinceCombat = 0;
        } else {
            ticksSinceCombat++;
        }
    }

    public void onGameObjectSpawned(GameObjectSpawned event) {}

    public void onGameObjectDespawned(GameObjectDespawned event) {}

    public void onNpcSpawned(NpcSpawned npcSpawned) {}

    public void onNpcDespawned(NpcDespawned npcDespawned) {}

    public void onAnimationChanged(AnimationChanged event) {}


}
