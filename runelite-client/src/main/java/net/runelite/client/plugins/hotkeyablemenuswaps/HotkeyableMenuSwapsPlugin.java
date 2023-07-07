/*
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * Copyright (c) 2018, Kamiel
 * Copyright (c) 2019, Rami <https://github.com/Rami-J>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.hotkeyablemenuswaps;

import static com.google.common.base.Predicates.alwaysTrue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Provides;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import static net.runelite.api.MenuAction.*;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.FocusChanged;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.widgets.WidgetID;
import static net.runelite.api.widgets.WidgetID.SPELLBOOK_GROUP_ID;
import net.runelite.api.widgets.WidgetInfo;
import static net.runelite.api.widgets.WidgetInfo.TO_GROUP;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.menuentryswapper.MenuEntrySwapperConfig;
import net.runelite.client.plugins.menuentryswapper.MenuEntrySwapperPlugin;
import net.runelite.client.util.Text;

// TODO when you press a key, then press and release a different key, the original key is no longer used for the keybind.
// Also, modifier keys cannot be activated if they are pressed when the client does not have focus, which is annoying.
// TODO mystery bug where my "T" for tree keybind stopped working entirely.
// wont-fix - when using shift as a hotkey it prevents some other hotkeys (e.g. lowercase "t") from being activated while shift is down.

@PluginDescriptor(
	name = "Custom Menu Swaps",
	tags = {"entry", "swapper", "custom", "text"}
)
@PluginDependency(MenuEntrySwapperPlugin.class)
public class HotkeyableMenuSwapsPlugin extends Plugin implements KeyListener
{
	@Inject
	private Client client;

	@Inject
	private HotkeyableMenuSwapsConfig config;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private MenuEntrySwapperConfig menuEntrySwapperConfig;

	// If a hotkey corresponding to a swap is currently held, these variables will be non-null. currentBankModeSwap is an exception because it uses menu entry swapper's bank swap enum, which already has an "off" value.
	// These variables do not factor in left-click swaps.
	private volatile BankSwapMode currentBankModeSwap;
	private volatile OccultAltarSwap hotkeyOccultAltarSwap;
	private volatile TreeRingSwap hotkeyTreeRingSwap;
	private volatile boolean swapUse;
	private volatile boolean swapJewelleryBox;
	private volatile PortalNexusSwap swapPortalNexus;
	private volatile PortalNexusXericsTalismanSwap swapPortalNexusXericsTalisman;
	private volatile PortalNexusDigsitePendantSwap swapPortalNexusDigsitePendant;

	final List<CustomSwap> customSwaps = new ArrayList<>();
	final List<CustomSwap> customShiftSwaps = new ArrayList<>();
	final List<CustomSwap> customHides = new ArrayList<>();

	@Provides
	HotkeyableMenuSwapsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HotkeyableMenuSwapsConfig.class);
	}

	@Override
	protected void startUp() {
		reloadCustomSwaps();

		resetHotkeys();

		keyManager.registerKeyListener(this);

		swapContains("venerate", "altar of the occult"::equals, "standard", () -> getCurrentOccultAltarSwap() == OccultAltarSwap.STANDARD);
		swapContains("venerate", "altar of the occult"::equals, "ancient", () -> getCurrentOccultAltarSwap() == OccultAltarSwap.ANCIENT);
		swapContains("venerate", "altar of the occult"::equals, "lunar", () -> getCurrentOccultAltarSwap() == OccultAltarSwap.LUNAR);
		swapContains("venerate", "altar of the occult"::equals, "arceuus", () -> getCurrentOccultAltarSwap() == OccultAltarSwap.ARCEUUS);

		for (String option : new String[]{"last-destination", "zanaris", "configure", "tree"}) {
			swapContains(option, alwaysTrue(), "zanaris", () -> getCurrentTreeRingSwap() == TreeRingSwap.ZANARIS);
			swapContains(option, alwaysTrue(), "last-destination", () -> getCurrentTreeRingSwap() == TreeRingSwap.LAST_DESTINATION);
			swapContains(option, alwaysTrue(), "configure", () -> getCurrentTreeRingSwap() == TreeRingSwap.CONFIGURE);
			swapContains(option, alwaysTrue(), "tree", () -> getCurrentTreeRingSwap() == TreeRingSwap.TREE);
		}
	}

	@Override
	protected void shutDown() {
		keyManager.unregisterKeyListener(this);
	}

	private OccultAltarSwap getCurrentOccultAltarSwap() {
		OccultAltarSwap currentSpellbook = OccultAltarSwap.getCurrentSpellbookMenuOption(client);
		if (hotkeyOccultAltarSwap == null || hotkeyOccultAltarSwap == currentSpellbook) {
			HotkeyableMenuSwapsConfig.OccultAltarLeftClick leftClickSwap = config.getOccultAltarLeftClickSwap();
			return (leftClickSwap.getFirstOption() == currentSpellbook)
				? leftClickSwap.getSecondOption()
				: leftClickSwap.getFirstOption();
		} else {
			return hotkeyOccultAltarSwap;
		}
	}

	private boolean swapJewelleryBoxSpecificOption() {
		return swapJewelleryBox && !vanillaJewelleryBoxSwapEnabled();
	}

	private boolean swapJewelleryBoxTeleportMenuOption() {
		return swapJewelleryBox && vanillaJewelleryBoxSwapEnabled();
	}

	private boolean vanillaJewelleryBoxSwapEnabled()
	{
		return (boolean) configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, "menuentryswapperplugin", Boolean.class) && menuEntrySwapperConfig.swapJewelleryBox();
	}

	private boolean vanillaPortalNexusSwapEnabled() {
		return (boolean) configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, "menuentryswapperplugin", Boolean.class) && menuEntrySwapperConfig.swapPortalNexus();
	}

	/**
	 * Takes into account left-click swap in addition to any hotkeys held down.
	 */
	private PortalNexusDigsitePendantSwap getPortalNexusDigsitePendantSwap()
	{
		return swapPortalNexusDigsitePendant != null ? swapPortalNexusDigsitePendant : config.pohDigsitePendantLeftClick();
	}

	/**
	 * Takes into account left-click swap in addition to any hotkeys held down.
	 */
	private PortalNexusXericsTalismanSwap getPortalNexusXericsTalismanSwap()
	{
		return swapPortalNexusXericsTalisman != null ? swapPortalNexusXericsTalisman : config.pohXericsTalismanLeftClick();
	}

//	@Getter
//	@AllArgsConstructor
//	public enum JewelleryBoxSwap {
//		TELEPORT_MENU(MenuAction.GAME_OBJECT_SECOND_OPTION, HotkeyableMenuSwapsConfig::pohXericsTalismanTeleportMenu),
//		DESTINATION(MenuAction.GAME_OBJECT_THIRD_OPTION, HotkeyableMenuSwapsConfig::pohXericsTalismanTeleportMenu),
//		;
//
//		private final MenuAction menuAction;
//		private final Function<HotkeyableMenuSwapsConfig, Keybind> keybindFunction;
//
//		public Keybind getKeybind(HotkeyableMenuSwapsConfig config) {
//			return keybindFunction.apply(config);
//		}
//	}
//
	@Getter
	@AllArgsConstructor
	public enum PortalNexusSwap {
		DESTINATION(MenuAction.GAME_OBJECT_FIRST_OPTION, HotkeyableMenuSwapsConfig::portalNexusDestinationSwapHotKey),
		TELEPORT_MENU(MenuAction.GAME_OBJECT_SECOND_OPTION, HotkeyableMenuSwapsConfig::portalNexusTeleportMenuSwapHotKey),
		CONFIGURATION(MenuAction.GAME_OBJECT_THIRD_OPTION, HotkeyableMenuSwapsConfig::portalNexusConfigurationSwapHotKey),
		;

		private final MenuAction menuAction;
		private final Function<HotkeyableMenuSwapsConfig, Keybind> keybindFunction;

		public Keybind getKeybind(HotkeyableMenuSwapsConfig config) {
			return keybindFunction.apply(config);
		}
	}

	@Getter
	@AllArgsConstructor
	public enum PortalNexusXericsTalismanSwap {
		DESTINATION(MenuAction.GAME_OBJECT_FIRST_OPTION, HotkeyableMenuSwapsConfig::pohXericsTalismanDestination),
		TELEPORT_MENU(MenuAction.GAME_OBJECT_SECOND_OPTION, HotkeyableMenuSwapsConfig::pohXericsTalismanTeleportMenu),
		CONFIGURATION(MenuAction.GAME_OBJECT_THIRD_OPTION, HotkeyableMenuSwapsConfig::pohXericsTalismanConfiguration),
		;

		private final MenuAction menuAction;
		private final Function<HotkeyableMenuSwapsConfig, Keybind> keybindFunction;

		public Keybind getKeybind(HotkeyableMenuSwapsConfig config) {
			return keybindFunction.apply(config);
		}
	}

	@Getter
	@AllArgsConstructor
	public enum PortalNexusDigsitePendantSwap
	{
		DESTINATION(MenuAction.GAME_OBJECT_FIRST_OPTION, HotkeyableMenuSwapsConfig::pohDigsitePendantDestination),
		TELEPORT_MENU(MenuAction.GAME_OBJECT_SECOND_OPTION, HotkeyableMenuSwapsConfig::pohDigsitePendantTeleportMenu),
		CONFIGURATION(MenuAction.GAME_OBJECT_THIRD_OPTION, HotkeyableMenuSwapsConfig::pohDigsitePendantConfiguration),
		;

		private final MenuAction menuAction;
		private final Function<HotkeyableMenuSwapsConfig, Keybind> keybindFunction;

		public Keybind getKeybind(HotkeyableMenuSwapsConfig config) {
			return keybindFunction.apply(config);
		}
	}

	private TreeRingSwap getCurrentTreeRingSwap() {
		return hotkeyTreeRingSwap;
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// not used.
	}

	private volatile int bankSwapVarbit = 0;

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int setting = client.getVarbitValue(6590);

		if (bankSwapVarbit != setting)
		{
			bankSwapVarbit = client.getVarbitValue(6590);
		}
	}

	// indexes correspond to values of the varbit that represents the current left-click option.
	private static final BankSwapMode[] bankSwapModes = {
			BankSwapMode.SWAP_1,
			BankSwapMode.SWAP_5,
			BankSwapMode.SWAP_10,
			BankSwapMode.SWAP_X,
			BankSwapMode.SWAP_ALL
	};

	@Override
	public void keyPressed(KeyEvent e)
	{
		// ignoring the current left click option allows one keybind to be used for two different swaps - e.g. the
		// same keybind for both "1" and "all", which makes bank-1 accessible while the left-click option is set
		// to "all" via the vanilla bank interface, without requiring an additional keybind.
		BankSwapMode currentLeftClick = bankSwapModes[bankSwapVarbit];

		for (BankSwapMode swapMode : BankSwapMode.values()) {
			if (swapMode != currentLeftClick && (swapMode.getKeybind(config)).matches(e)) {
				currentBankModeSwap = swapMode;
				break;
			}
		}

		for (OccultAltarSwap altarOption : OccultAltarSwap.values()) {
			if ((altarOption.getKeybind(config)).matches(e)) {
				hotkeyOccultAltarSwap = altarOption;
				break;
			}
		}

		for (TreeRingSwap treeRingSwap : TreeRingSwap.values()) {
			if ((treeRingSwap.getKeybind(config)).matches(e)) {
				hotkeyTreeRingSwap = treeRingSwap;
				break;
			}
		}

		for (PortalNexusSwap option : PortalNexusSwap.values()) {
			if ((option.getKeybind(config)).matches(e)) {
				swapPortalNexus = option;
				break;
			}
		}

		for (PortalNexusXericsTalismanSwap option : PortalNexusXericsTalismanSwap.values()) {
			if ((option.getKeybind(config)).matches(e)) {
				swapPortalNexusXericsTalisman = option;
				break;
			}
		}

		for (PortalNexusDigsitePendantSwap option : PortalNexusDigsitePendantSwap.values()) {
			if ((option.getKeybind(config)).matches(e)) {
				swapPortalNexusDigsitePendant = option;
				break;
			}
		}

		if (config.getSwapUseHotkey().matches(e)) {
			swapUse = true;
		}

		if (config.getSwapJewelleryBoxHotkey().matches(e)) {
			swapJewelleryBox = true;
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		for (BankSwapMode swapMode : BankSwapMode.values()) {
			if ((swapMode.getKeybind(config)).matches(e) && swapMode == currentBankModeSwap) {
				currentBankModeSwap = BankSwapMode.OFF;
				break;
			}
		}

		for (OccultAltarSwap altarOption : OccultAltarSwap.values()) {
			if ((altarOption.getKeybind(config)).matches(e) && altarOption == hotkeyOccultAltarSwap) {
				hotkeyOccultAltarSwap = null;
				break;
			}
		}

		for (TreeRingSwap treeRingSwap : TreeRingSwap.values()) {
			if ((treeRingSwap.getKeybind(config)).matches(e) && treeRingSwap == hotkeyTreeRingSwap) {
				hotkeyTreeRingSwap = null;
				break;
			}
		}

		for (PortalNexusSwap option : PortalNexusSwap.values()) {
			if ((option.getKeybind(config)).matches(e) && option == swapPortalNexus) {
				swapPortalNexus = null;
				break;
			}
		}

		for (PortalNexusXericsTalismanSwap option : PortalNexusXericsTalismanSwap.values()) {
			if ((option.getKeybind(config)).matches(e) && option == swapPortalNexusXericsTalisman) {
				swapPortalNexusXericsTalisman = null;
				break;
			}
		}

		for (PortalNexusDigsitePendantSwap option : PortalNexusDigsitePendantSwap.values()) {
			if ((option.getKeybind(config)).matches(e) && option == swapPortalNexusDigsitePendant) {
				swapPortalNexusDigsitePendant = null;
				break;
			}
		}

		if (config.getSwapUseHotkey().matches(e)) {
			swapUse = false;
		}

		if (config.getSwapJewelleryBoxHotkey().matches(e)) {
			swapJewelleryBox = false;
		}
	}

	@Subscribe
	public void onFocusChanged(FocusChanged event)
	{
		if (!event.isFocused())
		{
			resetHotkeys();
		}
	}

	private void resetHotkeys()
	{
		currentBankModeSwap = BankSwapMode.OFF;
		hotkeyOccultAltarSwap = null;
		hotkeyTreeRingSwap = null;
		swapUse = false;
		swapJewelleryBox = false;
		swapPortalNexus = null;
		swapPortalNexusXericsTalisman = null;
		swapPortalNexusDigsitePendant = null;
	}

	// Copy-pasted from the official runelite menu entry swapper plugin, with some modification.
	private boolean swapBank(MenuEntry menuEntry, MenuAction type)
	{
		if (type != MenuAction.CC_OP && type != MenuAction.CC_OP_LOW_PRIORITY)
		{
			return false;
		}

		final int widgetGroupId = WidgetInfo.TO_GROUP(menuEntry.getParam1());
		final boolean isDepositBoxPlayerInventory = widgetGroupId == WidgetID.DEPOSIT_BOX_GROUP_ID;
		final boolean isChambersOfXericStorageUnitPlayerInventory = widgetGroupId == WidgetID.CHAMBERS_OF_XERIC_STORAGE_UNIT_INVENTORY_GROUP_ID;
		final boolean isGroupStoragePlayerInventory = widgetGroupId == WidgetID.GROUP_STORAGE_INVENTORY_GROUP_ID;
		// Deposit- op 1 is the current withdraw amount 1/5/10/x for deposit box interface and chambers of xeric storage unit.
		// Deposit- op 2 is the current withdraw amount 1/5/10/x for bank interface
		if (
			currentBankModeSwap != BankSwapMode.OFF && currentBankModeSwap != BankSwapMode.SWAP_ALL_BUT_1
			&& type == MenuAction.CC_OP
			&& menuEntry.getIdentifier() == (isDepositBoxPlayerInventory || isGroupStoragePlayerInventory || isChambersOfXericStorageUnitPlayerInventory ? 1 : 2)
			&& (menuEntry.getOption().startsWith("Deposit-") || menuEntry.getOption().startsWith("Store") || menuEntry.getOption().startsWith("Donate"))
		) {
			final int opId = isDepositBoxPlayerInventory ? currentBankModeSwap.getDepositIdentifierDepositBox()
				: isChambersOfXericStorageUnitPlayerInventory ? currentBankModeSwap.getDepositIdentifierChambersStorageUnit()
				: isGroupStoragePlayerInventory ? currentBankModeSwap.getDepositIdentifierGroupStorage()
				: currentBankModeSwap.getDepositIdentifier();
			final MenuAction action = opId >= 6 ? MenuAction.CC_OP_LOW_PRIORITY : MenuAction.CC_OP;
			bankModeSwap(action, opId);
			return true;
		}

		// Deposit- op 1 is the current withdraw amount 1/5/10/x
		if (
			currentBankModeSwap != BankSwapMode.OFF && currentBankModeSwap != BankSwapMode.SWAP_EXTRA_OP
			&& type == MenuAction.CC_OP && menuEntry.getIdentifier() == 1
			&& menuEntry.getOption().startsWith("Withdraw")
		) {
			final MenuAction action;
			final int opId;
			if (widgetGroupId == WidgetID.CHAMBERS_OF_XERIC_STORAGE_UNIT_PRIVATE_GROUP_ID || widgetGroupId == WidgetID.CHAMBERS_OF_XERIC_STORAGE_UNIT_SHARED_GROUP_ID)
			{
				action = MenuAction.CC_OP;
				opId = currentBankModeSwap.getWithdrawIdentifierChambersStorageUnit();
			}
			else
			{
				action = currentBankModeSwap.getWithdrawMenuAction();
				opId = currentBankModeSwap.getWithdrawIdentifier();
			}
			bankModeSwap(action, opId);
			return true;
		}

		return false;
	}

	// Copy-pasted from the official runelite menu entry swapper plugin.
	private void bankModeSwap(MenuAction entryType, int entryIdentifier)
	{
		MenuEntry[] menuEntries = client.getMenuEntries();

		for (int i = menuEntries.length - 1; i >= 0; --i)
		{
			MenuEntry entry = menuEntries[i];

			if (entry.getType() == entryType && entry.getIdentifier() == entryIdentifier)
			{
				// Raise the priority of the op so it doesn't get sorted later
				entry.setType(MenuAction.CC_OP);

				menuEntries[i] = menuEntries[menuEntries.length - 1];
				menuEntries[menuEntries.length - 1] = entry;

				client.setMenuEntries(menuEntries);
				break;
			}
		}
	}

	// Copy-pasted from the official runelite menu entry swapper plugin.
	private final Multimap<String, Swap> swaps = LinkedHashMultimap.create();
	private final ArrayListMultimap<String, Integer> optionIndexes = ArrayListMultimap.create();

	// Copy-pasted from the official runelite menu entry swapper plugin, with some modification.
	@Subscribe(priority = -1) // This will run after the normal menu entry swapper, so it won't interfere with this plugin.
	public void onPostMenuSort(PostMenuSort e)
	{
		customSwaps();

		MenuEntry[] menuEntries = client.getMenuEntries();

		// Build option map for quick lookup in findIndex
		int idx = 0;
		optionIndexes.clear();
		for (MenuEntry entry : menuEntries)
		{
			String option = Text.removeTags(entry.getOption()).toLowerCase();
			optionIndexes.put(option, idx++);
		}

		// Perform swaps
		for (int i = 0; i < menuEntries.length; i++)
		{
			swapMenuEntry(menuEntries, i);
		}
	}

	// Copy-pasted from the official runelite menu entry swapper plugin.
	@Value
	class Swap
	{
		private Predicate<String> optionPredicate;
		private Predicate<String> targetPredicate;
		private String swappedOption;
		private Supplier<Boolean> enabled;
		private boolean strict;
	}

	/*
	 * Note to self: The way the menu entry swapper does most swaps is that it looks through the menu from bottom (0) to top (menuEntries.length) until it find the option to swap *with* (i.e. the top option e.g. "talk-to"), then it goes back in the menu to find the most recent menuentry that matches the criteria of the *swapped* option (e.g. "teleport").
	 */

	// Copy-pasted from the official runelite menu entry swapper plugin, with some modification.
	private void swapMenuEntry(MenuEntry[] menuEntries, int index)
	{
		MenuEntry menuEntry = menuEntries[index];
		String option = Text.removeTags(menuEntry.getOption()).toLowerCase();
		String target = Text.removeTags(menuEntry.getTarget()).toLowerCase();
		MenuAction menuAction = menuEntry.getType();

		if (swapBank(menuEntry, menuAction))
		{
			return;
		}

		if (menuAction == WIDGET_TARGET)
		{
			if (swapUse && option.equals("use"))
			{
				swap(optionIndexes, menuEntries, index, menuEntries.length - 1);
			}
			return;
		}

		if (target.equals("portal nexus") && swapPortalNexus != null) {
			boolean vanillaMesSwapEnabled = vanillaPortalNexusSwapEnabled();
			boolean hasLeftClickTeleportConfigured = menuEntry.getIdentifier() < 33408 || menuEntry.getIdentifier() > 33410;
			boolean destinationIsLeftClick = !vanillaMesSwapEnabled && hasLeftClickTeleportConfigured;
			if (destinationIsLeftClick && menuAction == MenuAction.GAME_OBJECT_FIRST_OPTION)
			{
				swap(swapPortalNexus.menuAction, target, index);
				return;
			} else if (!destinationIsLeftClick && menuAction == MenuAction.GAME_OBJECT_SECOND_OPTION) {
				swap(swapPortalNexus.menuAction, target, index);
				return;
			}
		}

		if (target.equals("xeric's talisman")) {
			PortalNexusXericsTalismanSwap portalNexusXericsTalismanSwap = getPortalNexusXericsTalismanSwap();
			if (portalNexusXericsTalismanSwap != null)
			{
				// https://chisel.weirdgloop.org/moid/object_name.html#/xeric's%20talisman/
				boolean hasLeftClickTeleportConfigured = menuEntry.getIdentifier() != 33419;
				if (hasLeftClickTeleportConfigured && menuAction == MenuAction.GAME_OBJECT_FIRST_OPTION)
				{
					swap(portalNexusXericsTalismanSwap.menuAction, target, index);
					return;
				}
				else if (!hasLeftClickTeleportConfigured && menuAction == MenuAction.GAME_OBJECT_SECOND_OPTION)
				{
					swap(portalNexusXericsTalismanSwap.menuAction, target, index);
					return;
				}
			}
		}

		if (target.equals("digsite pendant")) {
			PortalNexusDigsitePendantSwap portalNexusDigsitePendantSwap = getPortalNexusDigsitePendantSwap();
			if (portalNexusDigsitePendantSwap != null)
			{
				// https://chisel.weirdgloop.org/moid/object_name.html#/digsite%20pendant/
				boolean hasLeftClickTeleportConfigured = menuEntry.getIdentifier() != 33420;
				if (hasLeftClickTeleportConfigured && menuAction == MenuAction.GAME_OBJECT_FIRST_OPTION)
				{
					swap(portalNexusDigsitePendantSwap.menuAction, target, index);
					return;
				}
				else if (!hasLeftClickTeleportConfigured && menuAction == MenuAction.GAME_OBJECT_SECOND_OPTION)
				{
					swap(portalNexusDigsitePendantSwap.menuAction, target, index);
					return;
				}
			}
		}

		if (menuAction == MenuAction.GAME_OBJECT_SECOND_OPTION && target.endsWith("jewellery box") && swapJewelleryBoxSpecificOption()) { // second option is teleport menu
			swap(MenuAction.GAME_OBJECT_THIRD_OPTION, target, index);
			return;
		} else if (menuAction == MenuAction.GAME_OBJECT_THIRD_OPTION && target.endsWith("jewellery box") && swapJewelleryBoxTeleportMenuOption()) { // third option is the specific option (e.g. "Edgeville")
			swap(MenuAction.GAME_OBJECT_SECOND_OPTION, target, index);
			return;
		}

		// disgusting. But there isn't an easy way to implement some kind of custom predicate function or regex for the menu entry option without rewriting a lot of code.
		if (target.equals("fairy ring") || target.equals("spiritual fairy tree")) {
			if (option.startsWith("ring-")) {
				option = option.substring("ring-".length());
			}
			if (option.startsWith("last-destination")) {
				option = "last-destination";
			}
		}

		Collection<Swap> swaps = this.swaps.get(option);
		for (Swap swap : swaps)
		{
			if (swap.getTargetPredicate().test(target) && swap.getEnabled().get())
			{
				if (swap(swap.getSwappedOption(), target, index, swap.isStrict()))
				{
					break;
				}
			}
		}
	}

	// Copy-pasted from the official runelite menu entry swapper plugin.
	private void swap(String option, Predicate<String> targetPredicate, String swappedOption, Supplier<Boolean> enabled)
	{
		swaps.put(option, new Swap(alwaysTrue(), targetPredicate, swappedOption, enabled, true));
	}

	// Copy-pasted from the official runelite menu entry swapper plugin.
	private boolean swap(String option, String target, int index, boolean strict)
	{
		MenuEntry[] menuEntries = client.getMenuEntries();

		// find option to swap with
		int optionIdx = findIndex(menuEntries, index, option, target, strict);

		if (optionIdx >= 0)
		{
			swap(optionIndexes, menuEntries, optionIdx, index);
			return true;
		}

		return false;
	}

	private boolean swap(MenuAction menuAction, String target, int index)
	{
		MenuEntry[] menuEntries = client.getMenuEntries();

		// find option to swap with
		int optionIdx = findIndex(menuEntries, index, menuAction, target);

		if (optionIdx >= 0)
		{
			swap(optionIndexes, menuEntries, optionIdx, index);
			return true;
		}

		return false;
	}

	private int findIndex(MenuEntry[] entries, int limit, MenuAction menuAction, String target)
	{
		// We want the last index which matches the target, as that is what is top-most
		// on the menu
		for (int i = limit - 1; i >= 0; --i)
		{
			MenuEntry entry = entries[i];

			if (entry.getType() != menuAction) continue;

			String entryTarget = Text.removeTags(entry.getTarget()).toLowerCase();
			if (entryTarget.equals(target))
			{
				return i;
			}
		}

		return -1;
	}

	// Copy-pasted from the official runelite menu entry swapper plugin.
	private int findIndex(MenuEntry[] entries, int limit, String option, String target, boolean strict)
	{
		if (strict)
		{
			List<Integer> indexes = optionIndexes.get(option.toLowerCase());

			// We want the last index which matches the target, as that is what is top-most
			// on the menu
			for (int i = indexes.size() - 1; i >= 0; --i)
			{
				int idx = indexes.get(i);
				MenuEntry entry = entries[idx];
				String entryTarget = Text.removeTags(entry.getTarget()).toLowerCase();

				// Limit to the last index which is prior to the current entry
				if (idx < limit && entryTarget.equals(target))
				{
					return idx;
				}
			}
		}
		else
		{
			// Without strict matching we have to iterate all entries up to the current limit...
			for (int i = limit - 1; i >= 0; i--)
			{
				MenuEntry entry = entries[i];
				String entryOption = Text.removeTags(entry.getOption()).toLowerCase();
				String entryTarget = Text.removeTags(entry.getTarget()).toLowerCase();

				if (entryOption.contains(option.toLowerCase()) && entryTarget.equals(target))
				{
					return i;
				}
			}

		}

		return -1;
	}

	// Copy-pasted from the official runelite menu entry swapper plugin.
	private void swap(ArrayListMultimap<String, Integer> optionIndexes, MenuEntry[] entries, int index1, int index2)
	{
		MenuEntry entry1 = entries[index1],
				entry2 = entries[index2];

		entries[index1] = entry2;
		entries[index2] = entry1;

		client.setMenuEntries(entries);

		// Update optionIndexes
		String option1 = Text.removeTags(entry1.getOption()).toLowerCase(),
				option2 = Text.removeTags(entry2.getOption()).toLowerCase();

		List<Integer> list1 = optionIndexes.get(option1),
				list2 = optionIndexes.get(option2);

		// call remove(Object) instead of remove(int)
		list1.remove((Integer) index1);
		list2.remove((Integer) index2);

		sortedInsert(list1, index2);
		sortedInsert(list2, index1);
	}

	// Copy-pasted from the official runelite menu entry swapper plugin.
	private static <T extends Comparable<? super T>> void sortedInsert(List<T> list, T value) // NOPMD: UnusedPrivateMethod: false positive
	{
		int idx = Collections.binarySearch(list, value);
		list.add(idx < 0 ? -idx - 1 : idx, value);
	}

	// Copy-pasted from the official runelite menu entry swapper plugin.
	private void swapContains(String option, Predicate<String> targetPredicate, String swappedOption, Supplier<Boolean> enabled)
	{
		swaps.put(option, new Swap(alwaysTrue(), targetPredicate, swappedOption, enabled, false));
	}

	@Getter
	@ToString
	@RequiredArgsConstructor
	@EqualsAndHashCode
	static class CustomSwap
	{
		private final String option;
		private final String target;
		private final String topOption;
		private final String topTarget;

		private final Type optionType;
		private final Type targetType;
		private final Type topOptionType;
		private final Type topTargetType;

		private enum Type {
			EQUALS,
			STARTS_WITH,
			ENDS_WITH,
			CONTAINS,
			WILDCARD,
			IGNORE
		}

		public static CustomSwap fromString(String s)
		{
			String[] split = s.split(",");
			return new CustomSwap(
				split[0].toLowerCase().trim(),
				split.length > 1 ? split[1].toLowerCase().trim() : "",
				split.length > 2 ? split[2].toLowerCase().trim() : null,
				split.length > 3 ? split[3].toLowerCase().trim() : null
			);
		}

		CustomSwap(String option, String target)
		{
			this(option, target, null, null);
		}

		CustomSwap(String option, String target, String topOption, String topTarget)
		{
			this.optionType = getType(option);
			this.option = prepareMatch(option, optionType);
			this.targetType = getType(target);
			this.target = prepareMatch(target, targetType);
			this.topOptionType = getType(topOption);
			this.topOption = prepareMatch(topOption, topOptionType);
			this.topTargetType = getType(topTarget);
			this.topTarget = prepareMatch(topTarget, topTargetType);
//			System.out.println("types for " + option + " " + target + " "+ topOption + " " + topTarget + " " + optionType + " " + targetType + " " + topOptionType + " " + topTargetType);
		}

		private static Type getType(String s)
		{
			if (s == null) return Type.IGNORE;

			int star = s.indexOf('*');
			if (star == -1) return Type.EQUALS;
			if (star == 0) {
				if (s.length() == 1) return Type.IGNORE;
				star = s.indexOf('*', star + 1);
				if (star == -1) return Type.ENDS_WITH;
				if (star == s.length() - 1) return Type.CONTAINS;
			} else if (star == s.length() - 1) {
				return Type.STARTS_WITH;
			}

			return Type.WILDCARD;
		}

		private String prepareMatch(String option, Type optionType)
		{
			return optionType == Type.WILDCARD ? generateWildcardMatcher(option) : removeStars(option);
		}

		// copied from runelite.
		private static final Pattern WILDCARD_PATTERN = Pattern.compile("(?i)[^*]+|(\\*)");
		private static String generateWildcardMatcher(String pattern)
		{
			final Matcher matcher = WILDCARD_PATTERN.matcher(pattern);
			final StringBuffer buffer = new StringBuffer();

			buffer.append("(?i)");
			while (matcher.find())
			{
				if (matcher.group(1) != null)
				{
					matcher.appendReplacement(buffer, ".*");
				}
				else
				{
					matcher.appendReplacement(buffer, Matcher.quoteReplacement(Pattern.quote(matcher.group(0))));
				}
			}

			matcher.appendTail(buffer);
			return buffer.toString();
		}

		private String removeStars(String s)
		{
			return s == null ? s : s.replaceAll("\\*", "");
		}

		/** skip top option comparison, for testing */
		boolean matches(String option, String target)
		{
			return matches(option, target, "", "");
		}

		public boolean matches(String option, String target, String topOption, String topTarget)
		{
			return matches(option, this.option, optionType) &&
				matches(target, this.target, targetType) &&
				matches(topOption, this.topOption, topOptionType) &&
				matches(topTarget, this.topTarget, topTargetType);
		}

		private static boolean matches(String menuText, String swapText, Type type)
		{
			switch (type) {
				case IGNORE:
					return true;
				case EQUALS:
					return menuText.equals(swapText);
				case STARTS_WITH:
					return menuText.startsWith(swapText);
				case ENDS_WITH:
					return menuText.endsWith(swapText);
				case CONTAINS:
					return menuText.contains(swapText);
				case WILDCARD:
					return menuText.matches(swapText);
				default:
					// shouldn't happen.
					throw new IllegalStateException();
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged) {
		if (configChanged.getGroup().equals("hotkeyablemenuswaps")) {
			reloadCustomSwaps();
		}
	}

	private void reloadCustomSwaps()
	{
		customSwaps.clear();
		customSwaps.addAll(loadCustomSwaps(config.customSwaps()));

		customShiftSwaps.clear();
		customShiftSwaps.addAll(loadCustomSwaps(config.customShiftSwaps()));

		customHides.clear();
		customHides.addAll(loadCustomSwaps(config.customHides()));
	}

	private Collection<? extends CustomSwap> loadCustomSwaps(String customSwaps)
	{
		List<CustomSwap> swaps = new ArrayList<>();
		for (String customSwap : customSwaps.split("\n"))
		{
			if (customSwap.trim().equals("")) continue;
			swaps.add(CustomSwap.fromString(customSwap));
		}
		return swaps;
	}

	private boolean shiftModifier()
	{
		return client.isKeyPressed(KeyCode.KC_SHIFT);
	}

	public void customSwaps()
	{
		MenuEntry[] menuEntries = client.getMenuEntries();
		if (menuEntries.length == 0) return;

		menuEntries = filterEntries(menuEntries);
		int topEntryIndex = menuEntries.length - 1;
		if (topEntryIndex == -1) { // The filtering removed all the menu options. No swaps can happen, so return early.
			client.setMenuEntries(menuEntries);
			return;
		}
		MenuEntry topEntry = menuEntries[topEntryIndex];
		if (mayNotBeLeftClick(topEntry)) {
			return;
		}

		int entryIndex = getEntryIndexToSwap(menuEntries, shiftModifier() ? customShiftSwaps : customSwaps);
		if (entryIndex >= 0)
		{
			MenuEntry entryToSwap = menuEntries[entryIndex];
			if (isProtected(topEntry) || isProtected(entryToSwap)) {
				return;
			}

			// the client will open the right-click menu on left-click if the entry at the top is a CC_OP_LOW_PRIORITY.
			if (entryToSwap.getType() == MenuAction.CC_OP_LOW_PRIORITY)
			{
				entryToSwap.setType(MenuAction.CC_OP);
			}

			if (topEntryIndex > entryIndex)
			{
				menuEntries[topEntryIndex] = entryToSwap;
				menuEntries[entryIndex] = topEntry;
			}
		}

		client.setMenuEntries(menuEntries);
	}

	private int getEntryIndexToSwap(MenuEntry[] menuEntries, List<CustomSwap> swaps)
	{
		int entryIndex = -1;
		int latestMatchingSwapIndex = -1;
		MenuEntry topEntry = menuEntries[menuEntries.length - 1];
		String topEntryOption = Text.standardize(topEntry.getOption());
		String topEntryTarget = Text.standardize(topEntry.getTarget());
		// prefer to swap menu entries that are already at or near the top of the list.
		for (int i = menuEntries.length - 1; i >= 0; i--)
		{
			MenuEntry entry = menuEntries[i];

			String option = Text.standardize(entry.getOption());
			String target = Text.standardize(entry.getTarget());
			int swapIndex = matches(option, target, topEntryOption, topEntryTarget, swaps);
			if (swapIndex > latestMatchingSwapIndex)
			{
				entryIndex = i;
				latestMatchingSwapIndex = swapIndex;
			}
		}
		return entryIndex;
	}

	private boolean isProtected(MenuEntry entry)
	{
		return false;
	}

	private boolean mayNotBeLeftClick(MenuEntry entry)
	{
		return false;
	}

	private MenuEntry[] filterEntries(MenuEntry[] menuEntries)
	{
		List<MenuEntry> filtered = new ArrayList<>();
		MenuEntry topEntry = menuEntries[menuEntries.length - 1];
		String topEntryOption = Text.standardize(topEntry.getOption());
		String topEntryTarget = Text.standardize(topEntry.getTarget());
		for (MenuEntry entry : menuEntries)
		{
			String option = Text.standardize(Text.removeTags(entry.getOption()));
			String target = Text.standardize(Text.removeTags(entry.getTarget()));
			if (matches(option, target, topEntryOption, topEntryTarget, customHides) == -1)
			{
				filtered.add(entry);
			}
		}
		return filtered.toArray(new MenuEntry[0]);
	}

	private static int matches(String entryOption, String entryTarget, String topEntryOption, String topEntryTarget, List<CustomSwap> swaps)
	{
		for (int i = 0; i < swaps.size(); i++)
		{
			CustomSwap swap = swaps.get(i);
			if (swap.matches(entryOption, entryTarget, topEntryOption, topEntryTarget)) {
				return i;
			}
		}
		return -1;
	}
}
