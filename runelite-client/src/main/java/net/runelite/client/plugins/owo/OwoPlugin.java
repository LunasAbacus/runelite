package net.runelite.client.plugins.owo;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.owo.*;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
	name = "Owo"
)
public class OwoPlugin extends Plugin
{
	@Getter
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

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
//		activeLogic.shutDown();
	}

	@Subscribe
	public void onWorldChanged(WorldChanged worldChanged) {
		activeLogic.onWorldChanged(worldChanged);
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
	public void onItemContainerChanged(ItemContainerChanged event) {
		activeLogic.onItemContainerChanged(event);
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event) {
		activeLogic.onGameObjectSpawned(event);
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
