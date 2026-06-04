package com.raiiiden.hierarchy.bounty.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.raiiiden.hierarchy.bounty.config.BountyConfig;
import com.raiiiden.hierarchy.bounty.data.BountyData;
import com.raiiiden.hierarchy.bounty.gui.BountyMenus;
import com.raiiiden.hierarchy.bounty.model.Bounty;
import java.util.Comparator;
import java.util.stream.Collectors;
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

  private static int openMain(CommandSourceStack source, ServerPlayer player) {
    BountyMenus.openMain(player);
    return 1;
  }

  private static int openPlace(CommandSourceStack source, ServerPlayer player) {
    BountyMenus.openPlayerSelection(player, false, 0);
    return 1;
  }

  private static int openContribute(CommandSourceStack source, ServerPlayer player) {
    BountyMenus.openPlayerSelection(player, true, 0);
    return 1;
  }

  private static int openRedeem(CommandSourceStack source, ServerPlayer player) {
    BountyMenus.openRedemption(player);
    return 1;
  }

  private static int openList(CommandSourceStack source) {
    if (source.getEntity() instanceof ServerPlayer player) {
      BountyMenus.openActiveBoard(player, 0);
      return 1;
    }
    return list(source);
  }

  private static int info(CommandSourceStack source, ServerPlayer target) {
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
    source.sendSuccess(() -> Component.literal(message), false);
    return 1;
  }

  private static int fail(CommandSourceStack source, String message) {
    source.sendFailure(Component.literal(message));
    return 0;
  }
}
