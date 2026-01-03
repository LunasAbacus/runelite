package net.runelite.client.owo;

import net.runelite.api.Client;
import net.runelite.api.events.GameTick;

public abstract class OwoLogic {
    private OwoServer server;
    private Client client;

    public OwoLogic(OwoServer server, Client client) {
        this.server = server;
        this.client = client;
    }

    public void onGameTick(GameTick e) {}
}
