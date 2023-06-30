package net.runelite.client.plugins.resourcepacks;

import lombok.Value;

import java.util.List;

@Value
public class ResourcePacksChanged
{
	List<ResourcePackManifest> newManifest;
}
