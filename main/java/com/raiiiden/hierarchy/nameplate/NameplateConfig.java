package com.raiiiden.hierarchy.nameplate;

import net.minecraftforge.common.ForgeConfigSpec;

public final class NameplateConfig {
  public static final ForgeConfigSpec SPEC;
  public static final ForgeConfigSpec.BooleanValue ENABLE_CUSTOM_NAMEPLATES;
  public static final ForgeConfigSpec.BooleanValue SHOW_CLAN_TAG;
  public static final ForgeConfigSpec.BooleanValue SHOW_HUMANITY;
  public static final ForgeConfigSpec.BooleanValue SHOW_BOUNTY_ICON;
  public static final ForgeConfigSpec.DoubleValue MAX_RENDER_DISTANCE_BLOCKS;
  public static final ForgeConfigSpec.BooleanValue REQUIRE_LINE_OF_SIGHT;

  static {
    ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    builder.push("nameplates");
    ENABLE_CUSTOM_NAMEPLATES = builder.define("enableCustomNameplates", true);
    SHOW_CLAN_TAG = builder.define("showClanTag", true);
    SHOW_HUMANITY = builder.define("showHumanity", true);
    SHOW_BOUNTY_ICON = builder.comment("Show a red bounty icon next to players with an active bounty.").define("showBountyIcon", true);
    MAX_RENDER_DISTANCE_BLOCKS = builder.comment("Maximum distance in blocks where custom nameplates are visible.").defineInRange("maxRenderDistanceBlocks", 3.0D, 0.0D, 128.0D);
    REQUIRE_LINE_OF_SIGHT = builder.comment("When true, custom nameplates are hidden behind blocks.").define("requireLineOfSight", true);
    builder.pop();
    SPEC = builder.build();
  }

  private NameplateConfig() {
  }
}
