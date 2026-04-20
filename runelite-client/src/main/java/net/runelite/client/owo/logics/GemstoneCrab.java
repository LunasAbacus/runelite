package net.runelite.client.owo.logics;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.events.*;
import net.runelite.client.owo.OwoLogic;
import net.runelite.client.owo.instruction.InstructionFactory;
import net.runelite.client.owo.utils.OwoUtils;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.util.Optional;

@Slf4j
public class GemstoneCrab extends OwoLogic<DummyState> {

    // Tunnel game object ID
    private static final int TUNNEL_OBJECT_ID = 57631;

    // Gemstone Crab boss NPC ID
    private static final int GEMSTONE_CRAB_ID = 14779;

    private boolean isCrabKilled = false;

    private NPC gemstoneCrab = null;
    private GameObject tunnel = null;

    public GemstoneCrab(OwoPlugin plugin) {
        super(plugin, DummyState.NO_OP);

        Command command = InstructionFactory.createSimpleIdleCommand(500, 1000);
        server.updateCommand(command);

        plugin.setDebugText("Loaded GemstoneCrab");

        // Check for gemstone grab on load
        for (NPC npc : client.getNpcs()) {
            if (npc.getId() == GEMSTONE_CRAB_ID) {
                gemstoneCrab = npc;
                isCrabKilled = false;
                break;
            }
        }
    }

    @Override
    public void onGameTick(GameTick t) {
        super.onGameTick(t);

        if (isPerformingAction()) {
            // Wait for action to complete
            Command command = InstructionFactory.createSimpleIdleCommand(1000, 2000);
            server.updateCommand(command);
            plugin.setDebugTargetPoint(null);
            return;
        }

        if (gemstoneCrab != null) {
            handleAttackingState();
        } else {
            handleTunnelingState();
        }
    }

    /**
     * Go through tunnel
     * Next: Search for crab
     */
    private void handleTunnelingState() {
        if (tunnel != null) {
            Optional<Point> point = OwoUtils.getGameObjectClickPoint(tunnel, client);
            if (point.isEmpty()) {
                return;
            }
            Command command = InstructionFactory.createClickCommand(point.get().getX(), point.get().getY(), 20);
            server.updateCommand(command);

            plugin.setDebugText("Entering Tunnel");
            plugin.setDebugTargetPoint(point.get());
        }
    }

    /**
     * State: Fighting until crab is dead
     * Next: Tunnel
     */
    private void handleAttackingState() {
        if (gemstoneCrab != null) {
            Point point = OwoUtils.getNpcClickPoint(gemstoneCrab);

            plugin.setDebugText("Attacking Crab");
            plugin.setDebugTargetPoint(point);

            Command command = InstructionFactory.createClickCommand(point.getX(), point.getY());
            server.updateCommand(command);
        }
    }

    @Override
    public void onGameObjectSpawned(GameObjectSpawned event) {
        final GameObject gameObject = event.getGameObject();

        // Track tunnels in the scene
        if (gameObject.getId() == TUNNEL_OBJECT_ID) {
            log.debug("Found tunnel object");
            tunnel = gameObject;
        }
    }

    @Override
    public void onGameObjectDespawned(GameObjectDespawned event) {
        final GameObject gameObject = event.getGameObject();

        if (gameObject.getId() == TUNNEL_OBJECT_ID) {
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
