package com.raiiiden.hierarchy.bounty.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.raiiiden.hierarchy.bounty.BountyLogic;
import com.raiiiden.hierarchy.bounty.config.BountyConfig;
import com.raiiiden.hierarchy.bounty.data.BountyData;
import com.raiiiden.hierarchy.bounty.events.BountyEvents;
import com.raiiiden.hierarchy.bounty.model.Bounty;
import com.raiiiden.hierarchy.clan.currency.CurrencyManager;
import com.raiiiden.hierarchy.humanity.config.HumanityConfig;
import com.raiiiden.hierarchy.humanity.data.HumanityData;
import com.raiiiden.hierarchy.nameplate.NameplateUtil;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class BountyCommands {
  private BountyCommands() {
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(Commands.literal("bounty")
        .requires(source -> BountyConfig.ENABLE_BOUNTIES.get())
        .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
        .then(Commands.literal("info").then(Commands.argument("target", EntityArgument.player()).executes(ctx -> info(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
        .then(Commands.literal("place")
            .requires(source -> source.getEntity() instanceof ServerPlayer)
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("amount", LongArgumentType.longArg(1L)).executes(ctx -> place(ctx.getSource(), player(ctx.getSource()), EntityArgument.getPlayer(ctx, "target"), LongArgumentType.getLong(ctx, "amount"))))))
        .then(Commands.literal("contribute")
            .requires(source -> source.getEntity() instanceof ServerPlayer)
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("amount", LongArgumentType.longArg(1L)).executes(ctx -> contribute(ctx.getSource(), player(ctx.getSource()), EntityArgument.getPlayer(ctx, "target"), LongArgumentType.getLong(ctx, "amount"))))))
        .then(Commands.literal("cancel")
            .requires(source -> source.getEntity() instanceof ServerPlayer)
            .then(Commands.argument("target", EntityArgument.player()).executes(ctx -> cancel(ctx.getSource(), player(ctx.getSource()), EntityArgument.getPlayer(ctx, "target")))))
        .then(Commands.literal("redeem")
            .requires(source -> source.getEntity() instanceof ServerPlayer)
            .executes(ctx -> redeem(ctx.getSource(), player(ctx.getSource())))));
  }

  private static int place(CommandSourceStack source, ServerPlayer placer, ServerPlayer target, long amount) {
    long now = System.currentTimeMillis();
    BountyData data = BountyData.get(source.getServer());
    data.expireIfNeeded(target.getUUID(), now, source.getServer());
    if (amount < BountyConfig.MIN_BOUNTY_AMOUNT.get()) {
      return fail(source, "Invalid bounty amount.");
    }
    if (placer.getUUID().equals(target.getUUID())) {
      return fail(source, "You cannot place a bounty on yourself.");
    }
    if (data.hasActiveBounty(target.getUUID(), now)) {
      return fail(source, "That target already has an active bounty.");
    }
    if (data.isCoolingDown(target.getUUID(), now)) {
      return fail(source, "That target cannot be bountied yet.");
    }
    if (!BountyLogic.relationshipAllowsBounty(source.getServer(), placer.getUUID(), target.getUUID())) {
      return fail(source, "You cannot bounty a clan member or ally.");
    }
    if (!BountyLogic.isEligibleTarget(source.getServer(), target.getUUID(), now)) {
      return fail(source, "That target is not eligible for a bounty.");
    }
    if (!payBountyCost(placer, amount, BountyConfig.PLACE_CURRENCY_COST.get(), BountyConfig.PLACE_XP_COST.get())) {
      return fail(source, "You do not have enough currency.");
    }
    data.place(source.getServer(), target.getUUID(), target.getGameProfile().getName(), placer.getUUID(), amount, now);
    target.sendSystemMessage(Component.literal("A bounty has been placed on you for " + amount + "."));
    return ok(source, "Bounty placed on " + target.getGameProfile().getName() + " for " + amount + ".");
  }

  private static int contribute(CommandSourceStack source, ServerPlayer contributor, ServerPlayer target, long amount) {
    long now = System.currentTimeMillis();
    BountyData data = BountyData.get(source.getServer());
    data.expireIfNeeded(target.getUUID(), now, source.getServer());
    Bounty bounty = data.bounty(target.getUUID()).orElse(null);
    if (bounty == null) {
      return fail(source, "That target does not have an active bounty.");
    }
    if (amount < BountyConfig.MIN_BOUNTY_AMOUNT.get()) {
      return fail(source, "Invalid bounty amount.");
    }
    if (!BountyLogic.relationshipAllowsBounty(source.getServer(), contributor.getUUID(), target.getUUID())) {
      return fail(source, "You cannot contribute to a bounty on a clan member or ally.");
    }
    if (!payBountyCost(contributor, amount, BountyConfig.CONTRIBUTE_CURRENCY_COST.get(), BountyConfig.CONTRIBUTE_XP_COST.get())) {
      return fail(source, "You do not have enough currency.");
    }
    data.contribute(bounty, contributor.getUUID(), amount);
    return ok(source, "Added " + amount + " to " + bounty.targetName() + "'s bounty.");
  }

  private static int cancel(CommandSourceStack source, ServerPlayer placer, ServerPlayer target) {
    long now = System.currentTimeMillis();
    BountyData data = BountyData.get(source.getServer());
    data.expireIfNeeded(target.getUUID(), now, source.getServer());
    Bounty bounty = data.bounty(target.getUUID()).orElse(null);
    if (bounty == null) {
      return fail(source, "That target does not have an active bounty.");
    }
    Long contribution = bounty.contributions().get(placer.getUUID());
    if (contribution == null || contribution <= 0L) {
      return fail(source, "You do not have a bounty contribution on that target.");
    }
    long refund = Math.max(0L, contribution - BountyLogic.taxFor(contribution));
    if (refund > 0L && !CurrencyManager.depositPlayer(placer, refund)) {
      return fail(source, "Unable to refund bounty.");
    }
    if (!data.cancelContribution(target.getUUID(), placer.getUUID(), now, source.getServer())) {
      return fail(source, "Unable to cancel bounty.");
    }
    return ok(source, "Cancelled your bounty contribution on " + bounty.targetName() + " and refunded " + refund + ".");
  }

  private static int redeem(CommandSourceStack source, ServerPlayer killer) {
    ItemStack tag = BountyEvents.findDogTag(killer);
    if (tag.isEmpty()) {
      return fail(source, "You do not have a bounty dog tag to redeem.");
    }
    UUID targetId = tag.getOrCreateTag().getUUID("BountyTarget");
    long now = System.currentTimeMillis();
    BountyData data = BountyData.get(source.getServer());
    data.expireIfNeeded(targetId, now, source.getServer());
    Bounty bounty = data.bounty(targetId).orElse(null);
    if (bounty == null) {
      return fail(source, "That bounty is no longer active.");
    }
    if (!payClaimCost(killer)) {
      return fail(source, "You do not have enough currency.");
    }
    if (!CurrencyManager.depositPlayer(killer, bounty.amount())) {
      return fail(source, "Unable to pay bounty.");
    }
    tag.shrink(1);
    if (HumanityConfig.ENABLE_HUMANITY.get()) {
      HumanityData.get(source.getServer()).add(
              killer.getUUID(),
              HumanityConfig.BOUNTY_CLAIM_HUMANITY_GAIN.get()  // already double now
      );
      NameplateUtil.refresh(killer);
    }
    data.clearAndCooldown(targetId, now, source.getServer());
    return ok(source, "Redeemed bounty for " + bounty.amount() + ".");
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
    return ok(source, bounty.targetName() + " bounty: " + bounty.amount() + " (" + seconds + "s remaining).");
  }

  private static int list(CommandSourceStack source) {
    BountyData data = BountyData.get(source.getServer());
    data.expireAll(System.currentTimeMillis(), source.getServer());
    String bounties = data.activeBounties().stream()
        .sorted(Comparator.comparing(Bounty::targetName, String.CASE_INSENSITIVE_ORDER))
        .map(bounty -> bounty.targetName() + " (" + bounty.amount() + ")")
        .collect(Collectors.joining(", "));
    return ok(source, bounties.isBlank() ? "No active bounties." : "Bounties: " + bounties);
  }

  private static boolean payBountyCost(ServerPlayer player, long amount, long actionCurrencyCost, int xpCost) {
    long total = amount + BountyLogic.taxFor(amount) + actionCurrencyCost;
    if (total < amount || !CurrencyManager.hasPlayerFunds(player, total)) {
      return false;
    }
    if (xpCost > 0 && player.experienceLevel < xpCost) {
      return false;
    }
    if (!CurrencyManager.withdrawPlayer(player, total)) {
      return false;
    }
    if (xpCost > 0) {
      player.giveExperienceLevels(-xpCost);
    }
    return true;
  }

  private static boolean payClaimCost(ServerPlayer player) {
    long currency = BountyConfig.CLAIM_CURRENCY_COST.get();
    int xp = BountyConfig.CLAIM_XP_COST.get();
    if (currency > 0L && !CurrencyManager.hasPlayerFunds(player, currency)) {
      return false;
    }
    if (xp > 0 && player.experienceLevel < xp) {
      return false;
    }
    if (currency > 0L && !CurrencyManager.withdrawPlayer(player, currency)) {
      return false;
    }
    if (xp > 0) {
      player.giveExperienceLevels(-xp);
    }
    return true;
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
