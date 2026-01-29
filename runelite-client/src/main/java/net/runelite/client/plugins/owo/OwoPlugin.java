package net.runelite.client.plugins.owo;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatClient;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.owo.*;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.puzzlesolver.solver.pathfinding.Pathfinder;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@PluginDescriptor(
	name = "Owo"
)
public class OwoPlugin extends Plugin
{
	@Getter
	@Inject
	private Client client;

	@Getter
	@Inject
	private ClientThread clientThread;

	@Getter
	@Inject
	private InfoBoxManager infoBoxManager;

	@Getter
	@Inject
	private ItemManager itemManager;

	@Getter
	@Inject
	private Notifier notifier;

	@Getter
	@Inject
	private ChatCommandManager chatCommandManager;

	@Getter
	@Inject
	private ScheduledExecutorService executor;

	@Getter
	@Inject
	private ChatClient chatClient;

	@Inject
	private OwoConfig owoConfig;

	@Inject
	private OwoOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Setter
	@Getter
	private String debugText;

	@Setter
	@Getter
	private Point debugTargetPoint;

	@Getter
	private final OwoServer server;

	private OwoLogic activeLogic;

	@Inject
	public OwoPlugin() throws Exception {
		server = new OwoServer();
	}

	private void setActiveLogic(LogicType type) {
		switch (type) {
			case NO_OP:
				this.activeLogic = new NoOpLogic(this);
				break;
			case GEMSTONE_CRAB:
				this.activeLogic = new GemstoneCrab(this);
				break;
			case WOODCUTTING:
				this.activeLogic = new Woodcutting(this);
				break;
			case SLAYER:
				this.activeLogic = new SlayerMaster(this);
				break;
			case AGILITY_COURSE:
				this.activeLogic = new AgilityCourse(this);
				break;
		}
	}

    @Override
	protected void startUp() throws Exception {
		overlayManager.add(overlay);
		setActiveLogic(owoConfig.logicType());
		activeLogic.startUp();
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);
		setActiveLogic(LogicType.NO_OP);
		activeLogic.shutDown();
	}

	@Subscribe
	public void onWorldChanged(WorldChanged worldChanged) {
		activeLogic.onWorldChanged(worldChanged);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		activeLogic.onGameStateChanged(event);
	}

	@Provides
	OwoConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OwoConfig.class);
	}

	/*
	 * Updates configuration store if value is updated
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (event.getKey() != null && event.getKey().equals("logicType") && event.getNewValue() != null) {
			setActiveLogic(LogicType.valueOf(event.getNewValue()));
		}
	}

	@Subscribe
	public void onGameTick(GameTick e) {
		// TODO Nate if allow random pauses, prevent active logic from running for a while
		activeLogic.onGameTick(e);
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event) {
		activeLogic.onDecorativeObjectSpawned(event);
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event) {
		activeLogic.onDecorativeObjectDespawned(event);
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event) {
		activeLogic.onGroundObjectSpawned(event);
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event) {
		activeLogic.onGroundObjectDespawned(event);
	}

	@Subscribe
	public void onStatChanged(StatChanged event) {
		activeLogic.onStatChanged(event);
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned itemSpawned) {
		activeLogic.onItemSpawned(itemSpawned);
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned itemDespawned) {
		activeLogic.onItemDespawned(itemDespawned);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event) {
		activeLogic.onItemContainerChanged(event);
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event) {
		activeLogic.onGameObjectSpawned(event);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged varbitChanged) {
		activeLogic.onVarbitChanged(varbitChanged);
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event) {
		activeLogic.onGameObjectDespawned(event);
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned) {
		activeLogic.onNpcSpawned(npcSpawned);
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned) {
		activeLogic.onNpcDespawned(npcDespawned);
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event) { activeLogic.onAnimationChanged(event); }
}
