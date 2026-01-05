package net.runelite.client.owo;

import net.runelite.api.Client;
import net.runelite.api.events.*;

public abstract class OwoLogic {
    protected final OwoServer server;
    protected final Client client;

    public OwoLogic(OwoServer server, Client client) {
        this.server = server;
        this.client = client;
    }

    public void onGameTick(GameTick e) {}

    public void onGameObjectSpawned(GameObjectSpawned event) {}

    public void onGameObjectDespawned(GameObjectDespawned event) {}

    public void onNpcSpawned(NpcSpawned npcSpawned) {}

    public void onNpcDespawned(NpcDespawned npcDespawned) {}
}
