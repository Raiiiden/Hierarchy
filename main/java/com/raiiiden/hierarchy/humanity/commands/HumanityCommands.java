package com.raiiiden.hierarchy.humanity.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.raiiiden.hierarchy.humanity.HumanityRank;
import com.raiiiden.hierarchy.humanity.config.HumanityConfig;
import com.raiiiden.hierarchy.humanity.data.HumanityData;
import com.raiiiden.hierarchy.humanity.data.HumanityHistoryData;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class HumanityCommands {
  private HumanityCommands() {}

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(Commands.literal("humanity")
        .requires(source -> source.getEntity() instanceof ServerPlayer && HumanityConfig.ENABLE_HUMANITY.get())
        .executes(ctx -> show(ctx.getSource()))
        .then(Commands.literal("history")
            .executes(ctx -> history(ctx.getSource(), HumanityConfig.HISTORY_DEFAULT_DISPLAY.get()))
            .then(Commands.argument("count", IntegerArgumentType.integer(1))
                .executes(ctx -> history(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count"))))));
  }

  private static int show(CommandSourceStack source) {
    ServerPlayer player = playerOrNull(source);
    if (player == null) {
      return 0;
    }
    double value = HumanityData.get(source.getServer()).humanity(player.getUUID());
    source.sendSuccess(() -> Component.literal("Humanity").withStyle(ChatFormatting.GOLD), false);
    source.sendSuccess(() -> Component.literal("Rank: ").withStyle(ChatFormatting.GRAY)
            .append(HumanityRank.styled(value)), false);
    source.sendSuccess(() -> Component.literal(String.format("Value: %.1f", value)).withStyle(ChatFormatting.WHITE), false);
    source.sendSuccess(() -> Component.literal(standingDescription(value)).withStyle(ChatFormatting.GRAY), false);
    return 1;
  }

  private static int history(CommandSourceStack source, int count) {
    ServerPlayer player = playerOrNull(source);
    if (player == null) {
      return 0;
    }
    List<HumanityHistoryData.Entry> entries =
            HumanityHistoryData.get(source.getServer()).recent(player.getUUID(), count);
    if (entries.isEmpty()) {
      source.sendSuccess(() -> Component.literal("No humanity history yet.").withStyle(ChatFormatting.GRAY), false);
      return 1;
    }
    source.sendSuccess(() -> Component.literal("Recent humanity changes:").withStyle(ChatFormatting.GOLD), false);
    long now = System.currentTimeMillis();
    for (HumanityHistoryData.Entry entry : entries) {
      String sign = entry.delta() >= 0 ? "+" : "";
      ChatFormatting color = entry.delta() >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
      String text = String.format("%s%.1f %s", sign, entry.delta(), entry.reason());
      String ago = formatAgo(now - entry.timestamp());
      source.sendSuccess(() -> Component.literal(text).withStyle(color)
              .append(Component.literal(" (" + ago + " ago)").withStyle(ChatFormatting.DARK_GRAY)), false);
    }
    return entries.size();
  }

  private static ServerPlayer playerOrNull(CommandSourceStack source) {
    try {
      return source.getPlayerOrException();
    } catch (Exception ex) {
      source.sendFailure(Component.literal("This command must be run by a player."));
      return null;
    }
  }

  private static String standingDescription(double value) {
    int v = (int) Math.round(value);
    if (v == 0) {
      return "You walk the line between hero and villain.";
    }
    return v > 0 ? "You are seen as a force for good." : "You are feared as an outlaw.";
  }

  private static String formatAgo(long millis) {
    long totalSeconds = Math.max(0L, millis / 1000L);
    long days = totalSeconds / 86400L;
    long hours = (totalSeconds % 86400L) / 3600L;
    long minutes = (totalSeconds % 3600L) / 60L;
    long seconds = totalSeconds % 60L;
    if (days > 0) return days + "d " + hours + "h";
    if (hours > 0) return hours + "h " + minutes + "m";
    if (minutes > 0) return minutes + "m " + seconds + "s";
    return seconds + "s";
  }
}
