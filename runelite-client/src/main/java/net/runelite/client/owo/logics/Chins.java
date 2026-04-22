package net.runelite.client.owo.logics;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.owo.OwoLogic;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.util.Comparator;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class Chins extends OwoLogic<Chins.State> {
    protected enum State {
        IDLE,
        RESETTING_TRAP
    }

    private static final int SHAKING_BOX_TRAP = 9383; // Game Object
//    private static int COLLAPSED_TRAP = 10008; // Ground item
    private static final List<WorldPoint> validTrapLocations = List.of(
            new WorldPoint(2557, 2915, 0),
            new WorldPoint(2556, 2914, 0),
            new WorldPoint(2555, 2913, 0)
    );
    private final Map<WorldPoint, Long> lastTrapClickTimes = new HashMap<>();

    public Chins(final OwoPlugin plugin) {
        super(plugin, State.IDLE, List.of(), List.of(SHAKING_BOX_TRAP));
        plugin.setDebugText("Loaded Chins");
    }

    @Override
    public void onGameTick(GameTick t) {
        super.onGameTick(t);

        updateState();
    }

    private void updateState() {
        switch (state) {
            case IDLE:
                if (isPerformingAction(1)) {
                    setState(State.RESETTING_TRAP);
                    break;
                }
                Collection<GameObject> shakingTraps = interactionManager.getTrackedObjects(SHAKING_BOX_TRAP);
                if (canAct() && !shakingTraps.isEmpty()) {
                    plugin.setDebugText("Resetting shaking trap");
                    clickNextTrap(shakingTraps);
                    debounce(1000);
                }
                break;
            case RESETTING_TRAP:
                if (!isPerformingAction(1)) {
                    setState(State.IDLE);
                    break;
                }
                plugin.setDebugText("Waiting for next catch");
                break;
        }
    }

    private void clickNextTrap(final Collection<GameObject> shakingTraps) {
        WorldPoint playerLocation = interactionManager.findPlayerLocation();
        GameObject nextTrapObject = shakingTraps.stream()
                .filter(gameObject -> validTrapLocations.contains(gameObject.getWorldLocation()))
                .min(Comparator
                        .comparingLong((GameObject gameObject) -> lastTrapClickTimes.getOrDefault(gameObject.getWorldLocation(), Long.MIN_VALUE))
                        .thenComparingInt(gameObject -> gameObject.getWorldLocation().distanceTo(playerLocation)))
                .orElse(null);

        if (nextTrapObject == null) {
            plugin.setDebugText("No valid shaking trap found");
            log.debug("No valid shaking trap found in configured locations");
            return;
        }

        lastTrapClickTimes.put(nextTrapObject.getWorldLocation(), System.currentTimeMillis());
        interactionManager.clickGameObject(nextTrapObject);
    }
}
