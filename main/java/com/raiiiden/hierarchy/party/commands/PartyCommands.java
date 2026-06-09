package com.raiiiden.hierarchy.party.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.raiiiden.hierarchy.party.config.PartyConfig;
import com.raiiiden.hierarchy.party.data.PartyData;
import com.raiiiden.hierarchy.party.model.Party;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.stream.Collectors;

public final class PartyCommands {
    private PartyCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("party")
                .requires(source -> source.getEntity() instanceof ServerPlayer
                        && PartyConfig.ENABLE_PARTIES.get())
                // /party — show party info (or "not in a party")
                .executes(ctx -> info(ctx.getSource(), player(ctx.getSource())))
                .then(Commands.literal("create")
                        .executes(ctx -> create(ctx.getSource(), player(ctx.getSource()))))
                .then(Commands.literal("disband")
                        .executes(ctx -> disband(ctx.getSource(), player(ctx.getSource()))))
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> invite(ctx.getSource(), player(ctx.getSource()),
                                        EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("accept")
                        .executes(ctx -> accept(ctx.getSource(), player(ctx.getSource()))))
                .then(Commands.literal("decline")
                        .executes(ctx -> decline(ctx.getSource(), player(ctx.getSource()))))
                .then(Commands.literal("leave")
                        .executes(ctx -> leave(ctx.getSource(), player(ctx.getSource()))))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> kick(ctx.getSource(), player(ctx.getSource()),
                                        EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("list")
                        .executes(ctx -> info(ctx.getSource(), player(ctx.getSource()))))
                .then(Commands.literal("leader")
                        .executes(ctx -> leader(ctx.getSource(), player(ctx.getSource())))));
    }

    // -------------------------------------------------------------------------
    // Handlers
    // -------------------------------------------------------------------------

    /** /party create — explicitly creates a party (also auto-created by /party invite). */
    private static int create(CommandSourceStack source, ServerPlayer player) {
        PartyData data = PartyData.get(source.getServer());
        if (data.partyOf(player.getUUID()).isPresent()) {
            return fail(source, "You are already in a party.");
        }
        data.createParty(player.getUUID(), System.currentTimeMillis());
        data.syncAll();
        return ok(source, "Party created. Use /party invite <player> to add members.");
    }

    /** /party disband — leader only; dissolves the whole party. */
    private static int disband(CommandSourceStack source, ServerPlayer player) {
        PartyData data = PartyData.get(source.getServer());
        Party party = data.partyOf(player.getUUID()).orElse(null);
        if (party == null) return fail(source, "You are not in a party.");
        if (!party.isLeader(player.getUUID())) {
            return fail(source, "Only the party leader can disband the party.");
        }
        notifyParty(source.getServer(), party, "The party was disbanded.", player.getUUID());
        data.disbandParty(party);
        data.syncAll();
        return ok(source, "Party disbanded.");
    }

    /**
     * /party invite <player> — sends a party invite.
     * Auto-creates a party for the inviter if they are not already in one.
     * Only the party leader can send invites.
     */
    private static int invite(CommandSourceStack source, ServerPlayer player, ServerPlayer target) {
        if (player.getUUID().equals(target.getUUID())) {
            return fail(source, "You cannot invite yourself.");
        }
        PartyData data = PartyData.get(source.getServer());
        Party party = data.partyOf(player.getUUID()).orElse(null);
        if (party == null) {
            // Auto-create a party so the leader can invite straight away
            party = data.createParty(player.getUUID(), System.currentTimeMillis());
        }
        if (!party.isLeader(player.getUUID())) {
            return fail(source, "Only the party leader can invite players.");
        }
        if (data.partyOf(target.getUUID()).isPresent()) {
            return fail(source, target.getGameProfile().getName() + " is already in a party.");
        }
        if (party.isFull(PartyConfig.MAX_PARTY_MEMBERS.get())) {
            return fail(source, "Your party is full (" + PartyConfig.MAX_PARTY_MEMBERS.get() + " members max).");
        }
        if (data.hasPendingInvite(target.getUUID())) {
            return fail(source, target.getGameProfile().getName() + " already has a pending party invite.");
        }
        data.invite(target.getUUID(), party.getId());
        sendInviteMessage(target, player.getGameProfile().getName());
        return ok(source, "Invited " + target.getGameProfile().getName() + " to your party.");
    }

    /** /party accept — accepts a pending party invite. */
    private static int accept(CommandSourceStack source, ServerPlayer player) {
        PartyData data = PartyData.get(source.getServer());
        if (data.partyOf(player.getUUID()).isPresent()) {
            data.clearInvite(player.getUUID());
            return fail(source, "You are already in a party.");
        }
        Party party = data.pendingInvite(player.getUUID()).orElse(null);
        if (party == null) {
            return fail(source, "You do not have a pending party invite.");
        }
        if (party.isFull(PartyConfig.MAX_PARTY_MEMBERS.get())) {
            data.clearInvite(player.getUUID());
            return fail(source, "That party is now full.");
        }
        data.addMember(party, player.getUUID());
        notifyParty(source.getServer(), party, player.getGameProfile().getName() + " joined the party.", player.getUUID());
        data.syncAll();
        return ok(source, "Joined the party.");
    }

    /** /party decline — declines a pending party invite. */
    private static int decline(CommandSourceStack source, ServerPlayer player) {
        PartyData data = PartyData.get(source.getServer());
        if (!data.hasPendingInvite(player.getUUID())) {
            return fail(source, "You do not have a pending party invite.");
        }
        data.clearInvite(player.getUUID());
        return ok(source, "Declined the party invite.");
    }

    /** /party leave — leaves the party; transfers leadership if the leader leaves. */
    private static int leave(CommandSourceStack source, ServerPlayer player) {
        PartyData data = PartyData.get(source.getServer());
        Party party = data.partyOf(player.getUUID()).orElse(null);
        if (party == null) return fail(source, "You are not in a party.");
        boolean wasLeader = party.isLeader(player.getUUID());
        boolean partyStillExists = data.removeMember(party, player.getUUID());
        if (partyStillExists) {
            notifyParty(source.getServer(), party,
                    player.getGameProfile().getName() + " left the party.", null);
            if (wasLeader) {
                ServerPlayer newLeaderPlayer = source.getServer().getPlayerList().getPlayer(party.getLeader());
                String newLeaderName = newLeaderPlayer != null
                        ? newLeaderPlayer.getGameProfile().getName() : "another member";
                notifyParty(source.getServer(), party,
                        newLeaderName + " is now the party leader.", null);
            }
            data.syncAll();
        }
        return ok(source, "You left the party.");
    }

    /** /party kick <player> — leader only; removes a member from the party. */
    private static int kick(CommandSourceStack source, ServerPlayer player, ServerPlayer target) {
        if (player.getUUID().equals(target.getUUID())) {
            return fail(source, "You cannot kick yourself. Use /party leave or /party disband.");
        }
        PartyData data = PartyData.get(source.getServer());
        Party party = data.partyOf(player.getUUID()).orElse(null);
        if (party == null) return fail(source, "You are not in a party.");
        if (!party.isLeader(player.getUUID())) {
            return fail(source, "Only the party leader can kick members.");
        }
        if (!party.contains(target.getUUID())) {
            return fail(source, target.getGameProfile().getName() + " is not in your party.");
        }
        data.removeMember(party, target.getUUID());
        target.sendSystemMessage(Component.literal("You were kicked from the party."));
        notifyParty(source.getServer(), party,
                target.getGameProfile().getName() + " was kicked from the party.", null);
        data.syncAll();
        return ok(source, "Kicked " + target.getGameProfile().getName() + " from the party.");
    }

    /** /party or /party list — shows the current party roster. */
    private static int info(CommandSourceStack source, ServerPlayer player) {
        PartyData data = PartyData.get(source.getServer());
        Party party = data.partyOf(player.getUUID()).orElse(null);
        if (party == null) return fail(source, "You are not in a party.");
        String members = party.getMembers().stream()
                .map(id -> {
                    ServerPlayer online = source.getServer().getPlayerList().getPlayer(id);
                    String name = online != null ? online.getGameProfile().getName() : id.toString();
                    return party.isLeader(id) ? name + " ★" : name;
                })
                .collect(Collectors.joining(", "));
        return ok(source, "Party (" + party.getMembers().size() + "/" +
                PartyConfig.MAX_PARTY_MEMBERS.get() + "): " + members);
    }

    /** /party leader — shows who the current party leader is. */
    private static int leader(CommandSourceStack source, ServerPlayer player) {
        PartyData data = PartyData.get(source.getServer());
        Party party = data.partyOf(player.getUUID()).orElse(null);
        if (party == null) return fail(source, "You are not in a party.");
        ServerPlayer leaderPlayer = source.getServer().getPlayerList().getPlayer(party.getLeader());
        String leaderName = leaderPlayer != null
                ? leaderPlayer.getGameProfile().getName() : party.getLeader().toString();
        return ok(source, "Party leader: " + leaderName);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void sendInviteMessage(ServerPlayer target, String inviterName) {
        MutableComponent message = Component.literal(inviterName + " invited you to their party. ");
        message.append(Component.literal("[ACCEPT]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("/party accept")))));
        message.append(Component.literal(" "));
        message.append(Component.literal("[DECLINE]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.RED)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party decline"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("/party decline")))));
        target.sendSystemMessage(message);
    }

    private static void notifyParty(MinecraftServer server, Party party, String message, UUID except) {
        for (UUID memberId : party.getMembers()) {
            if (except != null && except.equals(memberId)) continue;
            ServerPlayer online = server.getPlayerList().getPlayer(memberId);
            if (online != null) {
                online.sendSystemMessage(Component.literal(message));
            }
        }
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