package com.raiiiden.hierarchy.humanity.config;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public final class HumanityConfig {
  public static final ForgeConfigSpec SPEC;
  public static final ForgeConfigSpec.BooleanValue ENABLE_HUMANITY;
  public static final ForgeConfigSpec.IntValue LOW_HUMANITY_BOUNTY_THRESHOLD;
  public static final ForgeConfigSpec.IntValue HIGH_HUMANITY_PROTECTED_THRESHOLD;
  public static final ForgeConfigSpec.IntValue MIN_HUMANITY;
  public static final ForgeConfigSpec.IntValue MAX_HUMANITY;
  public static final ForgeConfigSpec.IntValue PLAYER_KILL_HUMANITY_LOSS;
  public static final ForgeConfigSpec.IntValue PLAYER_KILL_LOW_STANDING_LOSS_ATTACKER_FIRST;
  public static final ForgeConfigSpec.IntValue PLAYER_KILL_NEUTRAL_STANDING_LOSS_ATTACKER_FIRST;
  public static final ForgeConfigSpec.IntValue PLAYER_KILL_HIGH_STANDING_LOSS_ATTACKER_FIRST;
  public static final ForgeConfigSpec.IntValue PLAYER_KILL_LOW_STANDING_LOSS_SELF_DEFENSE;
  public static final ForgeConfigSpec.IntValue PLAYER_KILL_NEUTRAL_STANDING_LOSS_SELF_DEFENSE;
  public static final ForgeConfigSpec.IntValue PLAYER_KILL_HIGH_STANDING_LOSS_SELF_DEFENSE;
  public static final ForgeConfigSpec.LongValue SELF_DEFENSE_WINDOW_SECONDS;
  public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOB_KILL_HUMANITY_GAINS;
  public static final ForgeConfigSpec.IntValue BOUNTY_CLAIM_HUMANITY_GAIN;
  public static final ForgeConfigSpec.BooleanValue DECAY_ON_WIPE;

  static {
    ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    builder.push("humanity");
    ENABLE_HUMANITY = builder.define("enableHumanity", true);
    MIN_HUMANITY = builder.defineInRange("minHumanity", -100, Integer.MIN_VALUE, Integer.MAX_VALUE);
    MAX_HUMANITY = builder.defineInRange("maxHumanity", 100, Integer.MIN_VALUE, Integer.MAX_VALUE);
    LOW_HUMANITY_BOUNTY_THRESHOLD = builder.defineInRange("lowHumanityBountyThreshold", -100, Integer.MIN_VALUE, Integer.MAX_VALUE);
    HIGH_HUMANITY_PROTECTED_THRESHOLD = builder.defineInRange("highHumanityProtectedThreshold", 100, Integer.MIN_VALUE, Integer.MAX_VALUE);
    PLAYER_KILL_HUMANITY_LOSS = builder.comment("Legacy setting kept for old configs. Use the per-standing player kill loss settings below.").defineInRange("playerKillHumanityLoss", 25, 0, Integer.MAX_VALUE);
    SELF_DEFENSE_WINDOW_SECONDS = builder.comment("How long after the first PvP hit a kill counts as attacker-first or self-defense.").defineInRange("selfDefenseWindowSeconds", 300L, 1L, 86400L);
    PLAYER_KILL_LOW_STANDING_LOSS_ATTACKER_FIRST = builder.comment("Humanity lost when you kill a low-standing player and you attacked first.").defineInRange("playerKillLowStandingLossAttackerFirst", 10, 0, Integer.MAX_VALUE);
    PLAYER_KILL_NEUTRAL_STANDING_LOSS_ATTACKER_FIRST = builder.comment("Humanity lost when you kill a neutral-standing player and you attacked first.").defineInRange("playerKillNeutralStandingLossAttackerFirst", 10, 0, Integer.MAX_VALUE);
    PLAYER_KILL_HIGH_STANDING_LOSS_ATTACKER_FIRST = builder.comment("Humanity lost when you kill a high-standing player and you attacked first.").defineInRange("playerKillHighStandingLossAttackerFirst", 5, 0, Integer.MAX_VALUE);
    PLAYER_KILL_LOW_STANDING_LOSS_SELF_DEFENSE = builder.comment("Humanity lost when you kill a low-standing player and they attacked first.").defineInRange("playerKillLowStandingLossSelfDefense", 0, 0, Integer.MAX_VALUE);
    PLAYER_KILL_NEUTRAL_STANDING_LOSS_SELF_DEFENSE = builder.comment("Humanity lost when you kill a neutral-standing player and they attacked first.").defineInRange("playerKillNeutralStandingLossSelfDefense", 0, 0, Integer.MAX_VALUE);
    PLAYER_KILL_HIGH_STANDING_LOSS_SELF_DEFENSE = builder.comment("Humanity lost when you kill a high-standing player and they attacked first.").defineInRange("playerKillHighStandingLossSelfDefense", 0, 0, Integer.MAX_VALUE);
    MOB_KILL_HUMANITY_GAINS = builder.comment(
        "Entity kill humanity gains, formatted as entity_id=amount.",
        "Examples: minecraft:zombie=1, minecraft:skeleton=1, minecraft:pillager=3")
        .defineList("mobKillHumanityGains", List.of(), HumanityConfig::isMobKillGainEntry);
    BOUNTY_CLAIM_HUMANITY_GAIN = builder.defineInRange("bountyClaimHumanityGain", 10, 0, Integer.MAX_VALUE);
    DECAY_ON_WIPE = builder.comment("Intended wipe behavior flag. Humanity is stored in world data, so deleting world data resets it.").define("decayOnWipe", true);
    builder.pop();
    SPEC = builder.build();
  }

  private static boolean isMobKillGainEntry(Object value) {
    if (!(value instanceof String entry)) {
      return false;
    }
    int separator = entry.lastIndexOf('=');
    if (separator <= 0 || separator >= entry.length() - 1) {
      return false;
    }
    String entityId = entry.substring(0, separator).trim();
    String amount = entry.substring(separator + 1).trim();
    if (!entityId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
      return false;
    }
    try {
      return Integer.parseInt(amount) > 0;
    } catch (NumberFormatException ignored) {
      return false;
    }
  }

  private HumanityConfig() {
  }
}
