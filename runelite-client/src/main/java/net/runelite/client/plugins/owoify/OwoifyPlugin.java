package net.runelite.client.plugins.owoify;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import okhttp3.OkHttpClient;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@PluginDescriptor(
	name = "Owoify"
)
public class OwoifyPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded e) {
		if(e.getGroupId() == WidgetID.DIALOG_NPC_GROUP_ID) {
			Widget widget = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT.getPackedId());
			clientThread.invokeLater(() -> {
				String text = Owoify.convert(widget.getText());
				widget.setText(text);
			});
		}
		else if(e.getGroupId() == WidgetID.DIALOG_PLAYER_GROUP_ID) {
			Widget widget = client.getWidget(WidgetInfo.DIALOG_PLAYER_TEXT.getPackedId());
			clientThread.invokeLater(() -> {
				String text = Owoify.convert(widget.getText());
				widget.setText(text);
			});
		}
		else if(e.getGroupId() == 541) {
			HashMap<Integer, Boolean> slots = new HashMap();

			for(int i=0;i<29;i++) {
				Widget prayer = client.getWidget(541, i+9);
				int slot = (int) Math.floor(Math.random()*29);

				while(slots.get(slot) != null) {
					slot = (int) Math.floor(Math.random()*29);
				}

				int x = (slot % 5) * 37;
				int y = (slot/5) * 37;
				prayer.setPos(x, y);
				slots.put(slot, true);
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage e) throws IOException {
		if(e.getType().equals(ChatMessageType.DIALOG)) {
			return;
		}

		e.getMessageNode().setValue(Owoify.convert(e.getMessage()));
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged e) {
		String text = Owoify.convert(e.getOverheadText());
		e.getActor().setOverheadText(text);
		e.getActor().setOverheadCycle(30);
	}
}
