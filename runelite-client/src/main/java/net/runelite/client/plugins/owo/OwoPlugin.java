package net.runelite.client.plugins.owo;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.owo.LogicManager;
import net.runelite.client.owo.LogicType;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
	name = "Owo"
)
public class OwoPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OwoConfig owoConfig;

	LogicManager logicManager;

    @Override
	protected void startUp() throws Exception {
		this.logicManager = new LogicManager(client);
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
		if (logicManager != null && event.getKey() != null && event.getKey().equals("logicType") && event.getNewValue() != null) {
			logicManager.setActiveLogic(LogicType.valueOf(event.getNewValue()));
		}
	}
}
