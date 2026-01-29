package net.runelite.client.owo;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.util.*;

@Slf4j
public class AgilityCourse extends OwoLogic {

    // Falador
    Integer[] faladorCourse = {
            ObjectID.ROOFTOPS_FALADOR_WALLCLIMB, // Working
            ObjectID.ROOFTOPS_FALADOR_TIGHTROPE_1, // Not working - Ground object 14899
            ObjectID.ROOFTOPS_FALADOR_HANDHOLDS_START, // ??? - Game Object 14901
            ObjectID.ROOFTOPS_FALADOR_GAP_1, // Game object
            ObjectID.ROOFTOPS_FALADOR_GAP_2, // Game Object
            ObjectID.ROOFTOPS_FALADOR_TIGHTROPE_2, // Game object
            ObjectID.ROOFTOPS_FALADOR_TIGHTROPE_3, // Ground object
            ObjectID.ROOFTOPS_FALADOR_GAP_3, // Game object
            ObjectID.ROOFTOPS_FALADOR_LEDGE_1, // Game Object
            ObjectID.ROOFTOPS_FALADOR_LEDGE_2, // Game object
            14923, // Gameobject
            ObjectID.ROOFTOPS_FALADOR_LEDGE_4, // Game object
            ObjectID.ROOFTOPS_FALADOR_EDGE // Game object
    };
    Set<Integer> faladorCourseObjectIds = new HashSet<>(Arrays.asList(faladorCourse));
    Map<Integer, TileObject> faladorCourseObjects = new HashMap<>();

    boolean started = false;

    int activeObstaclesIndex = 0;

    public AgilityCourse(final OwoPlugin plugin) {
        super(plugin);

        Command command = InstructionFactory.createDefaultIdle();
        server.updateCommand(command);

        plugin.setDebugText("Loaded Agility Course");
    }

    @Override
    public void onGameTick(GameTick t) {
        super.onGameTick(t);

        // If performing an action, idle
        if (isPerformingAction(2)) {
            idle();
            return;
        }

        // Pickup marks of grace
        if (pickupLoot(3)) {
            return;
        }

        // Click next obstacle
        int nextObstacleId = faladorCourse[activeObstaclesIndex];
        if (faladorCourseObjects.containsKey(nextObstacleId)) {
            started = true;
            Optional<Point> point = OwoUtils.getTileObjectClickBox(faladorCourseObjects.get(nextObstacleId));
            if (point.isPresent()) {
                Command command = InstructionFactory.createClickCommand(point.get().getX(), point.get().getY());
                server.updateCommand(command);

                plugin.setDebugText("Clicking next obstacle");
                plugin.setDebugTargetPoint(point.get());
            }
        } else {
            plugin.setDebugText("No falador course object found");
        }
    }

    @Override
    public void onDecorativeObjectSpawned(DecorativeObjectSpawned event) {
        DecorativeObject object = event.getDecorativeObject();
        if (faladorCourseObjectIds.contains(object.getId())) {
            faladorCourseObjects.put(object.getId(), object);
        }
    }

    @Override
    public void onDecorativeObjectDespawned(DecorativeObjectDespawned event) {
        DecorativeObject object = event.getDecorativeObject();
        if (faladorCourseObjectIds.contains(object.getId())) {
            faladorCourseObjects.remove(object.getId());
        }
    }

    @Override
    public void onGroundObjectSpawned(GroundObjectSpawned event) {
        GroundObject object = event.getGroundObject();
        if (faladorCourseObjectIds.contains(object.getId())) {
            faladorCourseObjects.put(object.getId(), object);
        }
    }

    @Override
    public void onGroundObjectDespawned(GroundObjectDespawned event) {
        GroundObject object = event.getGroundObject();
        if (faladorCourseObjectIds.contains(object.getId())) {
            faladorCourseObjects.remove(object.getId());
        }
    }

    @Override
    public void onGameObjectSpawned(GameObjectSpawned event) {
        GameObject object = event.getGameObject();
        if (faladorCourseObjectIds.contains(object.getId())) {
            faladorCourseObjects.put(object.getId(), object);
        }
    }

    @Override
    public void onGameObjectDespawned(GameObjectDespawned event) {
        GameObject object = event.getGameObject();
        if (faladorCourseObjectIds.contains(object.getId())) {
            faladorCourseObjects.remove(object.getId());
        }
    }

    @Override
    public void onStatChanged(StatChanged event)
    {
        if (event.getSkill() == Skill.AGILITY && started) {
            // Increment to next obstacle
            activeObstaclesIndex++;
            if (activeObstaclesIndex >= faladorCourse.length) {
                activeObstaclesIndex = 0;
            }
            log.debug("Agility xp : next obstacle. Index: {}", activeObstaclesIndex);
            return;
        }

        if (event.getSkill() == Skill.HITPOINTS && started) {
            log.debug("Failed obstacle. Resetting index to 0");
            activeObstaclesIndex = 0;
        }
    }
}
