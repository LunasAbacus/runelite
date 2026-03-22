package net.runelite.client.owo.instruction;

import net.runelite.api.Point;

import java.util.ArrayList;
import java.util.List;

public class InstructionFactory {
    private static final int tickMillis = 600;
    public static final String RIGHT_CLICK_BUTTON = "Right";
    public static final String Left_CLICK_BUTTON = "Left";
    public static final String MIDDLE_CLICK_BUTTON = "Middle";

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

    public static Command createHoverCommand(Point point) {
        return new Command(List.of(createClickInstruction(point.getX(), point.getY(), MIDDLE_CLICK_BUTTON)));
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
        return createClickInstruction(x, y, Left_CLICK_BUTTON);
    }

    public static Instruction createClickInstruction(int x, int y, String clickButton) {
        InstructionParameters instructionParameters = InstructionParameters.builder()
                .x(x)
                .y(y)
                .clickButton(clickButton)
                .build();
        return new Instruction(InstructionType.CLICK, instructionParameters);
    }

    public static Instruction createClickInstruction(Point point) {
        return createClickInstruction(point.getX(), point.getY());
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
        return new Command(List.of(createTypeInstruction(key)));
    }

    public static Instruction createTypeInstruction(String key) {
        InstructionParameters typeParams = InstructionParameters.builder()
                .keys(List.of(key))
                .build();
        return new Instruction(InstructionType.TYPE, typeParams);
    }

    /**
     * Create command to sleep for 1-2 ticks
     */
    public static Command createDefaultIdle() {
        return new Command(List.of(createIdleByMillisInstruction(tickMillis, tickMillis * 2)));
    }

    public static Command createBankTransactionCommand(List<Point> points) {
        List<Instruction> instructions = new ArrayList<>();

        for (Point point : points) {
            instructions.add(createClickInstruction(point));
        }

        // Wait for withdraw action to complete before closing
        instructions.add(createIdleByMillisInstruction(300, 600));

        instructions.add(createTypeInstruction("{Esc}"));

        return new Command(instructions);
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
