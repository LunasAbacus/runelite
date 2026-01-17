package net.runelite.client.owo.instruction;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Keep all values as integers or strings to work well with AHK parsing and maps
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstructionParameters {
    // Click
    private int x;
    private int y;
    private int radius;
    private int waitMinMillis;
    private int waitMaxMillis;
    private int isShift;
    private String clickButton;
    private int speed;

    // Drop
    private int keepTools;

    // Pray

    // Type
    private String keys;
}
