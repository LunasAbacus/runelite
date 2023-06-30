package net.runelite.client.plugins.resourcepacks;

import lombok.Data;

import javax.annotation.Nullable;
import java.net.URL;

@Data
public class ResourcePackManifest
{
	private final String internalName;
	private final String commit;

	private final String displayName;
	private final String compatibleVersion;
	private final String author;
	@Nullable
	private final String[] tags;
	private final URL repo;
	private final boolean hasIcon;

	@Override
	public String toString()
	{
		return displayName;
	}
}
