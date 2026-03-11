package net.runelite.client.owo.logics;

import net.runelite.client.owo.instruction.InstructionFactory;
import net.runelite.client.owo.OwoLogic;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.plugins.owo.OwoPlugin;

public class NoOpLogic extends OwoLogic {
    public NoOpLogic(OwoPlugin plugin) {
        super(plugin);

        Command command = InstructionFactory.createDefaultIdle();
        server.updateCommand(command);
    }
}
