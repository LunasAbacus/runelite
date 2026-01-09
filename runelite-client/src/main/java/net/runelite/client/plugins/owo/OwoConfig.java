package net.runelite.client.plugins.owo;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.owo.LogicType;

@ConfigGroup("owo")
public interface OwoConfig extends Config {
    @ConfigItem(
            keyName = "logicType",
            name = "Logic Type to run",
            description = "Configures which logic type will run",
            position = 0
    )
    default LogicType logicType() {
        return LogicType.NO_OP;
    }
}
