package net.runelite.client.owo.logics;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.owo.OwoLogic;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.util.*;

@Slf4j
public class AgilityCourse extends OwoLogic<AgilityCourse.State> {
    protected enum State {
        MOVING,
        IDLE,
        EMERGENCY
    }

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

//    // Colossal Wyrm Advanced
//    Integer[] activeCourse = {
//            ObjectID.VARLAMORE_WYRM_AGILITY_START_LADDER_TRIGGER, // x1
//            ObjectID.VARLAMORE_WYRM_AGILITY_BALANCE_1_TRIGGER, // x7
//            ObjectID.VARLAMORE_WYRM_AGILITY_BALANCE_1_TRIGGER,
//            ObjectID.VARLAMORE_WYRM_AGILITY_BALANCE_1_TRIGGER,
//            ObjectID.VARLAMORE_WYRM_AGILITY_BALANCE_1_TRIGGER,
//            ObjectID.VARLAMORE_WYRM_AGILITY_BALANCE_1_TRIGGER,
//            ObjectID.VARLAMORE_WYRM_AGILITY_BALANCE_1_TRIGGER,
//            ObjectID.VARLAMORE_WYRM_AGILITY_BALANCE_1_TRIGGER,
//            ObjectID.VARLAMORE_WYRM_AGILITY_ADVANCED_LADDER_1_TRIGGER, // x1
//            ObjectID.VARLAMORE_WYRM_AGILITY_ADVANCED_JUMP_1_TRIGGER, // x1
//            ObjectID.VARLAMORE_WYRM_AGILITY_ADVANCED_BALANCE_1_TRIGGER, // x2
//            ObjectID.VARLAMORE_WYRM_AGILITY_ADVANCED_BALANCE_1_TRIGGER,
//            ObjectID.VARLAMORE_WYRM_AGILITY_END_ZIPLINE_TRIGGER // x1
//    };

    // Ape Atoll
    private static final Map<WorldPoint, Integer> activeCourse = Map.of(
            new WorldPoint(2770, 2747, 0), ObjectID._100_ILM_STEPPING_STONE,
            new WorldPoint(2753, 2742, 0), ObjectID._100_ILM_CLIMBABLE_TREE,
            new WorldPoint(2753, 2742, 2), ObjectID._100_ILM_MONKEYBARS_START,
            new WorldPoint(2747, 2741, 0), ObjectID._100_ILM_CLIFF_CLIMB_1,
            new WorldPoint(2742, 2741, 0), ObjectID._100_ILM_ROPE_SWING,
            new WorldPoint(2756, 2731, 0), ObjectID._100_ILM_AGILITY_TREE_BASE,
            // Failure recovery
            new WorldPoint(2757, 2748, 0), ObjectID._100_ILM_STEPPING_STONE,
            new WorldPoint(2755, 2741, 0), ObjectID._100_ILM_STEPPING_STONE,
            new WorldPoint(2752, 2740, 0), ObjectID._100_ILM_CLIMBABLE_TREE,
//            new Point(,), ObjectID._100_ILM_MONKEYBARS_START,
            // ObjectID._100_ILM_CLIFF_CLIMB_1 covered by original case
//            new Point(,), ObjectID._100_ILM_ROPE_SWING,
            new WorldPoint(2758, 2735, 0), ObjectID._100_ILM_AGILITY_TREE_BASE
    );

    public AgilityCourse(final OwoPlugin plugin) {
        super(plugin, State.IDLE, List.of(), activeCourse.values(), List.of(), activeCourse.values(), List.of());
        plugin.setDebugText("Loaded Agility Course");
    }

    private int idleDuration = 0;
    @Override
    public void onGameTick(GameTick t) {
        super.onGameTick(t);

        if (idleDuration > 0) {
            idleDuration--;
            return;
        }

        int randomIdle = shouldRandomIdle(TaskIntensity.LOW);
        if (randomIdle > 0) {
            log.debug("Taking a idle break for {} ticks", randomIdle);
            idleDuration = randomIdle;
            return;
        }

        if (playerModule.isHurt(0.5)) {
            setState(State.EMERGENCY);
        }

        updateState();
    }

    private void updateState() {
        switch (state) {
            case IDLE:
                if (isPerformingAction(1)) {
                    setState(State.MOVING);
                    break;
                }
                if (canAct()) {
                    plugin.setDebugText("Clicking next obstacle");
                    WorldPoint playerLocation = interactionManager.findPlayerLocation();
                    Integer nextObstacleId = findClosestObstacleId(playerLocation);
                    if (nextObstacleId != null) {
                        interactionManager.clickClosestGameObject(List.of(nextObstacleId), "Next Obstacle");
                    } else {
                        plugin.setDebugText("No next obstacle configured for current location. " + playerLocation);
                    }
                    debounce(2000);
                }
                break;
            case MOVING:
                if (!isPerformingAction(1)) {
                    setState(State.IDLE);
                    break;
                }
                plugin.setDebugText("Navigating obstacle");
                break;
            case EMERGENCY:
                if (!playerModule.isHurt(0.6)) {
                    setState(State.IDLE);
                }
                plugin.setDebugText("Health got too low. Stopping");
        }
    }

    private Integer findClosestObstacleId(final WorldPoint playerLocation) {
        Integer exactMatch = activeCourse.get(playerLocation);
        if (exactMatch != null) {
            return exactMatch;
        }

        WorldPoint closestPoint = null;
        int closestDistance = Integer.MAX_VALUE;

        for (WorldPoint coursePoint : activeCourse.keySet()) {
            int distance = playerLocation.distanceTo(coursePoint);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestPoint = coursePoint;
            }
        }

        return closestPoint != null ? activeCourse.get(closestPoint) : null;
    }
}
