package net.runelite.client.owo.utils;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.owo.instruction.InstructionFactory;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.awt.*;
import java.util.Optional;

public class WorldUtils {
    public static Optional<Point> findTilePoint(final Client client, final int x, final int y) {
        WorldPoint wp = new WorldPoint(x, y, client.getTopLevelWorldView().getPlane());
        LocalPoint lp = LocalPoint.fromWorld(client, wp);

        if (lp == null) {
            return Optional.empty();
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly != null) {
            Rectangle r = poly.getBounds();
            return Optional.of(new Point(
                    r.x + r.width / 2,
                    r.y + r.height / 2
            ));
        } else {
            return Optional.empty();
        }
    }

    public static void clickTile(final OwoPlugin plugin, final int x, final int y) {
        Optional<Point> point = findTilePoint(plugin.getClient(), x, y);
        if (point.isEmpty()) {
            plugin.setDebugText("No tile found.");
            plugin.getServer().updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }

        Command command = InstructionFactory.createClickCommand(point.get().getX(), point.get().getY());
        plugin.getServer().updateCommand(command);

        plugin.setDebugText("Clicking target tile");
        plugin.setDebugTargetPoint(point.get());
    }
}
