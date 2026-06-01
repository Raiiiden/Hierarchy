package com.raiiiden.hierarchy.clan.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.raiiiden.hierarchy.Hierarchy;
import com.raiiiden.hierarchy.clan.bank.BankPermission;
import com.raiiiden.hierarchy.clan.bank.BankTransaction;
import com.raiiiden.hierarchy.clan.bank.BankTransactionType;
import com.raiiiden.hierarchy.clan.config.ClanCombatConfig;
import com.raiiiden.hierarchy.clan.currency.ActionCost;
import com.raiiiden.hierarchy.clan.currency.ClanAction;
import com.raiiiden.hierarchy.clan.currency.CurrencyManager;
import com.raiiiden.hierarchy.clan.data.ClanData;
import com.raiiiden.hierarchy.clan.model.Clan;
import com.raiiiden.hierarchy.clan.model.ClanRole;
import com.raiiiden.hierarchy.nameplate.NameplateUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public final class ClanCommands {
  private ClanCommands() {
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(Commands.literal("clan")
        .requires(source -> source.getEntity() instanceof ServerPlayer && ClanCombatConfig.ENABLE_CLANS.get())
        .then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
        .then(Commands.literal("disband").executes(ctx -> disband(ctx.getSource())))
        .then(Commands.literal("info").executes(ctx -> info(ctx.getSource())))
        .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
        .then(Commands.literal("invite").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> invite(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
        .then(Commands.literal("accept").executes(ctx -> accept(ctx.getSource())))
        .then(Commands.literal("deny").executes(ctx -> deny(ctx.getSource())))
        .then(Commands.literal("kick").then(Commands.argument("player", StringArgumentType.word()).executes(ctx -> kick(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))
        .then(Commands.literal("leave").executes(ctx -> leave(ctx.getSource())))
        .then(Commands.literal("promote").then(Commands.argument("player", StringArgumentType.word()).executes(ctx -> promote(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))
        .then(Commands.literal("demote").then(Commands.argument("player", StringArgumentType.word()).executes(ctx -> demote(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))
        .then(Commands.literal("transfer").then(Commands.argument("player", StringArgumentType.word()).executes(ctx -> transfer(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))
        .then(Commands.literal("tag").then(Commands.argument("tag", StringArgumentType.word()).executes(ctx -> tag(ctx.getSource(), StringArgumentType.getString(ctx, "tag")))))
        .then(Commands.literal("ally")
            .then(Commands.literal("accept").then(Commands.argument("clan", StringArgumentType.word()).executes(ctx -> allyAccept(ctx.getSource(), StringArgumentType.getString(ctx, "clan")))))
            .then(Commands.literal("deny").then(Commands.argument("clan", StringArgumentType.word()).executes(ctx -> allyDeny(ctx.getSource(), StringArgumentType.getString(ctx, "clan")))))
            .then(Commands.argument("clan", StringArgumentType.word()).executes(ctx -> ally(ctx.getSource(), StringArgumentType.getString(ctx, "clan")))))
        .then(Commands.literal("unally").then(Commands.argument("clan", StringArgumentType.word()).executes(ctx -> unally(ctx.getSource(), StringArgumentType.getString(ctx, "clan")))))
        .then(Commands.literal("enemy").then(Commands.argument("clan", StringArgumentType.word()).executes(ctx -> enemy(ctx.getSource(), StringArgumentType.getString(ctx, "clan")))))
        .then(Commands.literal("neutral").then(Commands.argument("clan", StringArgumentType.word()).executes(ctx -> neutral(ctx.getSource(), StringArgumentType.getString(ctx, "clan")))))
        .then(Commands.literal("ff")
            .then(Commands.literal("request").then(Commands.argument("clan", StringArgumentType.word()).executes(ctx -> ffRequest(ctx.getSource(), StringArgumentType.getString(ctx, "clan")))))
            .then(Commands.literal("accept").then(Commands.argument("clan", StringArgumentType.word()).executes(ctx -> ffAccept(ctx.getSource(), StringArgumentType.getString(ctx, "clan")))))
            .then(Commands.literal("deny").then(Commands.argument("clan", StringArgumentType.word()).executes(ctx -> ffDeny(ctx.getSource(), StringArgumentType.getString(ctx, "clan")))))
            .then(Commands.literal("revoke").then(Commands.argument("clan", StringArgumentType.word()).executes(ctx -> ffRevoke(ctx.getSource(), StringArgumentType.getString(ctx, "clan"))))))
        .then(Commands.literal("bank")
            .executes(ctx -> bank(ctx.getSource()))
            .then(Commands.literal("deposit").then(Commands.argument("amount", LongArgumentType.longArg(1L)).executes(ctx -> bankDeposit(ctx.getSource(), LongArgumentType.getLong(ctx, "amount")))))
            .then(Commands.literal("withdraw").then(Commands.argument("amount", LongArgumentType.longArg(1L)).executes(ctx -> bankWithdraw(ctx.getSource(), LongArgumentType.getLong(ctx, "amount")))))
            .then(Commands.literal("transactions")
                .executes(ctx -> bankTransactions(ctx.getSource(), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(ctx -> bankTransactions(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page"))))))
        .then(Commands.literal("members").executes(ctx -> members(ctx.getSource())))
        .then(Commands.literal("allies").executes(ctx -> allies(ctx.getSource())))
        .then(Commands.literal("role").then(Commands.argument("player", StringArgumentType.word()).executes(ctx -> role(ctx.getSource(), StringArgumentType.getString(ctx, "player"))))));
  }

  private static int create(CommandSourceStack source, String name) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    data.rememberPlayer(player.getUUID(), player.getGameProfile().getName());
    if (data.clanOf(player.getUUID()).isPresent()) {
      return fail(source, "You are already in a clan.");
    }
    if (data.clanByName(name).isPresent()) {
      return fail(source, "A clan with that name already exists.");
    }
    if (!chargeAction(source, data, player, null, ClanAction.CLAN_CREATE)) {
      return 0;
    }
    data.createClan(name, player.getUUID());
    NameplateUtil.refresh(player);
    return ok(source, "Created clan " + name + ".");
  }

  private static int disband(CommandSourceStack source) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan clan = ownClan(source, data, player);
    if (clan == null) {
      return 0;
    }
    if (!clan.getLeader().equals(player.getUUID())) {
      return fail(source, "You do not have permission to do that.");
    }
    if (!chargeAction(source, data, player, clan, ClanAction.CLAN_DISBAND)) {
      return 0;
    }
    notifyClan(source, data, clan, "Clan " + clan.getName() + " was disbanded.", "While you were offline, clan " + clan.getName() + " was disbanded.", player.getUUID());
    data.deleteClan(clan);
    NameplateUtil.refreshAll(source.getServer());
    return ok(source, "Disbanded clan " + clan.getName() + ".");
  }

  private static int info(CommandSourceStack source) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan clan = ownClan(source, data, player);
    if (clan == null) {
      return 0;
    }
    source.sendSuccess(() -> Component.literal("Clan " + clan.getName()), false);
    source.sendSuccess(() -> Component.literal("Tag: " + (clan.getTag().isBlank() ? "none" : "[" + clan.getTag() + "]")), false);
    source.sendSuccess(() -> Component.literal("Leader: " + data.playerName(clan.getLeader())), false);
    source.sendSuccess(() -> Component.literal("Co-Leaders: " + names(data, clan.getCoLeaders().stream().toList())), false);
    source.sendSuccess(() -> Component.literal("Members: " + clan.getMembers().size()), false);
    source.sendSuccess(() -> Component.literal("Allies: " + clan.getAllies().size()), false);
    source.sendSuccess(() -> Component.literal("Enemies: " + clan.getEnemies().size()), false);
    return 1;
  }

  private static int list(CommandSourceStack source) {
    ClanData data = data(source);
    List<Clan> clans = data.clans().stream().sorted(Comparator.comparing(Clan::getName, String.CASE_INSENSITIVE_ORDER)).toList();
    if (clans.isEmpty()) {
      return ok(source, "No clans exist.");
    }
    source.sendSuccess(() -> Component.literal("Clans: " + clans.stream().map(clan -> clan.getName() + " (" + clan.getMembers().size() + ")").collect(Collectors.joining(", "))), false);
    return clans.size();
  }

  private static int invite(CommandSourceStack source, ServerPlayer target) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    data.rememberPlayer(player.getUUID(), player.getGameProfile().getName());
    data.rememberPlayer(target.getUUID(), target.getGameProfile().getName());
    Clan clan = ownManagedClan(source, data, player);
    if (clan == null) {
      return 0;
    }
    if (data.clanOf(target.getUUID()).isPresent()) {
      return fail(source, "That player is already in a clan.");
    }
    data.invite(target.getUUID(), clan);
    sendClanInviteMessage(target, clan);
    return ok(source, "Invited " + target.getGameProfile().getName() + " to " + clan.getName() + ".");
  }

  private static int accept(CommandSourceStack source) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    if (data.clanOf(player.getUUID()).isPresent()) {
      return fail(source, "You are already in a clan.");
    }
    Clan clan = data.pendingInvite(player.getUUID()).orElse(null);
    if (clan == null) {
      return fail(source, "You do not have a pending clan invite.");
    }
    data.addMember(clan, player.getUUID());
    NameplateUtil.refresh(player);
    notifyClan(source, data, clan, player.getGameProfile().getName() + " joined the clan.", player.getUUID());
    return ok(source, "Joined " + clan.getName() + ".");
  }

  private static int deny(CommandSourceStack source) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    if (data.pendingInvite(player.getUUID()).isEmpty()) {
      return fail(source, "You do not have a pending clan invite.");
    }
    data.clearInvite(player.getUUID());
    return ok(source, "Denied the clan invite.");
  }

  private static int kick(CommandSourceStack source, String targetName) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan clan = ownManagedClan(source, data, player);
    if (clan == null) {
      return 0;
    }
    UUID targetId = memberByName(source, data, clan, targetName);
    if (targetId == null) {
      return 0;
    }
    if (targetId.equals(clan.getLeader())) {
      return fail(source, "You do not have permission to do that.");
    }
    data.removeMemberWithPvpDelay(clan, targetId, pvpAllowedAfter());
    refreshOnlineNameplate(source, targetId);
    notifyPlayer(source, data, targetId, "You were kicked from " + clan.getName() + ".", "While you were offline, you were kicked from " + clan.getName() + ".");
    return ok(source, "Kicked " + data.playerName(targetId) + ". PvP with their former clan is temporarily invalid.");
  }

  private static int leave(CommandSourceStack source) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan clan = ownClan(source, data, player);
    if (clan == null) {
      return 0;
    }
    if (clan.getLeader().equals(player.getUUID())) {
      return fail(source, "Transfer leadership or disband the clan before leaving.");
    }
    data.removeMemberWithPvpDelay(clan, player.getUUID(), pvpAllowedAfter());
    NameplateUtil.refresh(player);
    notifyClan(source, data, clan, player.getGameProfile().getName() + " left the clan.", player.getUUID());
    return ok(source, "Left " + clan.getName() + ". PvP with your former clan is temporarily invalid.");
  }

  private static int promote(CommandSourceStack source, String targetName) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan clan = ownManagedClan(source, data, player);
    if (clan == null) {
      return 0;
    }
    UUID targetId = memberByName(source, data, clan, targetName);
    if (targetId == null) {
      return 0;
    }
    if (clan.getLeader().equals(targetId)) {
      return fail(source, "You cannot promote this player.");
    }
    if (!clan.getCoLeaders().add(targetId)) {
      return fail(source, "You cannot promote this player.");
    }
    data.setDirty();
    notifyPlayer(source, data, targetId, "You were promoted to Co-Leader in " + clan.getName() + ".", "While you were offline, you were promoted to Co-Leader in " + clan.getName() + ".");
    return ok(source, "Promoted " + data.playerName(targetId) + " to co-leader.");
  }

  private static int demote(CommandSourceStack source, String targetName) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan clan = ownManagedClan(source, data, player);
    if (clan == null) {
      return 0;
    }
    UUID targetId = memberByName(source, data, clan, targetName);
    if (targetId == null) {
      return 0;
    }
    if (clan.getLeader().equals(targetId)) {
      return fail(source, "You do not have permission to do that.");
    }
    if (!clan.getCoLeaders().remove(targetId)) {
      return fail(source, "That player is not in your clan.");
    }
    data.setDirty();
    notifyPlayer(source, data, targetId, "You were demoted to member in " + clan.getName() + ".", "While you were offline, you were demoted to member in " + clan.getName() + ".");
    return ok(source, "Demoted " + data.playerName(targetId) + " to member.");
  }

  private static int transfer(CommandSourceStack source, String targetName) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan clan = ownClan(source, data, player);
    if (clan == null) {
      return 0;
    }
    if (!clan.getLeader().equals(player.getUUID())) {
      return fail(source, "You do not have permission to do that.");
    }
    UUID targetId = memberByName(source, data, clan, targetName);
    if (targetId == null) {
      return 0;
    }
    if (targetId.equals(player.getUUID())) {
      return fail(source, "You are already the leader.");
    }
    clan.getCoLeaders().add(player.getUUID());
    clan.setLeader(targetId);
    data.setDirty();
    notifyClan(source, data, clan, data.playerName(targetId) + " is now the clan leader.", "While you were offline, leadership of " + clan.getName() + " was transferred to " + data.playerName(targetId) + ".", null);
    return 1;
  }

  private static int tag(CommandSourceStack source, String tag) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan clan = ownManagedClan(source, data, player);
    if (clan == null) {
      return 0;
    }
    if (tag.length() > 6 || !tag.matches("[A-Za-z0-9]+")) {
      return fail(source, "Clan tag must be 1-6 letters or numbers.");
    }
    clan.setTag(tag.toUpperCase(java.util.Locale.ROOT));
    data.setDirty();
    NameplateUtil.refreshAll(source.getServer());
    return ok(source, "Clan tag set to [" + clan.getTag() + "].");
  }

  private static int ally(CommandSourceStack source, String targetName) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan own = ownManagedClan(source, data, player);
    Clan target = targetClan(source, data, targetName);
    if (own == null || target == null) {
      return 0;
    }
    if (own.getId().equals(target.getId())) {
      return fail(source, "You cannot ally with your own clan.");
    }
    if (data.areAllied(own.getId(), target.getId())) {
      return fail(source, "Those clans are already allied.");
    }
    if (data.hasAllianceRequest(target.getId(), own.getId())) {
      return fail(source, target.getName() + " already requested an alliance. Use /clan ally accept " + target.getName() + " or /clan ally deny " + target.getName() + ".");
    }
    if (data.hasAllianceRequest(own.getId(), target.getId())) {
      return fail(source, "Your clan already has a pending alliance request with " + target.getName() + ".");
    }
    if (!chargeAction(source, data, player, own, ClanAction.CLAN_ALLY)) {
      return 0;
    }
    data.requestAlliance(own.getId(), target.getId());
    notifyClanManagers(source, data, target, allianceRequestMessage(own.getName()), "While you were offline, " + own.getName() + " requested an alliance with your clan.", null);
    notifyClan(source, data, own, "Your clan requested an alliance with " + target.getName() + ".", "While you were offline, your clan requested an alliance with " + target.getName() + ".", player.getUUID());
    return ok(source, "Alliance request sent to " + target.getName() + ".");
  }

  private static int allyAccept(CommandSourceStack source, String targetName) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan own = ownManagedClan(source, data, player);
    Clan target = targetClan(source, data, targetName);
    if (own == null || target == null) {
      return 0;
    }
    if (data.areAllied(own.getId(), target.getId())) {
      data.clearAllianceRequest(target.getId(), own.getId());
      return fail(source, "Those clans are already allied.");
    }
    if (!data.hasAllianceRequest(target.getId(), own.getId())) {
      return fail(source, "There is no alliance request from that clan.");
    }
    data.addAlliance(own, target);
    notifyClan(source, data, own, "Your clan accepted an alliance with " + target.getName() + ".", "While you were offline, your clan accepted an alliance with " + target.getName() + ".", player.getUUID());
    notifyClan(source, data, target, own.getName() + " accepted your alliance request.", "While you were offline, " + own.getName() + " accepted your alliance request.", null);
    return ok(source, "Accepted alliance with " + target.getName() + ".");
  }

  private static int allyDeny(CommandSourceStack source, String targetName) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan own = ownManagedClan(source, data, player);
    Clan target = targetClan(source, data, targetName);
    if (own == null || target == null) {
      return 0;
    }
    if (!data.hasAllianceRequest(target.getId(), own.getId())) {
      return fail(source, "There is no alliance request from that clan.");
    }
    data.clearAllianceRequest(target.getId(), own.getId());
    notifyClan(source, data, target, own.getName() + " denied your alliance request.", "While you were offline, " + own.getName() + " denied your alliance request.", null);
    return ok(source, "Denied alliance request from " + target.getName() + ".");
  }

  private static int unally(CommandSourceStack source, String targetName) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan own = ownManagedClan(source, data, player);
    Clan target = targetClan(source, data, targetName);
    if (own == null || target == null) {
      return 0;
    }
    if (!data.areAllied(own.getId(), target.getId())) {
      return fail(source, "Those clans are not allied.");
    }
    if (!chargeAction(source, data, player, own, ClanAction.CLAN_UNALLY)) {
      return 0;
    }
    data.removeAlliance(own, target, pvpAllowedAfter());
    notifyClan(source, data, own, "Your clan un-allied " + target.getName() + ". PvP is temporarily invalid.", "While you were offline, your clan un-allied " + target.getName() + ".", player.getUUID());
    notifyClan(source, data, target, own.getName() + " ended the alliance. PvP is temporarily invalid.", "While you were offline, " + own.getName() + " un-allied your clan.", null);
    return ok(source, "Ended alliance with " + target.getName() + ". PvP is temporarily invalid.");
  }

  private static int ffRequest(CommandSourceStack source, String targetName) {
    ServerPlayer player = player(source);
    if (!ClanCombatConfig.ENABLE_FRIENDLY_FIRE_SYSTEM.get()) {
      return fail(source, "You do not have permission to do that.");
    }
    ClanData data = data(source);
    Clan own = ownManagedClan(source, data, player);
    Clan target = alliedClan(source, data, own, targetName);
    if (own == null || target == null) {
      return 0;
    }
    data.requestFriendlyFire(own.getId(), target.getId());
    notifyClan(source, data, target, own.getName() + " requested friendly fire. Use /clan ff accept " + own.getName() + " or /clan ff deny " + own.getName() + ".", null);
    return ok(source, "Friendly fire request sent.");
  }

  private static int enemy(CommandSourceStack source, String targetName) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan own = ownManagedClan(source, data, player);
    Clan target = targetClan(source, data, targetName);
    if (own == null || target == null) {
      return 0;
    }
    if (own.getId().equals(target.getId())) {
      return fail(source, "You cannot mark your own clan as an enemy.");
    }
    data.setEnemy(own, target);
    return ok(source, target.getName() + " marked as enemy.");
  }

  private static int neutral(CommandSourceStack source, String targetName) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan own = ownManagedClan(source, data, player);
    Clan target = targetClan(source, data, targetName);
    if (own == null || target == null) {
      return 0;
    }
    data.clearEnemy(own, target);
    return ok(source, target.getName() + " marked as neutral.");
  }

  private static int ffAccept(CommandSourceStack source, String targetName) {
    ServerPlayer player = player(source);
    if (!ClanCombatConfig.ENABLE_FRIENDLY_FIRE_SYSTEM.get()) {
      return fail(source, "You do not have permission to do that.");
    }
    ClanData data = data(source);
    Clan own = ownManagedClan(source, data, player);
    Clan target = alliedClan(source, data, own, targetName);
    if (own == null || target == null) {
      return 0;
    }
    if (!data.hasFriendlyFireRequest(own.getId(), target.getId())) {
      return fail(source, "There is no friendly fire request for that clan.");
    }
    if (!chargeAction(source, data, player, own, ClanAction.FRIENDLY_FIRE_ACCEPT)) {
      return 0;
    }
    data.setFriendlyFire(own.getId(), target.getId(), true);
    notifyClan(source, data, own, "Friendly fire enabled with " + target.getName() + ".", "While you were offline, friendly fire was enabled with " + target.getName() + ".", player.getUUID());
    notifyClan(source, data, target, own.getName() + " accepted friendly fire.", "While you were offline, " + own.getName() + " accepted friendly fire.", null);
    return ok(source, "Friendly fire enabled with " + target.getName() + ".");
  }

  private static int ffDeny(CommandSourceStack source, String targetName) {
    ServerPlayer player = player(source);
    if (!ClanCombatConfig.ENABLE_FRIENDLY_FIRE_SYSTEM.get()) {
      return fail(source, "You do not have permission to do that.");
    }
    ClanData data = data(source);
    Clan own = ownManagedClan(source, data, player);
    Clan target = alliedClan(source, data, own, targetName);
    if (own == null || target == null) {
      return 0;
    }
    if (!data.hasFriendlyFireRequest(own.getId(), target.getId())) {
      return fail(source, "There is no friendly fire request for that clan.");
    }
    data.clearFriendlyFireRequest(own.getId(), target.getId());
    notifyClan(source, data, target, own.getName() + " denied friendly fire.", null);
    return ok(source, "Denied friendly fire with " + target.getName() + ".");
  }

  private static int ffRevoke(CommandSourceStack source, String targetName) {
    ServerPlayer player = player(source);
    if (!ClanCombatConfig.ENABLE_FRIENDLY_FIRE_SYSTEM.get()) {
      return fail(source, "You do not have permission to do that.");
    }
    ClanData data = data(source);
    Clan own = ownManagedClan(source, data, player);
    Clan target = alliedClan(source, data, own, targetName);
    if (own == null || target == null) {
      return 0;
    }
    data.revokeFriendlyFire(own.getId(), target.getId(), pvpAllowedAfter());
    notifyClan(source, data, own, "Friendly fire revoked with " + target.getName() + ".", "While you were offline, friendly fire was revoked with " + target.getName() + ".", player.getUUID());
    notifyClan(source, data, target, own.getName() + " revoked friendly fire. PvP is temporarily invalid.", "While you were offline, " + own.getName() + " revoked friendly fire.", null);
    return ok(source, "Friendly fire revoked.");
  }

  private static int members(CommandSourceStack source) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan clan = ownClan(source, data, player);
    if (clan == null) {
      return 0;
    }
    source.sendSuccess(() -> Component.literal("Members: " + names(data, clan.getMembers().stream().toList())), false);
    return clan.getMembers().size();
  }

  private static int allies(CommandSourceStack source) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan clan = ownClan(source, data, player);
    if (clan == null) {
      return 0;
    }
    String allies = clan.getAllies().stream().map(data::clan).flatMap(java.util.Optional::stream).map(Clan::getName).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.joining(", "));
    return ok(source, "Allies: " + (allies.isBlank() ? "none" : allies));
  }

  private static int role(CommandSourceStack source, String targetName) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan clan = ownClan(source, data, player);
    if (clan == null) {
      return 0;
    }
    UUID targetId = memberByName(source, data, clan, targetName);
    if (targetId == null) {
      return 0;
    }
    return ok(source, data.playerName(targetId) + " is " + clan.roleOf(targetId).name().toLowerCase().replace('_', '-') + ".");
  }

  private static int bank(CommandSourceStack source) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan clan = ownClan(source, data, player);
    if (clan == null) {
      return 0;
    }
    if (!ClanCombatConfig.ENABLE_CLAN_BANK.get()) {
      return fail(source, "Clan bank is disabled.");
    }
    if (!hasBankPermission(clan, player.getUUID(), BankPermission.BANK_VIEW)) {
      return fail(source, "You do not have permission to access the clan bank.");
    }
    return ok(source, "Clan bank balance: " + clan.getBankBalance() + ".");
  }

  private static int bankDeposit(CommandSourceStack source, long amount) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan clan = ownClan(source, data, player);
    if (clan == null) {
      return 0;
    }
    if (!ClanCombatConfig.ENABLE_CLAN_BANK.get()) {
      return fail(source, "Clan bank is disabled.");
    }
    if (!hasBankPermission(clan, player.getUUID(), BankPermission.BANK_DEPOSIT)) {
      return fail(source, "You do not have permission to access the clan bank.");
    }
    if (amount < ClanCombatConfig.MIN_DEPOSIT_AMOUNT.get()) {
      return fail(source, "Invalid amount.");
    }
    if (!CurrencyManager.hasPlayerFunds(player, amount)) {
      return fail(source, "You do not have enough currency.");
    }
    synchronized (clan) {
      if (!CurrencyManager.withdrawPlayer(player, amount)) {
        return fail(source, "You do not have enough currency.");
      }
      if (!CurrencyManager.clanProvider().deposit(clan, amount)) {
        return fail(source, "Invalid amount.");
      }
      data.recordTransaction(clan, new BankTransaction(BankTransactionType.DEPOSIT, player.getUUID(), player.getGameProfile().getName(), amount, System.currentTimeMillis(), ""));
      auditBank(clan, player, BankTransactionType.DEPOSIT, amount);
    }
    return ok(source, "Deposited " + amount + " into the clan bank.");
  }

  private static int bankWithdraw(CommandSourceStack source, long amount) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan clan = ownClan(source, data, player);
    if (clan == null) {
      return 0;
    }
    if (!ClanCombatConfig.ENABLE_CLAN_BANK.get()) {
      return fail(source, "Clan bank is disabled.");
    }
    if (!hasBankPermission(clan, player.getUUID(), BankPermission.BANK_WITHDRAW)) {
      return fail(source, "You do not have permission to access the clan bank.");
    }
    if (amount < ClanCombatConfig.MIN_WITHDRAW_AMOUNT.get()) {
      return fail(source, "Invalid amount.");
    }
    synchronized (clan) {
      if (!CurrencyManager.clanProvider().hasFunds(clan, amount)) {
        return fail(source, "Clan bank does not have enough funds.");
      }
      if (!CurrencyManager.clanProvider().withdraw(clan, amount)) {
        return fail(source, "Clan bank does not have enough funds.");
      }
      if (!CurrencyManager.depositPlayer(player, amount)) {
        CurrencyManager.clanProvider().deposit(clan, amount);
        return fail(source, "You do not have enough currency.");
      }
      data.recordTransaction(clan, new BankTransaction(BankTransactionType.WITHDRAW, player.getUUID(), player.getGameProfile().getName(), amount, System.currentTimeMillis(), ""));
      auditBank(clan, player, BankTransactionType.WITHDRAW, amount);
    }
    return ok(source, "Withdrew " + amount + " from the clan bank.");
  }

  private static int bankTransactions(CommandSourceStack source, int page) {
    ServerPlayer player = player(source);
    ClanData data = data(source);
    Clan clan = ownClan(source, data, player);
    if (clan == null) {
      return 0;
    }
    if (!ClanCombatConfig.ENABLE_CLAN_BANK.get()) {
      return fail(source, "Clan bank is disabled.");
    }
    if (!hasBankPermission(clan, player.getUUID(), BankPermission.BANK_VIEW_TRANSACTIONS)) {
      return fail(source, "You do not have permission to access the clan bank.");
    }
    List<BankTransaction> transactions = new ArrayList<>(clan.getTransactions());
    if (transactions.isEmpty()) {
      return fail(source, "No transactions recorded.");
    }
    int pageSize = 5;
    int maxPage = (transactions.size() + pageSize - 1) / pageSize;
    if (page > maxPage) {
      return fail(source, "That page does not exist.");
    }
    int end = transactions.size() - (page - 1) * pageSize;
    int start = Math.max(0, end - pageSize);
    source.sendSuccess(() -> Component.literal("Clan bank transactions page " + page + "/" + maxPage + ":"), false);
    for (int i = end - 1; i >= start; i--) {
      BankTransaction transaction = transactions.get(i);
      source.sendSuccess(() -> Component.literal(transaction.type().name().toLowerCase() + " " + transaction.amount() + " by " + transaction.playerName() + " at " + Instant.ofEpochMilli(transaction.timestamp())), false);
    }
    return 1;
  }

  private static ServerPlayer player(CommandSourceStack source) {
    try {
      return source.getPlayerOrException();
    } catch (Exception ex) {
      throw new IllegalStateException("This command must be run by a player.", ex);
    }
  }

  private static ClanData data(CommandSourceStack source) {
    return ClanData.get(source.getServer());
  }

  private static Clan ownClan(CommandSourceStack source, ClanData data, ServerPlayer player) {
    return data.clanOf(player.getUUID()).orElseGet(() -> {
      source.sendFailure(Component.literal("You are not in a clan."));
      return null;
    });
  }

  private static Clan ownManagedClan(CommandSourceStack source, ClanData data, ServerPlayer player) {
    Clan clan = ownClan(source, data, player);
    if (clan == null) {
      return null;
    }
    if (!clan.canManage(player.getUUID())) {
      source.sendFailure(Component.literal("You do not have permission to do that."));
      return null;
    }
    return clan;
  }

  private static Clan targetClan(CommandSourceStack source, ClanData data, String name) {
    return data.clanByName(name).orElseGet(() -> {
      source.sendFailure(Component.literal("That clan does not exist."));
      return null;
    });
  }

  private static Clan alliedClan(CommandSourceStack source, ClanData data, Clan own, String name) {
    if (own == null) {
      return null;
    }
    Clan target = targetClan(source, data, name);
    if (target == null) {
      return null;
    }
    if (!data.areAllied(own.getId(), target.getId())) {
      source.sendFailure(Component.literal("Those clans are not allied."));
      return null;
    }
    return target;
  }

  private static UUID memberByName(CommandSourceStack source, ClanData data, Clan clan, String name) {
    UUID playerId = data.playerByName(name).orElse(null);
    if (playerId == null || !clan.contains(playerId)) {
      source.sendFailure(Component.literal("That player is not in your clan."));
      return null;
    }
    return playerId;
  }

  private static void notifyClan(CommandSourceStack source, ClanData data, Clan clan, String message, UUID except) {
    notifyClan(source, data, clan, message, message, except);
  }

  private static void notifyClan(CommandSourceStack source, ClanData data, Clan clan, String onlineMessage, String offlineMessage, UUID except) {
    for (UUID member : clan.getMembers()) {
      if (except != null && except.equals(member)) {
        continue;
      }
      notifyPlayer(source, data, member, onlineMessage, offlineMessage);
    }
  }

  private static void notifyPlayer(CommandSourceStack source, ClanData data, UUID playerId, String message) {
    notifyPlayer(source, data, playerId, message, message);
  }

  private static void notifyPlayer(CommandSourceStack source, ClanData data, UUID playerId, String onlineMessage, String offlineMessage) {
    ServerPlayer online = source.getServer().getPlayerList().getPlayer(playerId);
    if (online != null) {
      online.sendSystemMessage(Component.literal(onlineMessage));
    } else {
      data.queueNotification(playerId, offlineMessage);
    }
  }

  private static void notifyClanManagers(CommandSourceStack source, ClanData data, Clan clan, Component onlineMessage, String offlineMessage, UUID except) {
    List<UUID> managers = new ArrayList<>();
    managers.add(clan.getLeader());
    managers.addAll(clan.getCoLeaders());
    for (UUID manager : managers) {
      if (except != null && except.equals(manager)) {
        continue;
      }
      ServerPlayer online = source.getServer().getPlayerList().getPlayer(manager);
      if (online != null) {
        online.sendSystemMessage(onlineMessage);
      } else {
        data.queueNotification(manager, offlineMessage);
      }
    }
  }

  private static void sendClanInviteMessage(ServerPlayer target, Clan clan) {
    MutableComponent message = Component.literal("You have been invited to join " + clan.getName() + ". Type /clan accept or /clan deny. ");
    message.append(button("[ACCEPT]", "/clan accept", ChatFormatting.GREEN));
    message.append(Component.literal(" "));
    message.append(button("[DENY]", "/clan deny", ChatFormatting.RED));
    target.sendSystemMessage(message);
  }

  private static MutableComponent allianceRequestMessage(String clanName) {
    MutableComponent message = Component.literal(clanName + " requested an alliance with your clan. Type /clan ally accept " + clanName + " or /clan ally deny " + clanName + ". ");
    message.append(button("[ACCEPT]", "/clan ally accept " + clanName, ChatFormatting.GREEN));
    message.append(Component.literal(" "));
    message.append(button("[DENY]", "/clan ally deny " + clanName, ChatFormatting.RED));
    return message;
  }

  private static MutableComponent button(String label, String command, ChatFormatting color) {
    return Component.literal(label)
        .withStyle(style -> style
            .withColor(color)
            .withBold(true)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))));
  }

  private static void refreshOnlineNameplate(CommandSourceStack source, UUID playerId) {
    ServerPlayer online = source.getServer().getPlayerList().getPlayer(playerId);
    if (online != null) {
      NameplateUtil.refresh(online);
    }
  }

  private static String names(ClanData data, List<UUID> players) {
    String names = players.stream().map(data::playerName).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.joining(", "));
    return names.isBlank() ? "none" : names;
  }

  private static boolean hasBankPermission(Clan clan, UUID playerId, BankPermission permission) {
    return ClanCombatConfig.bankPermission(clan.roleOf(playerId), permission);
  }

  private static void auditBank(Clan clan, ServerPlayer player, BankTransactionType type, long amount) {
    if (ClanCombatConfig.ENABLE_BANK_AUDIT_LOGGING.get()) {
      Hierarchy.LOGGER.info("[ClanBank] {} {} {} in clan {}", player.getGameProfile().getName(), type.name().toLowerCase(), amount, clan.getName());
    }
  }

  private static boolean chargeAction(CommandSourceStack source, ClanData data, ServerPlayer player, Clan clan, ClanAction action) {
    ActionCost cost = ClanCombatConfig.cost(action);
    if (cost.isFree()) {
      return true;
    }
    if (!cost.canPay(player, clan)) {
      if (cost.currency() > 0L) {
        source.sendFailure(Component.literal("You do not have enough currency."));
      } else {
        source.sendFailure(Component.literal("You do not have enough XP levels."));
      }
      return false;
    }
    boolean paid = cost.withdraw(player, clan);
    if (paid) {
      data.setDirty();
      return true;
    }
    source.sendFailure(Component.literal("You do not have enough currency."));
    return false;
  }

  private static long pvpAllowedAfter() {
    return System.currentTimeMillis() + ClanCombatConfig.PVP_DELAY_SECONDS.get() * 1000L;
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
