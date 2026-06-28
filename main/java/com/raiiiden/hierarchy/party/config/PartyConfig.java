package com.raiiiden.hierarchy.party.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class PartyConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PARTIES;
    public static final ForgeConfigSpec.IntValue MAX_PARTY_MEMBERS;
    public static final ForgeConfigSpec.IntValue INVITE_EXPIRY_SECONDS;
    public static final ForgeConfigSpec.DoubleValue PARTY_RENDER_DISTANCE_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue SHOW_PARTY_INDICATOR;
    public static final ForgeConfigSpec.ConfigValue<String> PARTY_INDICATOR_COLOR;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("parties");
        ENABLE_PARTIES = builder
                .comment("Enable the party system and /party commands.")
                .define("enableParties", true);
        MAX_PARTY_MEMBERS = builder
                .comment("Maximum number of members per party (including the leader).")
                .defineInRange("maxPartyMembers", 4, 2, 100);
        INVITE_EXPIRY_SECONDS = builder
                .comment("Seconds before a pending party invite expires (real time).")
                .defineInRange("inviteExpirySeconds", 60, 5, 3600);
        PARTY_RENDER_DISTANCE_BLOCKS = builder
                .comment("Maximum distance in blocks at which the party indicator (◆/★) is visible.")
                .defineInRange("partyRenderDistanceBlocks", 64.0D, 0.0D, 256.0D);
        SHOW_PARTY_INDICATOR = builder
                .comment("Show ◆ above party members and ★ above the party leader when they are not a clan teammate.")
                .define("showPartyIndicator", true);
        PARTY_INDICATOR_COLOR = builder
                .comment("ChatFormatting color name for the party member indicator (◆). " +
                        "The party leader always uses GOLD (★). " +
                        "Valid values: BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE, " +
                        "GOLD, GRAY, DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE, YELLOW, WHITE.")
                .define("partyIndicatorColor", "LIGHT_PURPLE");
        builder.pop();
        SPEC = builder.build();
    }

    private PartyConfig() {}
}