package net.runelite.client.owo.instruction;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class Command {

    List<Instruction> instructions;
    UUID id;

    public Command(List<Instruction> instructions) {
        this.instructions = instructions;
        this.id = UUID.randomUUID();
    }
}
