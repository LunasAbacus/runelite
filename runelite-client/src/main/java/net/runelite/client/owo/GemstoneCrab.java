package net.runelite.client.owo;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.owo.instruction.Instruction;
import net.runelite.client.owo.instruction.InstructionParameters;
import net.runelite.client.owo.instruction.InstructionType;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.util.List;

@Slf4j
public class GemstoneCrab extends OwoLogic {

    // Tunnel game object ID
    private static final int TUNNEL_OBJECT_ID = 57631;

    // Gemstone Crab boss NPC ID
    private static final int GEMSTONE_CRAB_ID = 14779;

    private NPC gemstoneCrab = null;
    private GameObject tunnel = null;

    private final OwoPlugin plugin;

    public GemstoneCrab(OwoServer server, Client client, OwoPlugin plugin) {
        super(server, client);
        this.plugin = plugin;

        Command command = new Command(List.of(new Instruction(InstructionType.IDLE, new InstructionParameters(1, 1, 1, 1, 1))));
        server.updateCommand(command);

        plugin.setDebugText("Loaded GemstoneCrab");
    }

    @Override
    public void onGameTick(GameTick t) {
        // TODO Nate these points are not accurate
        // TODO Nate gemstone crab is still not getting found
        if (gemstoneCrab != null) {
            Point crabPoint = Perspective.localToCanvas(
                    client,
                    gemstoneCrab.getLocalLocation(),
                    gemstoneCrab.getLogicalHeight()
            );
            plugin.setDebugText("Crab Point: " + crabPoint);
            if (crabPoint != null) {
                Command command = new Command(List.of(new Instruction(InstructionType.LEFT_CLICK, new InstructionParameters(crabPoint.getX(), crabPoint.getY(), 25, 700, 200))));
                server.updateCommand(command);
            }
        } else if (tunnel != null) {
            Point tunnelPoint = tunnel.getCanvasLocation();
            plugin.setDebugText("Tunnel Point: " + tunnelPoint);
            if (tunnelPoint != null) {
                Command command = new Command(List.of(new Instruction(InstructionType.LEFT_CLICK, new InstructionParameters(tunnelPoint.getX(), tunnelPoint.getY(), 25, 700, 200))));
                server.updateCommand(command);
            }
        } else {
            plugin.setDebugText("GemstoneCrab loaded, but no object or npc found");
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

        // Track tunnels in the scene
        if (gameObject.getId() == TUNNEL_OBJECT_ID)
        {
            tunnel = null;
        }
    }

    @Override
    public void onNpcSpawned(NpcSpawned npcSpawned) {
        NPC npc = npcSpawned.getNpc();

        if (npc.getId() == GEMSTONE_CRAB_ID) {
            log.debug("Found crab npc");
            gemstoneCrab = npc;
        }
    }

    @Override
    public void onNpcDespawned(NpcDespawned npcDespawned) {
        NPC npc = npcDespawned.getNpc();

        if (npc.getId() == GEMSTONE_CRAB_ID) {
            gemstoneCrab = null;
        }
    }
}
