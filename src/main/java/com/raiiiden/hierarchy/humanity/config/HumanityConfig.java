package com.raiiiden.hierarchy.humanity.config;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public final class HumanityConfig {
  public static final ForgeConfigSpec SPEC;
  public static final ForgeConfigSpec.BooleanValue ENABLE_HUMANITY;
  public static final ForgeConfigSpec.DoubleValue LOW_HUMANITY_BOUNTY_THRESHOLD;
  public static final ForgeConfigSpec.DoubleValue HIGH_HUMANITY_PROTECTED_THRESHOLD;
  public static final ForgeConfigSpec.DoubleValue MIN_HUMANITY;
  public static final ForgeConfigSpec.DoubleValue MAX_HUMANITY;
  public static final ForgeConfigSpec.DoubleValue PLAYER_KILL_HUMANITY_LOSS;
  public static final ForgeConfigSpec.DoubleValue PLAYER_KILL_LOW_STANDING_LOSS_ATTACKER_FIRST;
  public static final ForgeConfigSpec.DoubleValue PLAYER_KILL_NEUTRAL_STANDING_LOSS_ATTACKER_FIRST;
  public static final ForgeConfigSpec.DoubleValue PLAYER_KILL_HIGH_STANDING_LOSS_ATTACKER_FIRST;
  public static final ForgeConfigSpec.DoubleValue PLAYER_KILL_LOW_STANDING_LOSS_SELF_DEFENSE;
  public static final ForgeConfigSpec.DoubleValue PLAYER_KILL_NEUTRAL_STANDING_LOSS_SELF_DEFENSE;
  public static final ForgeConfigSpec.DoubleValue PLAYER_KILL_HIGH_STANDING_LOSS_SELF_DEFENSE;
  public static final ForgeConfigSpec.LongValue SELF_DEFENSE_WINDOW_SECONDS;
  public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOB_KILL_HUMANITY_GAINS;
  public static final ForgeConfigSpec.DoubleValue BOUNTY_CLAIM_HUMANITY_GAIN;
  public static final ForgeConfigSpec.BooleanValue DECAY_ON_WIPE;
  public static final ForgeConfigSpec.BooleanValue ENABLE_RANK_NOTIFICATIONS;
  public static final ForgeConfigSpec.ConfigValue<String> RANK_CHANGE_MESSAGE;
  public static final ForgeConfigSpec.IntValue HISTORY_MAX_ENTRIES;
  public static final ForgeConfigSpec.IntValue HISTORY_DEFAULT_DISPLAY;
  public static final ForgeConfigSpec.ConfigValue<String> HERO_LABEL;
  public static final ForgeConfigSpec.ConfigValue<String> BANDIT_LABEL;
  public static final ForgeConfigSpec.ConfigValue<String> NEUTRAL_LABEL;

  static {
    ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    builder.push("humanity");
    ENABLE_HUMANITY = builder.define("enableHumanity", true);
    MIN_HUMANITY = builder.defineInRange("minHumanity", -100.0, -Double.MAX_VALUE, Double.MAX_VALUE);
    MAX_HUMANITY = builder.defineInRange("maxHumanity", 100.0, -Double.MAX_VALUE, Double.MAX_VALUE);
    LOW_HUMANITY_BOUNTY_THRESHOLD = builder.defineInRange("lowHumanityBountyThreshold", -100.0, -Double.MAX_VALUE, Double.MAX_VALUE);
    HIGH_HUMANITY_PROTECTED_THRESHOLD = builder.defineInRange("highHumanityProtectedThreshold", 100.0, -Double.MAX_VALUE, Double.MAX_VALUE);
    PLAYER_KILL_HUMANITY_LOSS = builder.comment("Legacy setting kept for old configs. Use the per-standing player kill loss settings below.").defineInRange("playerKillHumanityLoss", 25.0, 0.0, Double.MAX_VALUE);
    SELF_DEFENSE_WINDOW_SECONDS = builder.comment("How long after the first PvP hit a kill counts as attacker-first or self-defense.").defineInRange("selfDefenseWindowSeconds", 300L, 1L, 86400L);
    PLAYER_KILL_LOW_STANDING_LOSS_ATTACKER_FIRST = builder.comment("Humanity lost when you kill a low-standing player and you attacked first.").defineInRange("playerKillLowStandingLossAttackerFirst", 10.0, 0.0, Double.MAX_VALUE);
    PLAYER_KILL_NEUTRAL_STANDING_LOSS_ATTACKER_FIRST = builder.comment("Humanity lost when you kill a neutral-standing player and you attacked first.").defineInRange("playerKillNeutralStandingLossAttackerFirst", 10.0, 0.0, Double.MAX_VALUE);
    PLAYER_KILL_HIGH_STANDING_LOSS_ATTACKER_FIRST = builder.comment("Humanity lost when you kill a high-standing player and you attacked first.").defineInRange("playerKillHighStandingLossAttackerFirst", 15.0, 0.0, Double.MAX_VALUE);
    PLAYER_KILL_LOW_STANDING_LOSS_SELF_DEFENSE = builder.comment("Humanity lost when you kill a low-standing player and they attacked first.").defineInRange("playerKillLowStandingLossSelfDefense", 0.0, 0.0, Double.MAX_VALUE);
    PLAYER_KILL_NEUTRAL_STANDING_LOSS_SELF_DEFENSE = builder.comment("Humanity lost when you kill a neutral-standing player and they attacked first.").defineInRange("playerKillNeutralStandingLossSelfDefense", 0.0, 0.0, Double.MAX_VALUE);
    PLAYER_KILL_HIGH_STANDING_LOSS_SELF_DEFENSE = builder.comment("Humanity lost when you kill a high-standing player and they attacked first.").defineInRange("playerKillHighStandingLossSelfDefense", 5.0, 0.0, Double.MAX_VALUE);
    MOB_KILL_HUMANITY_GAINS = builder.comment(
                    "Entity kill humanity gains, formatted as entity_id=amount.",
                    "Examples: minecraft:zombie=1.0, minecraft:skeleton=0.5, minecraft:pillager=3.0")
            .defineList("mobKillHumanityGains", List.of("minecraft:zombie=0.5"), HumanityConfig::isMobKillGainEntry);
    BOUNTY_CLAIM_HUMANITY_GAIN = builder.defineInRange("bountyClaimHumanityGain", 10.0, 0.0, Double.MAX_VALUE);
    DECAY_ON_WIPE = builder.comment("Intended wipe behavior flag. Humanity is stored in world data, so deleting world data resets it.").define("decayOnWipe", true);
    ENABLE_RANK_NOTIFICATIONS = builder.comment("Send an action-bar message when a player's displayed humanity rank changes.").define("enableRankNotifications", true);
    RANK_CHANGE_MESSAGE = builder.comment("Rank-change action-bar message. {rank} is replaced with the new rank, e.g. 'Hero IV'.").define("rankChangeMessage", "You have become {rank}.");
    HISTORY_MAX_ENTRIES = builder.comment("Maximum humanity history entries stored per player.").defineInRange("historyMaxEntries", 50, 1, 1000);
    HISTORY_DEFAULT_DISPLAY = builder.comment("Default number of humanity history entries shown by /humanity history.").defineInRange("historyDefaultDisplay", 10, 1, 1000);
    HERO_LABEL = builder.comment("Display label for positive-humanity ranks.").define("heroLabel", "Hero");
    BANDIT_LABEL = builder.comment("Display label for negative-humanity ranks.").define("banditLabel", "Bandit");
    NEUTRAL_LABEL = builder.comment("Display label for zero/neutral humanity.").define("neutralLabel", "Neutral");
    builder.pop();
    SPEC = builder.build();
  }

  private static boolean isMobKillGainEntry(Object value) {
    if (!(value instanceof String entry)) return false;
    int separator = entry.lastIndexOf('=');
    if (separator <= 0 || separator >= entry.length() - 1) return false;
    String entityId = entry.substring(0, separator).trim();
    String amount = entry.substring(separator + 1).trim();
    if (!entityId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) return false;
    try {
      return Double.parseDouble(amount) > 0.0;
    } catch (NumberFormatException ignored) {
      return false;
    }
  }

  private HumanityConfig() {}
}
