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
  public static final ForgeConfigSpec.DoubleValue TEAMMATE_RENDER_DISTANCE_BLOCKS;
  public static final ForgeConfigSpec.BooleanValue SHOW_TEAMMATE_ARROW;
  public static final ForgeConfigSpec.DoubleValue ARROW_Y_OFFSET;
  public static final ForgeConfigSpec.DoubleValue ARROW_SCALE;
  public static final ForgeConfigSpec.DoubleValue ARROW_RENDER_DISTANCE_BLOCKS;

  static {
    ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    builder.push("nameplates");
    ENABLE_CUSTOM_NAMEPLATES = builder.define("enableCustomNameplates", true);
    SHOW_CLAN_TAG = builder.define("showClanTag", true);
    SHOW_HUMANITY = builder.define("showHumanity", true);
    SHOW_TEAMMATE_ARROW = builder.comment("Show a downward arrow (▼) above clan teammates.").define("showTeammateArrow", true);
    ARROW_Y_OFFSET = builder.comment("How many blocks above the nameplate the arrow floats.")
            .defineInRange("arrowYOffset", 0.4D, 0.0D, 4.0D);
    ARROW_SCALE = builder.comment("Scale of the teammate arrow glyph (nameplate text is ~0.025).")
            .defineInRange("arrowScale", 0.055D, 0.01D, 0.2D);
    ARROW_RENDER_DISTANCE_BLOCKS = builder.comment("Max distance the teammate arrow is visible. Independent of nameplate distance.")
            .defineInRange("arrowRenderDistanceBlocks", 100.0D, 0.0D, 256.0D);
    TEAMMATE_RENDER_DISTANCE_BLOCKS = builder.comment("Extended nameplate distance for clan teammates.").defineInRange("teammateRenderDistanceBlocks", 16.0D, 0.0D, 256.0D);
    SHOW_BOUNTY_ICON = builder.comment("Show a red bounty icon next to players with an active bounty.").define("showBountyIcon", true);
    MAX_RENDER_DISTANCE_BLOCKS = builder.comment("Maximum distance in blocks where custom nameplates are visible.").defineInRange("maxRenderDistanceBlocks", 5.0D, 0.0D, 128.0D);
    REQUIRE_LINE_OF_SIGHT = builder.comment("When true, custom nameplates are hidden behind blocks.").define("requireLineOfSight", true);
    builder.pop();
    SPEC = builder.build();
  }

  private NameplateConfig() {
  }
}
