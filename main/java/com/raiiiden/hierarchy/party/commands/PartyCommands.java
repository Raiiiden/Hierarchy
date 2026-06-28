package com.raiiiden.hierarchy.party.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.raiiiden.hierarchy.party.config.PartyConfig;
import com.raiiiden.hierarchy.party.data.PartyData;
import com.raiiiden.hierarchy.party.model.Party;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
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
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                                .map(p -> p.getGameProfile().getName()), builder))
                                .executes(ctx -> invite(ctx.getSource(), player(ctx.getSource()),
                                        StringArgumentType.getString(ctx, "player")))))
                .then(Commands.literal("help")
                        .executes(ctx -> help(ctx.getSource())))
                .then(Commands.literal("invites")
                        .executes(ctx -> invites(ctx.getSource(), player(ctx.getSource()))))
                .then(Commands.literal("accept")
                        .executes(ctx -> accept(ctx.getSource(), player(ctx.getSource()))))
                // /party deny is the primary verb; /party decline kept as an alias.
                .then(Commands.literal("deny")
                        .executes(ctx -> decline(ctx.getSource(), player(ctx.getSource()))))
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
    private static int invite(CommandSourceStack source, ServerPlayer player, String targetName) {
        MinecraftServer server = source.getServer();
        // Resolve the target by name — online first, then the server profile cache (offline).
        ServerPlayer onlineTarget = server.getPlayerList().getPlayerByName(targetName);
        UUID targetId;
        String targetDisplayName;
        if (onlineTarget != null) {
            targetId = onlineTarget.getUUID();
            targetDisplayName = onlineTarget.getGameProfile().getName();
        } else {
            GameProfile profile = server.getProfileCache() == null
                    ? null : server.getProfileCache().get(targetName).orElse(null);
            if (profile == null) {
                return fail(source, "No player named '" + targetName + "' has been seen on this server.");
            }
            targetId = profile.getId();
            targetDisplayName = profile.getName();
        }
        if (player.getUUID().equals(targetId)) {
            return fail(source, "You cannot invite yourself.");
        }
        PartyData data = PartyData.get(server);
        Party party = data.partyOf(player.getUUID()).orElse(null);
        if (party == null) {
            // Auto-create a party so the leader can invite straight away
            party = data.createParty(player.getUUID(), System.currentTimeMillis());
        }
        if (!party.isLeader(player.getUUID())) {
            return fail(source, "Only the party leader can invite players.");
        }
        if (data.partyOf(targetId).isPresent()) {
            return fail(source, targetDisplayName + " is already in a party.");
        }
        if (party.isFull(PartyConfig.MAX_PARTY_MEMBERS.get())) {
            return fail(source, "Your party is full (" + PartyConfig.MAX_PARTY_MEMBERS.get() + " members max).");
        }
        if (data.hasPendingInvite(targetId)) {
            return fail(source, targetDisplayName + " already has a pending party invite.");
        }
        data.invite(targetId, party.getId());
        if (onlineTarget != null) {
            sendInviteMessage(onlineTarget, player.getGameProfile().getName());
            return ok(source, "Invited " + targetDisplayName + " to your party.");
        }
        // Offline invite: they'll see it with /party invites when they log in (if it hasn't expired).
        return ok(source, "Invited " + targetDisplayName + " to your party. They will see it with /party invites when they log in.");
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

    /** /party help — lists the available party subcommands. */
    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Party commands:").withStyle(ChatFormatting.GOLD), false);
        String[][] entries = {
                {"/party", "Show your party roster"},
                {"/party create", "Create a party"},
                {"/party invite <player>", "Invite a player (auto-creates a party)"},
                {"/party invites", "Show your pending invite"},
                {"/party accept", "Accept your pending invite"},
                {"/party deny", "Decline your pending invite"},
                {"/party leave", "Leave your party"},
                {"/party kick <player>", "Remove a member (leader only)"},
                {"/party leader", "Show the party leader"},
                {"/party disband", "Disband the party (leader only)"},
        };
        for (String[] entry : entries) {
            source.sendSuccess(() -> Component.literal(entry[0]).withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(" — " + entry[1]).withStyle(ChatFormatting.GRAY)), false);
        }
        return 1;
    }

    /** /party invites — shows the player's pending party invite, if any. */
    private static int invites(CommandSourceStack source, ServerPlayer player) {
        PartyData data = PartyData.get(source.getServer());
        Party party = data.pendingInvite(player.getUUID()).orElse(null);
        if (party == null) {
            return ok(source, "You have no pending party invites.");
        }
        ServerPlayer leader = source.getServer().getPlayerList().getPlayer(party.getLeader());
        String leaderName = leader != null
                ? leader.getGameProfile().getName() : party.getLeader().toString();
        long remaining = data.pendingInviteExpiresAt(player.getUUID()) - System.currentTimeMillis();
        source.sendSuccess(() -> Component.literal("Pending party invite:"), false);
        MutableComponent line = Component.literal("- " + leaderName + "'s party ")
                .append(button("[ACCEPT]", "/party accept", ChatFormatting.GREEN))
                .append(Component.literal(" "))
                .append(button("[DENY]", "/party deny", ChatFormatting.RED))
                .append(Component.literal(" (expires in " + formatDuration(remaining) + ")")
                        .withStyle(ChatFormatting.GRAY));
        source.sendSuccess(() -> line, false);
        return 1;
    }

    // Human-readable countdown, e.g. "59s", "1m 0s".
    private static String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes > 0 ? minutes + "m " + seconds + "s" : seconds + "s";
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
        message.append(button("[ACCEPT]", "/party accept", ChatFormatting.GREEN));
        message.append(Component.literal(" "));
        message.append(button("[DENY]", "/party deny", ChatFormatting.RED));
        message.append(Component.literal(" (expires soon — review with /party invites)")
                .withStyle(ChatFormatting.GRAY));
        target.sendSystemMessage(message);
    }

    private static MutableComponent button(String label, String command, ChatFormatting color) {
        return Component.literal(label)
                .withStyle(style -> style
                        .withColor(color)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal(command))));
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
        source.sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message).withStyle(ChatFormatting.RED));
        return 0;
    }
}