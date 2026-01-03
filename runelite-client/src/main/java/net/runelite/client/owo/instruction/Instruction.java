package net.runelite.client.owo.instruction;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Instruction {
    private InstructionType type;
    private InstructionParameters parameters;
}
