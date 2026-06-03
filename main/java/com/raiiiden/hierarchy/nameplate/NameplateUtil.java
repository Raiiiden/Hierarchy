package com.raiiiden.hierarchy.nameplate;

import com.raiiiden.hierarchy.bounty.config.BountyConfig;
import com.raiiiden.hierarchy.bounty.data.BountyData;
import com.raiiiden.hierarchy.clan.config.ClanCombatConfig;
import com.raiiiden.hierarchy.clan.data.ClanData;
import com.raiiiden.hierarchy.clan.model.Clan;
import com.raiiiden.hierarchy.humanity.config.HumanityConfig;
import com.raiiiden.hierarchy.humanity.data.HumanityData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class NameplateUtil {
  private static final String[] ROMAN = {"I", "II", "III", "IV", "V"};

  private NameplateUtil() {}

  public static void refresh(ServerPlayer player) {
    if (!NameplateConfig.ENABLE_CUSTOM_NAMEPLATES.get()) {
      player.setCustomName(null);
      player.setCustomNameVisible(false);
      return;
    }
    player.setCustomName(build(player.server, player.getUUID().toString(), player.getGameProfile().getName()));
    player.setCustomNameVisible(true);
  }

  public static void refreshAll(MinecraftServer server) {
    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      refresh(player);
    }
  }

  public static Component clientNameplate(Player player, boolean isTeammate) {
    if (!player.hasCustomName()) return player.getName();
    // Arrow is now a separate world-space render — no longer prepended here
    return player.getCustomName();
  }

  public static Component clientNameplate(Player player) {
    return clientNameplate(player, false);
  }

  private static Component build(MinecraftServer server, String playerUuid, String username) {
    java.util.UUID id = java.util.UUID.fromString(playerUuid);
    MutableComponent component = Component.empty();

    if (NameplateConfig.SHOW_CLAN_TAG.get() && ClanCombatConfig.ENABLE_CLANS.get()) {
      Clan clan = ClanData.get(server).clanOf(id).orElse(null);
      if (clan != null && !clan.getTag().isBlank()) {
        component.append(Component.literal("[" + clan.getTag() + "] ").withStyle(ChatFormatting.GREEN));
      }
    }

    component.append(Component.literal(username).withStyle(ChatFormatting.GRAY));

    // Only call this once, using humanityDisplay() which returns int
    if (NameplateConfig.SHOW_HUMANITY.get() && HumanityConfig.ENABLE_HUMANITY.get()) {
      component.append(Component.literal(" "))
              .append(humanityLabel(HumanityData.get(server).humanityDisplay(id)));
    }

    if (NameplateConfig.SHOW_BOUNTY_ICON.get() && BountyConfig.ENABLE_BOUNTIES.get()) {
      if (BountyData.get(server).hasActiveBounty(id, System.currentTimeMillis())) {
        component.append(Component.literal(" ✛").withStyle(ChatFormatting.RED));
      }
    }

    return component;
  }

  private static Component humanityLabel(int humanity) {
    // Cast to int since MIN/MAX are now DoubleValue
    int min = HumanityConfig.MIN_HUMANITY.get().intValue();
    int max = HumanityConfig.MAX_HUMANITY.get().intValue();
    if (humanity < 0) {
      int level = level(Math.abs(humanity), Math.max(1, Math.abs(min)));
      return Component.literal("Bandit " + ROMAN[level - 1]).withStyle(ChatFormatting.RED);
    }
    int level = level(humanity, Math.max(1, max));
    return Component.literal("Hero " + ROMAN[level - 1]).withStyle(ChatFormatting.AQUA);
  }

  private static int level(int value, int range) {
    if (value <= 0) return 1;
    return Math.max(1, Math.min(5, (int) Math.ceil(value / (range / 5.0D))));
  }
}