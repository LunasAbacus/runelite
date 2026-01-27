package net.runelite.client.owo;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.plugins.owo.OwoPlugin;
import net.runelite.client.plugins.slayer.*;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SlayerMaster extends OwoLogic {

    private final List<NPC> targets = new ArrayList<>();

    private int amount;

    private String taskLocation;
    private String taskName;

    private boolean loginFlag;
    private final List<Pattern> targetNames = new ArrayList<>();

    public SlayerMaster(final OwoPlugin plugin) {
        super(plugin);

        Command command = InstructionFactory.createDefaultIdle();
        server.updateCommand(command);

        plugin.setDebugText("Loaded SlayerMaster");
    }

    @Override
    public void onGameTick(GameTick t) {
        super.onGameTick(t);

        // TODO Nate safety checks for eating/teleporting before idle check

        // If performing an action, idle
        if (isPerformingAction()) {
            idle();
            return;
        }

        if (targets != null && !targets.isEmpty()) {
            // Find closest npc and mark for click
            clickNearestTargetNpc();
        } else {
            plugin.setDebugText("No current task.");
        }

        // TODO Stretch goal, notify discord on task completion
    }

    private void clickNearestTargetNpc() {
        WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
        Optional<NPC> closestTarget = OwoUtils.findClosestNPC(targets, playerWp);
        if (closestTarget.isEmpty()) {
            plugin.setDebugText("No target NPCs found.");
            server.updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }

        Point point = OwoUtils.getNpcClickPoint(closestTarget.get());
        Command command = InstructionFactory.createClickCommand(point.getX(), point.getY());
        server.updateCommand(command);

        plugin.setDebugText("Clicking closest target NPC for task: " + taskName);
        plugin.setDebugTargetPoint(point);
    }

    @Override
    public void startUp() {
        if (client.getGameState() == GameState.LOGGED_IN) {
            loginFlag = true;
            plugin.getClientThread().invoke(this::updateTask);
        }
    }

    @Override
    public void shutDown() {
        targets.clear();
    }

    @Override
    public void onGameStateChanged(GameStateChanged event) {
        switch (event.getGameState()) {
            case HOPPING:
            case LOGGING_IN:
            case CONNECTION_LOST:
                loginFlag = true; // to avoid re-adding the infobox
                targets.clear();
                break;
        }
    }

    @Override
    public void onNpcSpawned(NpcSpawned npcSpawned) {
        NPC npc = npcSpawned.getNpc();
        if (isTarget(npc)) {
            targets.add(npc);
        }
    }

    @Override
    public void onNpcDespawned(NpcDespawned npcDespawned) {
        NPC npc = npcDespawned.getNpc();
        targets.remove(npc);
    }

    @Override
    public void onVarbitChanged(VarbitChanged varbitChanged) {
        int varpId = varbitChanged.getVarpId();
        int varbitId = varbitChanged.getVarbitId();
        if (varpId == VarPlayerID.SLAYER_COUNT
                || varpId == VarPlayerID.SLAYER_AREA
                || varpId == VarPlayerID.SLAYER_TARGET
                || varbitId == VarbitID.SLAYER_TARGET_BOSSID
                || varpId == VarPlayerID.SLAYER_COUNT_ORIGINAL) {
            plugin.getClientThread().invokeLater(this::updateTask);
        }
    }

    private void updateTask() {
        int amount = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
        if (amount > 0) {
            int taskId = client.getVarpValue(VarPlayerID.SLAYER_TARGET);

            int taskDBRow;
            if (taskId == 98 /* Bosses, from [proc,helper_slayer_current_assignment] */) {
                var bossRows = client.getDBRowsByValue(
                        DBTableID.SlayerTaskSublist.ID,
                        DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID,
                        0,
                        client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID));

                if (bossRows.isEmpty()) {
                    return;
                }
                taskDBRow = (Integer) client.getDBTableField(bossRows.get(0), DBTableID.SlayerTaskSublist.COL_TASK, 0)[0];
            } else {
                var taskRows = client.getDBRowsByValue(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, taskId);
                if (taskRows.isEmpty()) {
                    return;
                }
                taskDBRow = taskRows.get(0);
            }

            var taskName = (String) client.getDBTableField(taskDBRow, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0)[0];

            int areaId = client.getVarpValue(VarPlayerID.SLAYER_AREA);
            String taskLocation = null;
            if (areaId > 0) {
                var areaRows = client.getDBRowsByValue(DBTableID.SlayerArea.ID, DBTableID.SlayerArea.COL_AREA_ID, 0, areaId);
                if (areaRows.isEmpty()) {
                    return;
                }

                taskLocation = (String) client.getDBTableField(areaRows.get(0), DBTableID.SlayerArea.COL_AREA_NAME_IN_HELPER, 0)[0];
            }

            if (loginFlag) {
                log.debug("Sync slayer task: {}x {} at {}", amount, taskName, taskLocation);
                setTask(taskName, amount, taskLocation);
            } else if (!Objects.equals(taskName, this.taskName) || !Objects.equals(taskLocation, this.taskLocation)) {
                log.debug("Task change: {}x {} at {}", amount, taskName, taskLocation);
                setTask(taskName, amount, taskLocation);
            } else if (amount != this.amount) {
                log.debug("Amount change: {} -> {}", this.amount, amount);

                this.amount = amount;
            }
        } else if (this.amount > 0) {
            log.debug("Task complete");
            targets.clear();
            setBlankTask();
        }
    }

    @VisibleForTesting
    boolean isTarget(NPC npc) {
        if (targetNames.isEmpty()) {
            return false;
        }

        final NPCComposition composition = npc.getTransformedComposition();
        if (composition == null) {
            return false;
        }

        final String name = composition.getName()
                .replace('\u00A0', ' ')
                .toLowerCase();

        for (Pattern target : targetNames) {
            final Matcher targetMatcher = target.matcher(name);
            if (targetMatcher.find()
                    && (ArrayUtils.contains(composition.getActions(), "Attack")
                    // Pick action is for zygomite-fungi
                    || ArrayUtils.contains(composition.getActions(), "Pick"))) {
                return true;
            }
        }
        return false;
    }

    private void rebuildTargetNames(Task task) {
        targetNames.clear();

        if (task != null) {
            Arrays.stream(task.getTargetNames())
                    .map(SlayerMaster::targetNamePattern)
                    .forEach(targetNames::add);

            targetNames.add(targetNamePattern(taskName.replaceAll("s$", "")));
        }
    }

    private static Pattern targetNamePattern(final String targetName) {
        return Pattern.compile("(?:\\s|^)" + targetName + "(?:\\s|$)", Pattern.CASE_INSENSITIVE);
    }

    private void rebuildTargetList() {
        targets.clear();

        for (NPC npc : client.getNpcs()) {
            if (isTarget(npc)) {
                targets.add(npc);
            }
        }
    }

    void setBlankTask() {
        setTask("", 0, null);
    }

    private void setTask(String name, int amt, String location) {
        taskName = name;
        amount = amt;
        taskLocation = location;

        Task task = Task.getTask(name);
        rebuildTargetNames(task);
        rebuildTargetList();
    }
}
