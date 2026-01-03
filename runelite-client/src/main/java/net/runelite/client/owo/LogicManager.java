package net.runelite.client.owo;

import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;

public class LogicManager {
    private OwoLogic activeLogic;

    private final OwoServer server;
    private final Client client;

    public LogicManager(Client client) throws Exception {
        this.client = client;
        this.server = new OwoServer();
        setActiveLogic(LogicType.NO_OP);
    }

    public void setActiveLogic(LogicType type) {
        switch (type) {
            case NO_OP:
                this.activeLogic = new NoOpLogic(server, client);
                break;
            case GEMSTONE_CRAB:
                this.activeLogic = new GemstoneCrab(server, client);
                break;
        }
    }

    @Subscribe
    public void onGameTick(GameTick e) {
        activeLogic.onGameTick(e);
    }

}
