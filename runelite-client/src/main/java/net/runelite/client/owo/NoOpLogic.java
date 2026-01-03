package net.runelite.client.owo;

import net.runelite.api.Client;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.owo.instruction.Instruction;
import net.runelite.client.owo.instruction.InstructionParameters;
import net.runelite.client.owo.instruction.InstructionType;

import java.util.List;

public class NoOpLogic extends OwoLogic {
    public NoOpLogic(OwoServer server, Client client) {
        super(server, client);

        Command command = new Command(List.of(new Instruction(InstructionType.IDLE, new InstructionParameters())));
        server.updateCommand(command);
    }
}
