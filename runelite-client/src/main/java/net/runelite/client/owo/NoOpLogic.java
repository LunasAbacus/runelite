package net.runelite.client.owo;

import net.runelite.client.owo.instruction.Command;
import net.runelite.client.plugins.owo.OwoPlugin;

public class NoOpLogic extends OwoLogic {
    public NoOpLogic(OwoPlugin plugin) {
        super(plugin);

        Command command = InstructionFactory.createDefaultIdle();
        server.updateCommand(command);
    }
}
