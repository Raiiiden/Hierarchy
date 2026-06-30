package com.raiiiden.hierarchy.humanity;

import com.raiiiden.hierarchy.humanity.config.HumanityConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Single source of truth for the displayed humanity rank (e.g. "Hero IV", "Bandit II",
 * "Neutral"). Ranks are derived from the rounded humanity value against the configured
 * MIN/MAX range, split into five tiers each side of zero. Labels are configurable.
 */
public final class HumanityRank {
  private static final String[] ROMAN = {"I", "II", "III", "IV", "V"};

  private HumanityRank() {}

  /** Human-readable rank for a humanity value. Uses the rounded value so it matches nameplates. */
  public static String displayName(double value) {
    if (!HumanityConfig.ENABLE_HUMANITY.get()) {
      return HumanityConfig.NEUTRAL_LABEL.get();
    }
    int v = (int) Math.round(value);
    if (v == 0) {
      return HumanityConfig.NEUTRAL_LABEL.get();
    }
    if (v < 0) {
      int min = HumanityConfig.MIN_HUMANITY.get().intValue();
      int tier = tier(Math.abs(v), Math.max(1, Math.abs(min)));
      return HumanityConfig.BANDIT_LABEL.get() + " " + ROMAN[tier - 1];
    }
    int max = HumanityConfig.MAX_HUMANITY.get().intValue();
    int tier = tier(v, Math.max(1, max));
    return HumanityConfig.HERO_LABEL.get() + " " + ROMAN[tier - 1];
  }

  /** Rank color: aqua for heroes, red for bandits, gray for neutral. */
  public static ChatFormatting color(double value) {
    int v = (int) Math.round(value);
    if (v == 0) {
      return ChatFormatting.GRAY;
    }
    return v < 0 ? ChatFormatting.RED : ChatFormatting.AQUA;
  }

  /** Styled component combining {@link #displayName} and {@link #color}. */
  public static Component styled(double value) {
    return Component.literal(displayName(value)).withStyle(color(value));
  }

  private static int tier(int value, int range) {
    if (value <= 0) {
      return 1;
    }
    return Math.max(1, Math.min(5, (int) Math.ceil(value / (range / 5.0D))));
  }
}
