package net.saint.createrenderfixer;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = Mod.MOD_ID)
public class ModConfig implements ConfigData {

	// Instances

	@ConfigEntry.Category("instances")
	@Comment("Enable state caching on supported dynamic instances to prevent re-render when state is unmodified. (Default: true)")
	public boolean cacheDynamicInstances = true;

	@ConfigEntry.Category("instances")
	@Comment("Freeze dynamic instances once they're above a certain block distance from the player. (Default: true)")
	public boolean freezeDistantInstances = true;

	@ConfigEntry.Category("instances")
	@Comment("Distance in blocks to freeze dynamic instances. Recommended < 64 to cut in before Create limits tick rate. (Default: 62)")
	public int freezeDistantInstancesRange = 62;

	@ConfigEntry.Category("instances")
	@Comment("Freeze dynamic instances when they're in an occluded chunk. Not effective due to subpar engine occlusion checks. (Default: false)")
	public boolean freezeOccludedInstances = false;

	@ConfigEntry.Category("instances")
	@Comment("Force-disables the tick-based rate limiter on Create dynamic instances. Generally not needed or effective. (Default: false)")
	public boolean forceDisableRateLimiting = false;

	@ConfigEntry.Category("instances")
	@Comment("Blacklist of contraptions to exclude from instance freezing. Comma-separated list of ids.")
	public String freezeInstanceBlacklist = "create:windmill_bearing";

	// LODs

	@ConfigEntry.Category("lods")
	@Comment("Enable injection of Create contraption blocks for LOD building with Distant Horizons. (Default: true)")
	public boolean injectContraptionLODs = true;

	// Entities

	@ConfigEntry.Category("entities")
	@Comment("Limit entity render distance. (Default: true)")
	public boolean limitEntityRenderDistance = true;

	@ConfigEntry.Category("entities")
	@Comment("Apply entity render distance limit to all entities, not just Create entities. (Default: false)")
	public boolean limitEntityRenderDistanceAppliesToAll = false;

	@ConfigEntry.Category("entities")
	@Comment("Offset added to entity LOD distance thresholds. (Default: 0)")
	public int entityLODDistanceOffset = 0;

	@ConfigEntry.Category("entities")
	@Comment("Limit block entity render distance to respect LOD thresholds. (Default: true)")
	public boolean limitBlockEntityRenderDistance = true;

	@ConfigEntry.Category("entities")
	@Comment("Offset added to block entity LOD distance thresholds. (Default: 0)")
	public int blockEntityLODDistanceOffset = 0;

}
