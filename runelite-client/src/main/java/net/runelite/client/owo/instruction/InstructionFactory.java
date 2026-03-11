package net.runelite.client.owo.instruction;

import net.runelite.api.Point;

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
        return new Command(List.of(createClickInstruction(x, y)));
    }

    public static Command createClickCommand(Point point) {
        return createClickCommand(point.getX(), point.getY());
    }

    public static Command createShiftClickCommand(int x, int y) {
        return createShiftClickCommand(x, y, 1);
    }

    /**
     * Create command to click at the current coordinates and waits for 1-2 ticks
     * @param x Coordinate x to click
     * @param y Coordinate y to click
     * @param sleepTicks number of ticks to sleep
     */
    public static Command createClickCommand(int x, int y, int sleepTicks) {
        return new Command(List.of(
                createClickInstruction(x, y),
                createIdleByMillisInstruction(sleepTicks * tickMillis, sleepTicks * tickMillis + tickMillis)
        ));
    }

    public static Instruction createClickInstruction(int x, int y) {
        InstructionParameters instructionParameters = InstructionParameters.builder()
                .x(x)
                .y(y)
                .clickButton(Left_CLICK_BUTTON)
                .build();
        return new Instruction(InstructionType.CLICK, instructionParameters);
    }

    public static Command createShiftClickCommand(int x, int y, int sleepTicks) {
        return new Command(List.of(
                createShiftClickInstruction(x, y),
                createIdleByMillisInstruction(sleepTicks * tickMillis, sleepTicks * tickMillis + tickMillis)
        ));
    }

    public static Instruction createShiftClickInstruction(int x, int y) {
        InstructionParameters instructionParameters = InstructionParameters.builder()
                .x(x)
                .y(y)
                .isShift(1)
                .clickButton(Left_CLICK_BUTTON)
                .build();
        return new Instruction(InstructionType.CLICK, instructionParameters);
    }

    public static Command createSimpleIdleCommand(int minWaitMillis, int maxWaitMillis) {
        return new Command(List.of(createIdleByMillisInstruction(minWaitMillis, maxWaitMillis)));
    }

    public static Instruction createIdleByTicksInstruction(int tickMin, int tickMax) {
        InstructionParameters instructionParameters = InstructionParameters.builder()
                .waitMinMillis(tickMin * tickMillis)
                .waitMaxMillis(tickMax * tickMillis)
                .build();
        return new Instruction(InstructionType.SLEEP, instructionParameters);
    }

    public static Instruction createIdleByMillisInstruction(int minWaitMillis, int maxWaitMillis) {
        InstructionParameters instructionParameters = InstructionParameters.builder()
                .waitMinMillis(minWaitMillis)
                .waitMaxMillis(maxWaitMillis)
                .build();
        return new Instruction(InstructionType.SLEEP, instructionParameters);
    }

    public static Command createTypeCommand(String key) {
        InstructionParameters typeParams = InstructionParameters.builder()
                .keys(List.of(key))
                .build();
        Instruction typeInstruction = new Instruction(InstructionType.TYPE, typeParams);
        return new Command(List.of(typeInstruction));
    }

    /**
     * Create command to sleep for 1-2 ticks
     */
    public static Command createDefaultIdle() {
        return new Command(List.of(createIdleByMillisInstruction(tickMillis, tickMillis * 2)));
    }

    public static Command createBankDepositCommand(int x, int y) {
        // Click deposit button + Hit escape key
        InstructionParameters clickParams = InstructionParameters.builder()
                .x(x)
                .y(y)
                .clickButton(Left_CLICK_BUTTON)
                .build();
        Instruction deposit = new Instruction(InstructionType.CLICK, clickParams);

        InstructionParameters afterClickWaitParams = InstructionParameters.builder()
                .waitMinMillis(200)
                .waitMaxMillis(500)
                .build();
        Instruction afterClickWait = new Instruction(InstructionType.SLEEP, afterClickWaitParams);

        InstructionParameters typeParams = InstructionParameters.builder()
                .keys(List.of("{Esc}"))
                .build();
        Instruction escape = new Instruction(InstructionType.TYPE, typeParams);

        InstructionParameters waitParams = InstructionParameters.builder()
                .waitMinMillis(tickMillis)
                .waitMaxMillis(tickMillis * 2)
                .build();
        Instruction wait = new Instruction(InstructionType.SLEEP, waitParams);

        return new Command(List.of(deposit, afterClickWait, escape, wait));
    }

    public static Command createClickAndConfirmCommand(int x, int y, int tickWait) {
        InstructionParameters clickParams = InstructionParameters.builder()
                .x(x)
                .y(y)
                .clickButton(Left_CLICK_BUTTON)
                .build();
        Instruction click = new Instruction(InstructionType.CLICK, clickParams);

        InstructionParameters afterClickWaitParams = InstructionParameters.builder()
                .waitMinMillis(tickMillis * tickWait)
                .waitMaxMillis(tickMillis * tickWait + tickMillis)
                .build();
        Instruction afterClickWait = new Instruction(InstructionType.SLEEP, afterClickWaitParams);

        InstructionParameters typeParams = InstructionParameters.builder()
                .keys(List.of(" "))
                .build();
        Instruction confirm = new Instruction(InstructionType.TYPE, typeParams);

        InstructionParameters waitParams = InstructionParameters.builder()
                .waitMinMillis(tickMillis * 3)
                .waitMaxMillis(tickMillis * 6)
                .build();
        Instruction wait = new Instruction(InstructionType.SLEEP, waitParams);

        return new Command(List.of(click, afterClickWait, confirm, wait));
    }
}
