package net.runelite.client.owo;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.events.*;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.util.Optional;

@Slf4j
public class GemstoneCrab extends OwoLogic {

    // Tunnel game object ID
    private static final int TUNNEL_OBJECT_ID = 57631;

    // Gemstone Crab boss NPC ID
    private static final int GEMSTONE_CRAB_ID = 14779;

    private boolean isCrabKilled = false;

    private NPC gemstoneCrab = null;
    private GameObject tunnel = null;

    // TODO Nate rewrite this completely with new method

    public GemstoneCrab(OwoPlugin plugin) {
        super(plugin);

        Command command = InstructionFactory.createSimpleIdleCommand(500, 1000);
        server.updateCommand(command);

        plugin.setDebugText("Loaded GemstoneCrab");
    }

    @Override
    public void onGameTick(GameTick t) {
        super.onGameTick(t);

        if (isPerformingAction()) {
            // Wait for action to complete
            Command command = InstructionFactory.createSimpleIdleCommand(1000, 2000);
            server.updateCommand(command);
            return;
        }

        if (gemstoneCrab != null) {
            handleAttackingState();
        } else if (isCrabKilled) {
            handleTunnelingState();
        } else {
            handleSearchingState();
        }
    }

    /**
     * Go through tunnel
     * Next: Search for crab
     */
    private void handleTunnelingState() {
        if (tunnel != null) {
            Optional<Point> point = OwoUtils.getGameObjectClickPoint(tunnel);
            if (point.isEmpty()) {
                return;
            }
            Command command = InstructionFactory.createClickCommand(point.get().getX(), point.get().getY());
            server.updateCommand(command);

            plugin.setDebugText("Tunnel Point: " + point);
            plugin.setDebugTargetPoint(point.get());
        }
        // TODO Nate else need to search for tunnel again like if outside view
    }

    /**
     * State: wait for crab to spawn
     * Next: fight crab
     */
    private void handleSearchingState() {
        plugin.setDebugTargetPoint(null);
        Command command = InstructionFactory.createSimpleIdleCommand(2000, 3000);
        server.updateCommand(command);
    }

    /**
     * State: Fighting until crab is dead
     * Next: Tunnel
     */
    private void handleAttackingState() {
        if (gemstoneCrab != null) {
            Point point = OwoUtils.getNpcClickPoint(gemstoneCrab);

            plugin.setDebugText("Crab Point: " + point);
            plugin.setDebugTargetPoint(point);

            Command command = InstructionFactory.createClickCommand(point.getX(), point.getY());
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
