package net.runelite.client.owo.instruction;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private Integer x;
    private Integer y;
    private Integer radius;
    private Integer waitMinMillis;
    private Integer waitMaxMillis;
    private Integer isShift;
    private String clickButton;
    private Integer speed;

    // Drop
    private Integer keepTools;

    // Pray

    // Type
    private List<String> keys;
}
