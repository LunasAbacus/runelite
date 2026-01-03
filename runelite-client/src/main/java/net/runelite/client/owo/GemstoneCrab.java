package net.runelite.client.owo;

import net.runelite.api.Client;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.owo.instruction.Instruction;
import net.runelite.client.owo.instruction.InstructionParameters;
import net.runelite.client.owo.instruction.InstructionType;

import java.util.List;

public class GemstoneCrab extends OwoLogic {

    public GemstoneCrab(OwoServer server, Client client) {
        super(server, client);

        Command command = new Command(List.of(new Instruction(InstructionType.IDLE, new InstructionParameters(1, 1, 1, 1, 1))));
        server.updateCommand(command);
    }
}
