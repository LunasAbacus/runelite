package net.runelite.client.owo;

import net.runelite.client.owo.instruction.Command;
import net.runelite.client.owo.instruction.Instruction;
import net.runelite.client.owo.instruction.InstructionParameters;
import net.runelite.client.owo.instruction.InstructionType;

import java.util.List;

public class InstructionFactory {
    private static final int tickMillis = 600;
    public static final String RIGHT_CLICK_BUTTON = "Right";
    public static final String Left_CLICK_BUTTON = "Left";

    /**
     * Create command to click at the current coordinates and waits for 1-2 ticks
     * @param x Coordinate x to click
     * @param y Coordinate y to click
     */
    public static Command createClickCommand(int x, int y) {
        InstructionParameters instructionParameters = InstructionParameters.builder()
                .x(x)
                .y(y)
                .clickButton(Left_CLICK_BUTTON)
                .waitMinMillis(tickMillis)
                .waitMaxMillis(tickMillis * 2)
                .build();
        return new Command(List.of(new Instruction(InstructionType.CLICK, instructionParameters)));
    }

    public static Command createSimpleIdleCommand(int minWait, int maxWait) {
        InstructionParameters instructionParameters = InstructionParameters.builder()
                .waitMinMillis(minWait)
                .waitMaxMillis(maxWait)
                .build();
        return new Command(List.of(new Instruction(InstructionType.SLEEP, instructionParameters)));
    }

    /**
     * Create command to sleep for 1-2 ticks
     */
    public static Command createDefaultIdle() {
        InstructionParameters instructionParameters = InstructionParameters.builder()
                .waitMinMillis(tickMillis)
                .waitMaxMillis(tickMillis * 2)
                .build();
        return new Command(List.of(new Instruction(InstructionType.SLEEP, instructionParameters)));
    }

    public static Command createBankDepositCommand(int x, int y) {
        // Click deposit button + Hit escape key
        InstructionParameters clickParams = InstructionParameters.builder()
                .x(x)
                .y(y)
                .clickButton(Left_CLICK_BUTTON)
                .waitMinMillis(200)
                .waitMaxMillis(500)
                .build();
        Instruction deposit = new Instruction(InstructionType.CLICK, clickParams);

        InstructionParameters typeParams = InstructionParameters.builder()
                .keys(List.of("{Esc}"))
                .build();
        Instruction escape = new Instruction(InstructionType.TYPE, typeParams);

        InstructionParameters waitParams = InstructionParameters.builder()
                .waitMinMillis(tickMillis)
                .waitMaxMillis(tickMillis * 2)
                .build();
        Instruction wait = new Instruction(InstructionType.SLEEP, waitParams);

        return new Command(List.of(deposit, escape, wait));
    }
}
