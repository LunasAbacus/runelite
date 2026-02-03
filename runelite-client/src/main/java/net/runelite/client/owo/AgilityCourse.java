package net.runelite.client.owo;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.util.*;

import static net.runelite.api.Skill.AGILITY;

@Slf4j
public class AgilityCourse extends OwoLogic {
    // Falador
//    Integer[] activeCourse = {
//            ObjectID.ROOFTOPS_FALADOR_WALLCLIMB, // Working
//            ObjectID.ROOFTOPS_FALADOR_TIGHTROPE_1, // Not working - Ground object 14899
//            ObjectID.ROOFTOPS_FALADOR_HANDHOLDS_START, // ??? - Game Object 14901
//            ObjectID.ROOFTOPS_FALADOR_GAP_1, // Game object
//            ObjectID.ROOFTOPS_FALADOR_GAP_2, // Game Object
//            ObjectID.ROOFTOPS_FALADOR_TIGHTROPE_2, // Game object
//            ObjectID.ROOFTOPS_FALADOR_TIGHTROPE_3, // Ground object
//            ObjectID.ROOFTOPS_FALADOR_GAP_3, // Game object
//            ObjectID.ROOFTOPS_FALADOR_LEDGE_1, // Game Object
//            ObjectID.ROOFTOPS_FALADOR_LEDGE_2, // Game object
//            14923, // Gameobject
//            ObjectID.ROOFTOPS_FALADOR_LEDGE_4, // Game object
//            ObjectID.ROOFTOPS_FALADOR_EDGE // Game object
//    };

    // Colossal Wyrm Advanced
    Integer[] activeCourse = {
            ObjectID.VARLAMORE_WYRM_AGILITY_START_LADDER_TRIGGER, // x1
            ObjectID.VARLAMORE_WYRM_AGILITY_BALANCE_1_TRIGGER, // x7
            ObjectID.VARLAMORE_WYRM_AGILITY_BALANCE_1_TRIGGER,
            ObjectID.VARLAMORE_WYRM_AGILITY_BALANCE_1_TRIGGER,
            ObjectID.VARLAMORE_WYRM_AGILITY_BALANCE_1_TRIGGER,
            ObjectID.VARLAMORE_WYRM_AGILITY_BALANCE_1_TRIGGER,
            ObjectID.VARLAMORE_WYRM_AGILITY_BALANCE_1_TRIGGER,
            ObjectID.VARLAMORE_WYRM_AGILITY_BALANCE_1_TRIGGER,
            ObjectID.VARLAMORE_WYRM_AGILITY_ADVANCED_LADDER_1_TRIGGER, // x1
            ObjectID.VARLAMORE_WYRM_AGILITY_ADVANCED_JUMP_1_TRIGGER, // x1
            ObjectID.VARLAMORE_WYRM_AGILITY_ADVANCED_BALANCE_1_TRIGGER, // x2
            ObjectID.VARLAMORE_WYRM_AGILITY_ADVANCED_BALANCE_1_TRIGGER,
            ObjectID.VARLAMORE_WYRM_AGILITY_END_ZIPLINE_TRIGGER // x1
    };
    Map<Integer, Integer> lookaheadObjects = Map.of(
            ObjectID.VARLAMORE_WYRM_AGILITY_START_LADDER_TRIGGER, ObjectID.VARLAMORE_WYRM_AGILITY_BALANCE_1_TRIGGER,
            ObjectID.VARLAMORE_WYRM_AGILITY_BALANCE_1_TRIGGER, ObjectID.VARLAMORE_WYRM_AGILITY_ADVANCED_LADDER_1_TRIGGER,
            ObjectID.VARLAMORE_WYRM_AGILITY_ADVANCED_LADDER_1_TRIGGER, ObjectID.VARLAMORE_WYRM_AGILITY_ADVANCED_JUMP_1_TRIGGER,
            ObjectID.VARLAMORE_WYRM_AGILITY_ADVANCED_JUMP_1_TRIGGER, ObjectID.VARLAMORE_WYRM_AGILITY_ADVANCED_BALANCE_1_TRIGGER,
            ObjectID.VARLAMORE_WYRM_AGILITY_ADVANCED_BALANCE_1_TRIGGER, ObjectID.VARLAMORE_WYRM_AGILITY_END_ZIPLINE_TRIGGER,
            ObjectID.VARLAMORE_WYRM_AGILITY_END_ZIPLINE_TRIGGER, ObjectID.VARLAMORE_WYRM_AGILITY_START_LADDER_TRIGGER
    );

    Set<Integer> activeCourseObjectIds = new HashSet<>(Arrays.asList(activeCourse));
    Map<Integer, TileObject> activeCourseObjects = new HashMap<>();

    int activeObstaclesIndex = 0;
    int lastAgilityXp = 999999999;
    boolean preventMisclicks = false;

    public AgilityCourse(final OwoPlugin plugin) {
        super(plugin);
        server.updateCommand(InstructionFactory.createDefaultIdle());
        plugin.setDebugText("Loaded Agility Course");
    }

    @Override
    public void onGameTick(GameTick t) {
        super.onGameTick(t);

        // Already performing action
        if (isPerformingAction(1)) {
            preventMisclicks = false;
            idle();
            return;
        }

        preventMisclicks = true;

        // Pickup marks of grace
        if (pickupLoot(3)) {
            return;
        }

        // Click next obstacle
        int nextObstacleId = activeCourse[activeObstaclesIndex];
        handleNextObstacle(nextObstacleId);
    }

    private void handleNextObstacle(int nextObstacleId) {
        if (activeCourseObjects.containsKey(nextObstacleId)) {
            int sleepTicks = 8;
            // TODO Can make this more elegant with weighted long waits, but good enough for now
            // Every ~ 50 obstacles take a short break
            if (Math.random() < 0.015) {
                sleepTicks = 40 + (int)(Math.random() * 30);
            }

            Optional<Point> point = OwoUtils.getTileObjectClickBox(activeCourseObjects.get(nextObstacleId));
            if (point.isPresent()) {
                Command command = InstructionFactory.createClickCommand(point.get().getX(), point.get().getY(), sleepTicks);
                server.updateCommand(command);

                if (sleepTicks > 8) {
                    plugin.setDebugText("Clicking next obstacle after idle: " + sleepTicks + " ticks.");
                    log.debug("Idling for {} ticks", sleepTicks);
                } else {
                    plugin.setDebugText("Clicking next obstacle");
                }
                plugin.setDebugTargetPoint(point.get());
            }
        } else {
            plugin.setDebugText("No course object found");
        }
    }

    @Override
    public void onMenuOptionClicked(MenuOptionClicked event) {
        log.debug("Menu option clicked: {}", event.getMenuOption());
        if (preventMisclicks && OwoUtils.isWalkAction(event)) {
            event.consume();
        }
    }

    @Override
    public void onStatChanged(StatChanged event) {
        if (event.getSkill() != AGILITY) {
            return;
        }

        // Determine how much EXP was actually gained
        int agilityXp = event.getXp();
        int skillGained = agilityXp - lastAgilityXp;
        lastAgilityXp = agilityXp;

        if (skillGained > 0) {
            // Increment to next obstacle
            activeObstaclesIndex++;
            if (activeObstaclesIndex >= activeCourse.length) {
                activeObstaclesIndex = 0;
            }
            log.debug("Advancing to next obstacle. Index: {}", activeObstaclesIndex);
        }
    }

    @Override
    public void onDecorativeObjectSpawned(DecorativeObjectSpawned event) {
        DecorativeObject object = event.getDecorativeObject();
        if (activeCourseObjectIds.contains(object.getId())) {
            activeCourseObjects.put(object.getId(), object);
        }
    }

    @Override
    public void onDecorativeObjectDespawned(DecorativeObjectDespawned event) {
        DecorativeObject object = event.getDecorativeObject();
        if (activeCourseObjectIds.contains(object.getId())) {
            activeCourseObjects.remove(object.getId());
        }
    }

    @Override
    public void onGroundObjectSpawned(GroundObjectSpawned event) {
        GroundObject object = event.getGroundObject();
        if (activeCourseObjectIds.contains(object.getId())) {
            activeCourseObjects.put(object.getId(), object);
        }
    }

    @Override
    public void onGroundObjectDespawned(GroundObjectDespawned event) {
        GroundObject object = event.getGroundObject();
        if (activeCourseObjectIds.contains(object.getId())) {
            activeCourseObjects.remove(object.getId());
        }
    }

    @Override
    public void onGameObjectSpawned(GameObjectSpawned event) {
        GameObject object = event.getGameObject();
        if (activeCourseObjectIds.contains(object.getId())) {
            activeCourseObjects.put(object.getId(), object);
        }
    }

    @Override
    public void onGameObjectDespawned(GameObjectDespawned event) {
        GameObject object = event.getGameObject();
        if (activeCourseObjectIds.contains(object.getId())) {
            activeCourseObjects.remove(object.getId());
        }
    }

    @Override
    public void onWallObjectSpawned(WallObjectSpawned event) {
        WallObject object = event.getWallObject();
        if (activeCourseObjectIds.contains(object.getId())) {
            activeCourseObjects.put(object.getId(), object);
        }
    }

    @Override
    public void onWallObjectDespawned(WallObjectDespawned event) {
        WallObject object = event.getWallObject();
        if (activeCourseObjectIds.contains(object.getId())) {
            activeCourseObjects.remove(object.getId());
        }
    }
}
