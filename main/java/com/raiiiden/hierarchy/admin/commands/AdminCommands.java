package com.raiiiden.hierarchy.admin.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.raiiiden.hierarchy.bounty.data.BountyData;
import com.raiiiden.hierarchy.bounty.model.Bounty;
import com.raiiiden.hierarchy.clan.data.ClanData;
import com.raiiiden.hierarchy.clan.model.Clan;
import com.raiiiden.hierarchy.humanity.data.HumanityData;
import com.raiiiden.hierarchy.nameplate.NameplateUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class AdminCommands {
    private AdminCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hierarchyadmin")
                .requires(source -> source.hasPermission(2))  // op level 2

                // Humanity commands
                .then(Commands.literal("humanity")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> humanityGet(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                .executes(ctx -> humanitySet(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        DoubleArgumentType.getDouble(ctx, "amount"))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                .executes(ctx -> humanityAdd(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        DoubleArgumentType.getDouble(ctx, "amount"))))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> humanityReset(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"))))))

                // Bounty admin commands
                .then(Commands.literal("bounty")
                        .then(Commands.literal("clear")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> bountyClear(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("clearall")
                                .executes(ctx -> bountyClearAll(ctx.getSource())))
                        .then(Commands.literal("info")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> bountyInfo(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"))))))

                // Clan admin commands
                .then(Commands.literal("clan")
                        .then(Commands.literal("disband")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> clanDisband(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("info")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> clanInfo(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("clearpvpdelays")
                                .executes(ctx -> clearPvpDelays(ctx.getSource())))
                        .then(Commands.literal("clearpvpdelays")
                                .executes(ctx -> clearPvpDelays(ctx.getSource()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> clearPvpDelaysForPlayer(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("kick")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> clanKick(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("resync")
                                .executes(ctx -> clanResync(ctx.getSource()))))

                // Wipe commands
                .then(Commands.literal("wipe")
                        .then(Commands.literal("bounties")
                                .executes(ctx -> wipeBounties(ctx.getSource())))
                        .then(Commands.literal("humanity")
                                .executes(ctx -> wipeHumanity(ctx.getSource())))));
    }

    // --- Humanity ---

    private static int humanityGet(CommandSourceStack source, ServerPlayer target) {
        double humanity = HumanityData.get(source.getServer()).humanity(target.getUUID());
        return ok(source, target.getGameProfile().getName() + " has " + humanity + " humanity.");
    }

    private static int humanitySet(CommandSourceStack source, ServerPlayer target, double amount) {
        HumanityData.get(source.getServer()).set(target.getUUID(), amount);
        NameplateUtil.refresh(target);
        return ok(source, "Set " + target.getGameProfile().getName() + "'s humanity to " + amount + ".");
    }

    private static int humanityAdd(CommandSourceStack source, ServerPlayer target, double amount) {
        HumanityData data = HumanityData.get(source.getServer());
        data.add(target.getUUID(), amount);
        NameplateUtil.refresh(target);
        double newValue = data.humanity(target.getUUID());
        return ok(source, "Adjusted " + target.getGameProfile().getName()
                + "'s humanity by " + amount + ". Now: " + newValue + ".");
    }

    private static int humanityReset(CommandSourceStack source, ServerPlayer target) {
        HumanityData.get(source.getServer()).set(target.getUUID(), 0);
        NameplateUtil.refresh(target);
        return ok(source, "Reset " + target.getGameProfile().getName() + "'s humanity to 0.");
    }

    // --- Bounty ---

    private static int bountyClear(CommandSourceStack source, ServerPlayer target) {
        long now = System.currentTimeMillis();
        BountyData data = BountyData.get(source.getServer());
        if (data.bounty(target.getUUID()).isEmpty()) {
            return fail(source, target.getGameProfile().getName() + " has no active bounty.");
        }
        data.clearAndCooldown(target.getUUID(), now);
        NameplateUtil.refresh(target);
        return ok(source, "Cleared bounty on " + target.getGameProfile().getName() + ".");
    }

    private static int bountyClearAll(CommandSourceStack source) {
        long now = System.currentTimeMillis();
        BountyData data = BountyData.get(source.getServer());
        int count = data.activeBounties().size();
        for (Bounty bounty : data.activeBounties().stream().toList()) {
            data.clearAndCooldown(bounty.targetId(), now);
        }
        NameplateUtil.refreshAll(source.getServer());
        return ok(source, "Cleared " + count + " bounties.");
    }

    private static int bountyInfo(CommandSourceStack source, ServerPlayer target) {
        long now = System.currentTimeMillis();
        BountyData data = BountyData.get(source.getServer());
        data.expireIfNeeded(target.getUUID(), now);
        Bounty bounty = data.bounty(target.getUUID()).orElse(null);
        if (bounty == null) {
            return ok(source, target.getGameProfile().getName() + " has no active bounty."
                    + (data.isCoolingDown(target.getUUID(), now)
                    ? " (on cooldown until " + data.cooldownUntil(target.getUUID()) + ")" : ""));
        }
        long seconds = Math.max(0L, (bounty.expiresAt() - now) / 1000L);
        boolean paused = bounty.pausedAt() > 0L;
        return ok(source, target.getGameProfile().getName() + " bounty: " + bounty.rewards().size() + " reward item(s)"
                + " | expires in " + seconds + "s"
                + (paused ? " (paused)" : "")
                + " | contributors: " + bounty.contributions().size());
    }

    // --- Clan ---

    private static int clanDisband(CommandSourceStack source, ServerPlayer target) {
        ClanData data = ClanData.get(source.getServer());
        Clan clan = data.clanOf(target.getUUID()).orElse(null);
        if (clan == null) {
            return fail(source, target.getGameProfile().getName() + " is not in a clan.");
        }
        String name = clan.getName();
        data.deleteClan(clan);
        NameplateUtil.refreshAll(source.getServer());
        return ok(source, "Disbanded clan " + name + ".");
    }

    private static int clanInfo(CommandSourceStack source, ServerPlayer target) {
        ClanData data = ClanData.get(source.getServer());
        Clan clan = data.clanOf(target.getUUID()).orElse(null);
        if (clan == null) {
            return fail(source, target.getGameProfile().getName() + " is not in a clan.");
        }
        source.sendSuccess(() -> Component.literal("Clan: " + clan.getName()
                + " | Members: " + clan.getMembers().size()
                + " | Leader: " + data.playerName(clan.getLeader())
                + " | Bank: " + clan.getBankBalance()), false);
        return 1;
    }

    private static int clearPvpDelays(CommandSourceStack source) {
        ClanData.get(source.getServer()).clearAllPvpDelays();
        return ok(source, "Cleared all PvP delays and combat snapshots.");
    }

    private static int clearPvpDelaysForPlayer(CommandSourceStack source, ServerPlayer target) {
        ClanData data = ClanData.get(source.getServer());
        // Clear snapshots involving this player
        data.clearCombatSnapshots(target.getUUID());
        // Clear their personal clan delays
        // Needs a new method on ClanData since playerClanPvpDelayUntil is private
        data.clearPlayerPvpDelays(target.getUUID());
        return ok(source, "Cleared PvP delays for " + target.getGameProfile().getName() + ".");
    }

    private static int clanKick(CommandSourceStack source, ServerPlayer target) {
        ClanData data = ClanData.get(source.getServer());
        Clan clan = data.clanOf(target.getUUID()).orElse(null);
        if (clan == null) {
            return fail(source, target.getGameProfile().getName() + " is not in a clan.");
        }
        if (clan.getLeader().equals(target.getUUID())) {
            return fail(source, "Cannot kick the clan leader. Use /hierarchyadmin clan disband instead.");
        }
        data.removeMember(clan, target.getUUID());
        NameplateUtil.refresh(target);
        return ok(source, "Kicked " + target.getGameProfile().getName() + " from " + clan.getName() + ".");
    }

    private static int clanResync(CommandSourceStack source) {
        ClanData.get(source.getServer()).resyncTeams();
        return ok(source, "Resynced all clan scoreboard teams.");
    }

    // --- Wipe ---

    private static int wipeBounties(CommandSourceStack source) {
        long now = System.currentTimeMillis();
        BountyData data = BountyData.get(source.getServer());
        int count = data.activeBounties().size();
        for (Bounty bounty : data.activeBounties().stream().toList()) {
            data.clearAndCooldown(bounty.targetId(), now);
        }
        NameplateUtil.refreshAll(source.getServer());
        return ok(source, "Wiped " + count + " active bounties.");
    }

    private static int wipeHumanity(CommandSourceStack source) {
        HumanityData.get(source.getServer()).clear();
        NameplateUtil.refreshAll(source.getServer());
        return ok(source, "Wiped all humanity data.");
    }

    private static int ok(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), true); // true = log to ops
        return 1;
    }

    private static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
}
