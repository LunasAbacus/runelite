package net.runelite.client.owo.modules;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.client.owo.utils.OwoUtils;

import java.util.*;
import java.util.stream.Collectors;

public class WorldTrackingModule {
    private final Map<Integer, List<GameObject>> trackedGameObjects = new HashMap<>();
    private final Map<Integer, List<NPC>> trackedNPCs = new HashMap<>();

    public WorldTrackingModule(final List<Integer> npcsToTrack, final List<Integer> gameObjectsToTrack) {
        npcsToTrack.forEach(id -> trackedNPCs.put(id, new ArrayList<>()));
        gameObjectsToTrack.forEach(id -> trackedGameObjects.put(id, new ArrayList<>()));
    }

    public void clear() {
        trackedNPCs.values().forEach(List::clear);
        trackedGameObjects.values().forEach(List::clear);
    }

    public Optional<Point> findClosestTrackedGameObject(final Client client, final List<Integer> ids) {
        List<GameObject> matchingGameObjects = ids.stream()
                .map(trackedGameObjects::get)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        Optional<GameObject> closestGameObject = OwoUtils.findClosestGameObject(
                matchingGameObjects,
                client.getLocalPlayer().getWorldLocation()
        );
        if (closestGameObject.isPresent()) {
            return OwoUtils.getGameObjectClickPoint(closestGameObject.get());
        }
        return Optional.empty();
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

    public Optional<Point> findClosestTrackedNpcPoint(final Client client, final List<Integer> ids) {
        List<NPC> matchingNpcs = ids.stream()
                .map(trackedNPCs::get)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
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
                .flatMap(List::stream)
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
}
