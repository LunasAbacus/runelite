package net.runelite.client.owo.modules;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.owo.utils.OwoUtils;

import java.util.*;
import java.util.stream.Collectors;

public class WorldTrackingModule {
    private final Map<Integer, Collection<GameObject>> trackedGameObjects = new HashMap<>();
    private final Map<Integer, Collection<NPC>> trackedNPCs = new HashMap<>();
    private final Map<Integer, Collection<DecorativeObject>> trackedDecorations = new HashMap<>();
    private final Map<Integer, Collection<GroundObject>> trackedGroundObjects = new HashMap<>();
    private final Map<Integer, Collection<WallObject>> trackedWallObjects = new HashMap<>();

    public WorldTrackingModule(final Collection<Integer> npcsToTrack,
                               final Collection<Integer> gameObjectsToTrack,
                               final Collection<Integer> decorationsToTrack,
                               final Collection<Integer> groundObjectsToTrack,
                               final Collection<Integer> wallObjectsToTrack) {
        npcsToTrack.forEach(id -> trackedNPCs.put(id, new ArrayList<>()));
        gameObjectsToTrack.forEach(id -> trackedGameObjects.put(id, new ArrayList<>()));
        decorationsToTrack.forEach(id -> trackedDecorations.put(id, new ArrayList<>()));
        groundObjectsToTrack.forEach(id -> trackedGroundObjects.put(id, new ArrayList<>()));
        wallObjectsToTrack.forEach(id -> trackedWallObjects.put(id, new ArrayList<>()));
    }

    public void clear() {
        trackedNPCs.values().forEach(Collection::clear);
        trackedGameObjects.values().forEach(Collection::clear);
        trackedDecorations.values().forEach(Collection::clear);
        trackedGroundObjects.values().forEach(Collection::clear);
        trackedWallObjects.values().forEach(Collection::clear);
    }

    public Optional<Point> findClosestTrackedGameObject(final Client client, final List<Integer> ids) {
        List<TileObject> matchingTileObjects = new ArrayList<>();
        matchingTileObjects.addAll(getTrackedObjectsById(ids, trackedGameObjects));
        matchingTileObjects.addAll(getTrackedObjectsById(ids, trackedDecorations));
        matchingTileObjects.addAll(getTrackedObjectsById(ids, trackedGroundObjects));
        matchingTileObjects.addAll(getTrackedObjectsById(ids, trackedWallObjects));

        Optional<TileObject> closestTileObject = findClosestTileObject(
                matchingTileObjects,
                client.getLocalPlayer().getWorldLocation()
        );
        if (closestTileObject.isEmpty()) {
            return Optional.empty();
        }

        TileObject tileObject = closestTileObject.get();
        if (tileObject instanceof GameObject) {
            return OwoUtils.getGameObjectClickPoint((GameObject) tileObject, client);
        }
        return OwoUtils.getTileObjectClickBox(tileObject, client);
    }

    public void trackGameObject(final GameObject gameObject) {
        if (trackedGameObjects.containsKey(gameObject.getId())) {
            trackedGameObjects.get(gameObject.getId()).add(gameObject);
        }
    }

    public void untrackGameObject(final GameObject gameObject) {
        if (trackedGameObjects.containsKey(gameObject.getId())) {
            trackedGameObjects.get(gameObject.getId()).remove(gameObject);
        }
    }

    public void trackDecorativeObject(final DecorativeObject decorativeObject) {
        if (trackedDecorations.containsKey(decorativeObject.getId())) {
            trackedDecorations.get(decorativeObject.getId()).add(decorativeObject);
        }
    }

    public void untrackDecorativeObject(final DecorativeObject decorativeObject) {
        if (trackedDecorations.containsKey(decorativeObject.getId())) {
            trackedDecorations.get(decorativeObject.getId()).remove(decorativeObject);
        }
    }

    public void trackGroundObject(final GroundObject groundObject) {
        if (trackedGroundObjects.containsKey(groundObject.getId())) {
            trackedGroundObjects.get(groundObject.getId()).add(groundObject);
        }
    }

    public void untrackGroundObject(final GroundObject groundObject) {
        if (trackedGroundObjects.containsKey(groundObject.getId())) {
            trackedGroundObjects.get(groundObject.getId()).remove(groundObject);
        }
    }

    public void trackWallObject(final WallObject wallObject) {
        if (trackedWallObjects.containsKey(wallObject.getId())) {
            trackedWallObjects.get(wallObject.getId()).add(wallObject);
        }
    }

    public void untrackWallObject(final WallObject wallObject) {
        if (trackedWallObjects.containsKey(wallObject.getId())) {
            trackedWallObjects.get(wallObject.getId()).remove(wallObject);
        }
    }

    public Optional<Point> findClosestTrackedNpcPoint(final Client client, final Collection<Integer> ids) {
        List<NPC> matchingNpcs = ids.stream()
                .map(trackedNPCs::get)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        Optional<NPC> closestNpc = OwoUtils.findClosestNPC(
                matchingNpcs,
                client.getLocalPlayer().getWorldLocation()
        );
        return closestNpc.map(OwoUtils::getNpcClickPoint);
    }

    public Optional<NPC> findClosestNpc(final Client client, final List<Integer> ids) {
        List<NPC> matchingNpcs = ids.stream()
                .map(trackedNPCs::get)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        return OwoUtils.findClosestNPC(
                matchingNpcs,
                client.getLocalPlayer().getWorldLocation()
        );
    }

    public void trackNPC(final NPC npc) {
        if (trackedNPCs.containsKey(npc.getId())) {
            trackedNPCs.get(npc.getId()).add(npc);
        }
    }

    public void untrackNPC(final NPC npc) {
        if (trackedNPCs.containsKey(npc.getId())) {
            trackedNPCs.get(npc.getId()).remove(npc);
        }
    }

    private static <T> List<T> getTrackedObjectsById(final Collection<Integer> ids, final Map<Integer, Collection<T>> trackedObjects) {
        return ids.stream()
                .map(trackedObjects::get)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private static Optional<TileObject> findClosestTileObject(final Collection<TileObject> tileObjects, final WorldPoint playerWp) {
        TileObject best = null;
        int bestDist = Integer.MAX_VALUE;

        for (TileObject tileObject : tileObjects) {
            if (tileObject == null) {
                continue;
            }

            WorldPoint objectWp = tileObject.getWorldLocation();
            if (objectWp.getPlane() != playerWp.getPlane()) {
                continue;
            }

            int dist = playerWp.distanceTo(objectWp);
            if (dist < bestDist) {
                bestDist = dist;
                best = tileObject;
            }
        }

        return Optional.ofNullable(best);
    }
}
