package net.runelite.client.owo.instruction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructionParameters {
    private int xCoord;
    private int yCoord;
    private int radius;
    private int waitMinMillis;
    private int waitMaxMillis;
}
