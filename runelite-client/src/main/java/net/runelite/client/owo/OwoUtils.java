package net.runelite.client.owo;

import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;
import java.util.List;
import java.util.Optional;

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
}
