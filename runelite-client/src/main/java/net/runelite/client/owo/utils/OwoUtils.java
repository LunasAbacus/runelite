package net.runelite.client.owo.utils;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOptionClicked;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class OwoUtils {
    public static Point getNpcClickPoint(NPC npc) {
        Rectangle bounds = npc.getConvexHull().getBounds();
        int centerX = bounds.x + bounds.width / 2;
        int centerY = bounds.y + bounds.height / 2;
        return new Point(centerX, centerY);
    }

    public static Optional<Point> getGameObjectClickPoint(GameObject gameObject) {
        Shape clickbox = gameObject.getClickbox();
        if (clickbox == null) {
            return Optional.empty();
        }

        Rectangle bounds = clickbox.getBounds();
        int centerX = bounds.x + bounds.width / 2;
        int centerY = bounds.y + bounds.height / 2;
        return Optional.of(new Point(centerX, centerY));
    }

    public static Optional<Point> getTileItemClickPoint(Tile tile, Client client) {
        if (tile == null) {
            log.debug("Trying to click null tile");
            return Optional.empty();
        }

        LocalPoint lp = tile.getLocalLocation();
        if (lp == null) {
            log.debug("Trying to click null local point");
            return Optional.empty();
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);

        Rectangle r = poly.getBounds();
        Point click = new Point(
                r.x + r.width / 2,
                r.y + r.height / 2
        );
        return Optional.of(click);
    }

    public static Optional<Point> getTileObjectClickBox(TileObject tileObject) {
        Shape clickbox = tileObject.getClickbox();
        if (clickbox == null) {
            return Optional.empty();
        }

        Rectangle bounds = clickbox.getBounds();
        int centerX = bounds.x + bounds.width / 2;
        int centerY = bounds.y + bounds.height / 2;

        return Optional.of(new Point(centerX, centerY));
    }

    // TODO Nate safety check point is on screen

    public static Optional<GameObject> findClosestGameObject(List<GameObject> gameObjects, WorldPoint playerWp) {
        GameObject best = null;
        int bestDist = Integer.MAX_VALUE;

        for (GameObject obj : gameObjects) {
            if (obj == null) {
                continue;
            }

            WorldPoint objWp = obj.getWorldLocation();

            // If you only want same plane:
            if (objWp.getPlane() != playerWp.getPlane()) {
                continue;
            }

            int dist = playerWp.distanceTo(objWp);
            if (dist < bestDist) {
                bestDist = dist;
                best = obj;
            }
        }

        return Optional.ofNullable(best);
    }

    public static Optional<NPC> findClosestNPC(List<NPC> npcs, WorldPoint playerWp) {
        NPC best = null;
        int bestDist = Integer.MAX_VALUE;

        for (NPC npc : npcs) {
            if (npc == null) {
                continue;
            }

            WorldPoint objWp = npc.getWorldLocation();

            // If you only want same plane:
            if (objWp.getPlane() != playerWp.getPlane()) {
                continue;
            }

            int dist = playerWp.distanceTo(objWp);
            if (dist < bestDist) {
                bestDist = dist;
                best = npc;
            }
        }

        return Optional.ofNullable(best);
    }

    private static final Set<String> WALK_OPTIONS = Set.of(
            "Walk here",
            "Walk"
    );

    public static boolean isWalkAction(MenuOptionClicked event)
    {
        String option = event.getMenuOption();
        return WALK_OPTIONS.contains(option);
    }

    private static final Map<Integer, Integer> WEIGHTED_RANDOM_TICK_WEIGHTS = Map.ofEntries(
            // Example values: key is the result, value is its relative weight.
            Map.entry(1, 50),       // 0.6 seconds
            Map.entry(50, 20),      // 30 seconds
            Map.entry(300, 3),      // 3 minutes
            Map.entry(3, 1)         // 10 minutes
    );

    public static int weightedRandomTick()
    {
        return weightedRandomTick(WEIGHTED_RANDOM_TICK_WEIGHTS);
    }

    public static int weightedRandomTick(Map<Integer, Integer> weights)
    {
        long totalWeight = 0;
        for (int weight : weights.values()) {
            if (weight > 0) {
                totalWeight += weight;
            }
        }

        if (totalWeight <= 0) {
            throw new IllegalArgumentException("weights must contain at least one positive value");
        }

        long roll = ThreadLocalRandom.current().nextLong(totalWeight);
        long cumulative = 0;
        for (Map.Entry<Integer, Integer> entry : weights.entrySet()) {
            int weight = entry.getValue();
            if (weight <= 0) {
                continue;
            }

            cumulative += weight;
            if (roll < cumulative) {
                return entry.getKey();
            }
        }

        throw new IllegalStateException("failed to select weighted random int");
    }


}
