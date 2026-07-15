package com.raiiiden.hierarchy.bounty.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.raiiiden.hierarchy.bounty.config.BountyConfig;
import com.raiiiden.hierarchy.bounty.data.BountyData;
import com.raiiiden.hierarchy.bounty.gui.BountyMenus;
import com.raiiiden.hierarchy.bounty.model.Bounty;
import java.util.Comparator;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class BountyCommands {
  private BountyCommands() {
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(Commands.literal("bounty")
            .requires(source -> BountyConfig.ENABLE_BOUNTIES.get())
            .executes(ctx -> openMain(ctx.getSource(), player(ctx.getSource())))
            .then(Commands.literal("help").executes(ctx -> help(ctx.getSource())))
            .then(Commands.literal("list").executes(ctx -> openList(ctx.getSource())))
            .then(Commands.literal("info").then(Commands.argument("target", EntityArgument.player()).executes(ctx -> info(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
            .then(Commands.literal("place")
                    .requires(source -> source.getEntity() instanceof ServerPlayer)
                    .executes(ctx -> openPlace(ctx.getSource(), player(ctx.getSource()))))
            .then(Commands.literal("contribute")
                    .requires(source -> source.getEntity() instanceof ServerPlayer)
                    .executes(ctx -> openContribute(ctx.getSource(), player(ctx.getSource()))))
            .then(Commands.literal("redeem")
                    .requires(source -> source.getEntity() instanceof ServerPlayer)
                    .executes(ctx -> openRedeem(ctx.getSource(), player(ctx.getSource())))));
  }

  // -------------------------------------------------------------------------
  // Command handlers
  // -------------------------------------------------------------------------

  private static int help(CommandSourceStack source) {
    source.sendSuccess(() -> Component.literal("Bounty commands:").withStyle(ChatFormatting.GOLD), false);
    String[][] entries = {
        {"/bounty", "Open the bounty board"},
        {"/bounty list", "List active bounties"},
        {"/bounty info <player>", "Show a player's bounty"},
        {"/bounty place", "Place a new bounty"},
        {"/bounty contribute", "Add to an existing bounty"},
        {"/bounty redeem", "Redeem rewards you've earned"},
    };
    for (String[] entry : entries) {
      source.sendSuccess(() -> Component.literal(entry[0]).withStyle(ChatFormatting.YELLOW)
              .append(Component.literal(" — " + entry[1]).withStyle(ChatFormatting.GRAY)), false);
    }
    return 1;
  }

  // /bounty — opens the main bounty board. Blocked in NPC_ONLY mode.
  private static int openMain(CommandSourceStack source, ServerPlayer player) {
    if (!accessAllowed(source)) return 0;
    BountyMenus.open(player);
    return 1;
  }

  // /bounty place — opens player-selection for placing a new bounty. Blocked in NPC_ONLY mode.
  private static int openPlace(CommandSourceStack source, ServerPlayer player) {
    if (!accessAllowed(source)) return 0;
    BountyMenus.openPlayerSelection(player, false, 0);
    return 1;
  }

  // /bounty contribute — opens player-selection for contributing to an existing bounty. Blocked in NPC_ONLY mode.
  private static int openContribute(CommandSourceStack source, ServerPlayer player) {
    if (!accessAllowed(source)) return 0;
    BountyMenus.openPlayerSelection(player, true, 0);
    return 1;
  }

  // /bounty redeem — always allowed regardless of access mode.
  private static int openRedeem(CommandSourceStack source, ServerPlayer player) {
    BountyMenus.openRedemption(player);
    return 1;
  }

  // /bounty list — opens the active-bounty board (or prints to console). Blocked in NPC_ONLY mode.
  private static int openList(CommandSourceStack source) {
    if (!accessAllowed(source)) return 0;
    if (source.getEntity() instanceof ServerPlayer player) {
      BountyMenus.openActiveBoard(player, 0);
      return 1;
    }
    return list(source);
  }

  // /bounty info <target> — prints bounty info as chat. Blocked in NPC_ONLY mode.
  private static int info(CommandSourceStack source, ServerPlayer target) {
    if (!accessAllowed(source)) return 0;
    long now = System.currentTimeMillis();
    BountyData data = BountyData.get(source.getServer());
    data.expireIfNeeded(target.getUUID(), now, source.getServer());
    Bounty bounty = data.bounty(target.getUUID()).orElse(null);
    if (bounty == null) {
      return fail(source, "That target does not have an active bounty.");
    }
    long seconds = Math.max(0L, (bounty.expiresAt() - now) / 1000L);
    return ok(source, bounty.targetName() + " bounty: " + bounty.rewards().size() + " reward item(s) (" + seconds + "s remaining).");
  }

  // -------------------------------------------------------------------------
  // Internal helpers
  // -------------------------------------------------------------------------

  // Returns true if the command source is allowed to access the bounty board via commands.
  // In NPC_ONLY mode, all commands except /bounty redeem are blocked and this returns false.
  private static boolean accessAllowed(CommandSourceStack source) {
    if (BountyConfig.isNpcOnly()) {
      source.sendFailure(Component.literal(BountyConfig.NPC_ONLY_BLOCKED_MESSAGE.get()).withStyle(ChatFormatting.RED));
      return false;
    }
    return true;
  }

  private static int list(CommandSourceStack source) {
    BountyData data = BountyData.get(source.getServer());
    data.expireAll(System.currentTimeMillis(), source.getServer());
    String bounties = data.activeBounties().stream()
            .sorted(Comparator.comparing(Bounty::targetName, String.CASE_INSENSITIVE_ORDER))
            .map(bounty -> bounty.targetName() + " (" + bounty.rewards().size() + " items)")
            .collect(Collectors.joining(", "));
    return ok(source, bounties.isBlank() ? "No active bounties." : "Bounties: " + bounties);
  }

  private static ServerPlayer player(CommandSourceStack source) {
    try {
      return source.getPlayerOrException();
    } catch (Exception ex) {
      throw new IllegalStateException("This command must be run by a player.", ex);
    }
  }

  private static int ok(CommandSourceStack source, String message) {
    source.sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.GREEN), false);
    return 1;
  }

  private static int fail(CommandSourceStack source, String message) {
    source.sendFailure(Component.literal(message).withStyle(ChatFormatting.RED));
    return 0;
  }
}