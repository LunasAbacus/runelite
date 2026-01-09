package net.runelite.client.owo;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.events.*;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.owo.instruction.Instruction;
import net.runelite.client.owo.instruction.InstructionParameters;
import net.runelite.client.owo.instruction.InstructionType;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.awt.*;
import java.util.List;

@Slf4j
public class GemstoneCrab extends OwoLogic {

    // Tunnel game object ID
    private static final int TUNNEL_OBJECT_ID = 57631;

    // Gemstone Crab boss NPC ID
    private static final int GEMSTONE_CRAB_ID = 14779;

    enum GemstoneCrabState {
        ATTACK, TUNNEL, SEARCH, ACTION
    }

    private boolean isCrabKilled = false;

    private NPC gemstoneCrab = null;
    private GameObject tunnel = null;

    private final OwoPlugin plugin;

    public GemstoneCrab(OwoServer server, Client client, OwoPlugin plugin) {
        super(server, client);
        this.plugin = plugin;

        Command command = new Command(List.of(new Instruction(InstructionType.IDLE, new InstructionParameters(0, 0, 0, 500, 1000))));
        server.updateCommand(command);

        plugin.setDebugText("Loaded GemstoneCrab");
    }

    @Override
    public void onGameTick(GameTick t) {
        super.onGameTick(t);

        // Determine state
        GemstoneCrabState state;
        if (!isIdle()) {
            // Wait for action to complete
            state = GemstoneCrabState.ACTION;
            Command command = new Command(List.of(new Instruction(InstructionType.IDLE,
                    new InstructionParameters(0, 0, 0, 1000, 2000))));
            server.updateCommand(command);
            plugin.setDebugState(state.name());
            return;
        }

        if (gemstoneCrab != null) {
            state = GemstoneCrabState.ATTACK;
        } else if (isCrabKilled) {
            state = GemstoneCrabState.TUNNEL;
        } else {
            state = GemstoneCrabState.SEARCH;
        }
        plugin.setDebugState(state.name());

        switch (state) {
            case ATTACK: handleAttackingState(); break;
            case SEARCH: handleSearchingState(); break;
            case TUNNEL: handleTunnelingState(); break;
        }
    }

    /**
     * Go through tunnel
     * Next: Search for crab
     */
    private void handleTunnelingState() {
        // TODO Nate what to do if tunnel is outside view?
        if (tunnel != null) {
            // Click on the tunnel
            Shape clickbox = tunnel.getClickbox();
            if (clickbox == null) {
                return;
            }
            Rectangle bounds = clickbox.getBounds();
            int centerX = bounds.x + bounds.width / 2;
            int centerY = bounds.y + bounds.height / 2;
            Point point = new Point(centerX, centerY);

            plugin.setDebugText("Tunnel Point: " + point);
            plugin.setDebugTargetPoint(point);
            // TODO Nate add a random wait command
            // TODO Nate create a factory for instruction sets
            Command command = new Command(
                    List.of(
                            new Instruction(InstructionType.LEFT_CLICK,
                                    new InstructionParameters(point.getX(), point.getY(), 25, 10000, 12000))));
            server.updateCommand(command);
        }
        // TODO Nate else need to search for tunnel again
    }

    /**
     * State: wait for crab to spawn
     * Next: fight crab
     */
    private void handleSearchingState() {
        Command command = new Command(List.of(new Instruction(InstructionType.IDLE,
                new InstructionParameters(0, 0, 0, 2000, 3000))));
        server.updateCommand(command);
    }

    /**
     * State: Fighting until crab is dead
     * Next: Tunnel
     */
    private void handleAttackingState() {
        if (gemstoneCrab != null) {
            // TODO Nate make a util for clicking on NPCs and Objects
            // Click on gemstoneCrab
            Rectangle bounds = gemstoneCrab.getConvexHull().getBounds();
            int centerX = bounds.x + bounds.width / 2;
            int centerY = bounds.y + bounds.height / 2;
            Point point = new Point(centerX, centerY);

            plugin.setDebugText("Crab Point: " + point);
            plugin.setDebugTargetPoint(point);

            // TODO Nate create a factory for instruction sets
            Command command = new Command(
                    List.of(
                            new Instruction(InstructionType.LEFT_CLICK,
                                    new InstructionParameters(point.getX(), point.getY(), 25, 10000, 12000))
                    )
            );
            // TODO Nate how to handle delay between command and action?
            server.updateCommand(command);
        }
    }

    @Override
    public void onGameObjectSpawned(GameObjectSpawned event) {
        final GameObject gameObject = event.getGameObject();

        // Track tunnels in the scene
        if (gameObject.getId() == TUNNEL_OBJECT_ID)
        {
            log.debug("Found tunnel object");
            tunnel = gameObject;
        }
    }

    @Override
    public void onGameObjectDespawned(GameObjectDespawned event) {
        final GameObject gameObject = event.getGameObject();

        if (gameObject.getId() == TUNNEL_OBJECT_ID)
        {
            tunnel = null;
        }
    }

    @Override
    public void onNpcSpawned(NpcSpawned npcSpawned) {
        NPC npc = npcSpawned.getNpc();

        if (npc.getId() == GEMSTONE_CRAB_ID) {
            gemstoneCrab = npc;
            isCrabKilled = false;
        }
    }

    @Override
    public void onNpcDespawned(NpcDespawned npcDespawned) {
        NPC npc = npcDespawned.getNpc();

        if (npc.getId() == GEMSTONE_CRAB_ID) {
            gemstoneCrab = null;
            isCrabKilled = true;
        }
    }
}
