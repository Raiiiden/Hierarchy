package com.raiiiden.hierarchy.bounty.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BountyConfig {
  public static final ForgeConfigSpec SPEC;
  public static final ForgeConfigSpec.BooleanValue ENABLE_BOUNTIES;
  public static final ForgeConfigSpec.BooleanValue ENABLE_DOG_TAGS;
  public static final ForgeConfigSpec.ConfigValue<String> ACCESS_MODE;
  public static final ForgeConfigSpec.ConfigValue<String> NPC_ONLY_BLOCKED_MESSAGE;
  public static final ForgeConfigSpec.DoubleValue TAX_PERCENT;
  public static final ForgeConfigSpec.LongValue DURATION_SECONDS;
  public static final ForgeConfigSpec.LongValue COOLDOWN_SECONDS;
  public static final ForgeConfigSpec.BooleanValue PAUSE_BOUNTY_WHILE_TARGET_OFFLINE;
  public static final ForgeConfigSpec.BooleanValue PAUSE_COOLDOWN_WHILE_OFFLINE;
  public static final ForgeConfigSpec.LongValue RECENT_PLAYER_KILL_ELIGIBILITY_SECONDS;
  public static final ForgeConfigSpec.DoubleValue MAX_ELIGIBLE_HUMANITY;
  public static final ForgeConfigSpec.LongValue MIN_BOUNTY_AMOUNT;
  public static final ForgeConfigSpec.LongValue PLACE_CURRENCY_COST;
  public static final ForgeConfigSpec.LongValue CONTRIBUTE_CURRENCY_COST;
  public static final ForgeConfigSpec.LongValue CLAIM_CURRENCY_COST;
  public static final ForgeConfigSpec.IntValue PLACE_XP_COST;
  public static final ForgeConfigSpec.IntValue CONTRIBUTE_XP_COST;
  public static final ForgeConfigSpec.IntValue CLAIM_XP_COST;

  static {
    ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    builder.push("bounties");
    ENABLE_BOUNTIES = builder.define("enableBounties", true);
    ENABLE_DOG_TAGS = builder.define("enableDogTags", true);
    ACCESS_MODE = builder
            .comment("Controls how players access the bounty board.",
                    "ANYWHERE  - /bounty commands work normally (default).",
                    "NPC_ONLY  - /bounty place/contribute/list/info are blocked; players must use a Bounty NPC.",
                    "           /bounty redeem always works regardless of this setting.",
                    "           Use /hierarchy openbounty <player> from an NPC or command block to open the GUI.")
            .define("accessMode", "ANYWHERE");
    NPC_ONLY_BLOCKED_MESSAGE = builder
            .comment("Message shown when a player uses a blocked /bounty command in NPC_ONLY mode.")
            .define("npcOnlyBlockedMessage", "Visit a Bounty NPC.");
    TAX_PERCENT = builder.defineInRange("taxPercent", 0.0D, 0.0D, 100.0D);
    DURATION_SECONDS = builder.defineInRange("bountyDurationSeconds", 86400L, 1L, 31536000L);
    COOLDOWN_SECONDS = builder.defineInRange("postBountyCooldownSeconds", 3600L, 0L, 31536000L);
    PAUSE_BOUNTY_WHILE_TARGET_OFFLINE = builder.define("pauseBountyWhileTargetOffline", true);
    PAUSE_COOLDOWN_WHILE_OFFLINE = builder.comment("Defaults false: cooldowns continue while offline.").define("pauseCooldownWhileOffline", false);
    RECENT_PLAYER_KILL_ELIGIBILITY_SECONDS = builder.defineInRange("recentPlayerKillEligibilitySeconds", 1800L, 1L, 604800L);
    MAX_ELIGIBLE_HUMANITY = builder.comment("Players at or below this humanity can receive bounties. Default 50 covers Hero I through Bandit V on the default -100..100 scale.").defineInRange("maxEligibleHumanity", 50.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
    MIN_BOUNTY_AMOUNT = builder.comment("Legacy amount setting kept for old configs; item bounties use reward slots.").defineInRange("minimumBountyAmount", 1L, 1L, Long.MAX_VALUE);
    builder.pop();
    builder.push("bountyCurrencyCosts");
    PLACE_CURRENCY_COST = builder.comment("Bounty placement cost when common playerCurrencyType is item. Uses the common configured currency item.").defineInRange("place", 10L, 0L, Long.MAX_VALUE);
    CONTRIBUTE_CURRENCY_COST = builder.defineInRange("contribute", 0L, 0L, Long.MAX_VALUE);
    CLAIM_CURRENCY_COST = builder.defineInRange("claim", 0L, 0L, Long.MAX_VALUE);
    builder.pop();
    builder.push("bountyXpCosts");
    PLACE_XP_COST = builder.comment("Bounty placement cost when common playerCurrencyType is xp. Cost is XP levels.").defineInRange("place", 5, 0, Integer.MAX_VALUE);
    CONTRIBUTE_XP_COST = builder.defineInRange("contribute", 0, 0, Integer.MAX_VALUE);
    CLAIM_XP_COST = builder.defineInRange("claim", 0, 0, Integer.MAX_VALUE);
    builder.pop();
    SPEC = builder.build();
  }

  private BountyConfig() {
  }
  public static boolean isNpcOnly() {
    return "NPC_ONLY".equalsIgnoreCase(ACCESS_MODE.get().trim());
  }
}