package net.runelite.client.owo.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ItemAmount {
    private int itemId;
    private int inventoryCount;
    private int stackSize;

    public static ItemAmount ofCount(int itemId, int count) {
        return new ItemAmount(itemId, count, 1);
    }

    public static ItemAmount ofStack(int itemId, int stackSize) {
        return new ItemAmount(itemId, 1, stackSize);
    }
}
