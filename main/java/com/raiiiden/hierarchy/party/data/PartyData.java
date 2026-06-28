package com.raiiiden.hierarchy.party.data;

import com.raiiiden.hierarchy.party.model.Party;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class PartyData extends SavedData {
    private static final String DATA_NAME = "hierarchy_parties";

    // partyId → Party
    private final Map<UUID, Party> parties = new LinkedHashMap<>();
    // playerId → partyId
    private final Map<UUID, UUID> playerParty = new HashMap<>();
    // inviteeId → pending invite (which party, and when it was sent)
    private final Map<UUID, PendingInvite> pendingInvites = new HashMap<>();

    private record PendingInvite(UUID partyId, long createdAt) {}

    private MinecraftServer server;

    public static PartyData get(MinecraftServer server) {
        PartyData data = server.getLevel(Level.OVERWORLD)
                .getDataStorage()
                .computeIfAbsent(PartyData::load, PartyData::new, DATA_NAME);
        data.server = server;
        return data;
    }

    public static PartyData load(CompoundTag tag) {
        PartyData data = new PartyData();
        ListTag partyTags = tag.getList("Parties", 10);
        for (int i = 0; i < partyTags.size(); i++) {
            CompoundTag partyTag = partyTags.getCompound(i);
            UUID id = partyTag.getUUID("Id");
            UUID leader = partyTag.getUUID("Leader");
            long createdAt = partyTag.getLong("CreatedAt");
            Party party = new Party(id, leader, createdAt);
            party.getMembers().clear();
            ListTag memberTags = partyTag.getList("Members", 8);
            for (int j = 0; j < memberTags.size(); j++) {
                party.getMembers().add(UUID.fromString(memberTags.getString(j)));
            }
            // Discard solo or empty parties on load — parties are temporary
            if (party.getMembers().size() < 2) continue;
            data.parties.put(id, party);
            for (UUID memberId : party.getMembers()) {
                data.playerParty.put(memberId, id);
            }
        }
        ListTag inviteTags = tag.getList("PendingInvites", 10);
        for (int i = 0; i < inviteTags.size(); i++) {
            CompoundTag entry = inviteTags.getCompound(i);
            UUID inviteeId = entry.getUUID("Player");
            UUID partyId = entry.getUUID("Party");
            long createdAt = entry.getLong("CreatedAt");
            // Only restore invites for parties that survived the load check above
            if (data.parties.containsKey(partyId)) {
                data.pendingInvites.put(inviteeId, new PendingInvite(partyId, createdAt));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag partyTags = new ListTag();
        for (Party party : parties.values()) {
            if (party.getMembers().size() < 2) continue;
            CompoundTag partyTag = new CompoundTag();
            partyTag.putUUID("Id", party.getId());
            partyTag.putUUID("Leader", party.getLeader());
            partyTag.putLong("CreatedAt", party.getCreatedAt());
            ListTag memberTags = new ListTag();
            for (UUID memberId : party.getMembers()) {
                memberTags.add(StringTag.valueOf(memberId.toString()));
            }
            partyTag.put("Members", memberTags);
            partyTags.add(partyTag);
        }
        tag.put("Parties", partyTags);
        ListTag inviteTags = new ListTag();
        for (Map.Entry<UUID, PendingInvite> entry : pendingInvites.entrySet()) {
            PendingInvite invite = entry.getValue();
            if (!parties.containsKey(invite.partyId())) continue;
            CompoundTag inviteTag = new CompoundTag();
            inviteTag.putUUID("Player", entry.getKey());
            inviteTag.putUUID("Party", invite.partyId());
            inviteTag.putLong("CreatedAt", invite.createdAt());
            inviteTags.add(inviteTag);
        }
        tag.put("PendingInvites", inviteTags);
        return tag;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public Optional<Party> partyOf(UUID playerId) {
        UUID partyId = playerParty.get(playerId);
        return partyId == null ? Optional.empty() : Optional.ofNullable(parties.get(partyId));
    }

    public Optional<Party> party(UUID partyId) {
        return Optional.ofNullable(parties.get(partyId));
    }

    public boolean arePartied(UUID a, UUID b) {
        UUID partyIdA = playerParty.get(a);
        if (partyIdA == null) return false;
        return partyIdA.equals(playerParty.get(b));
    }

    private static long inviteTtlMillis() {
        return com.raiiiden.hierarchy.party.config.PartyConfig.INVITE_EXPIRY_SECONDS.get() * 1000L;
    }

    public boolean hasPendingInvite(UUID playerId) {
        return livePendingInvite(playerId).isPresent();
    }

    public Optional<Party> pendingInvite(UUID playerId) {
        return livePendingInvite(playerId).map(invite -> parties.get(invite.partyId()));
    }

    /** Absolute time (millis) the player's invite expires, or 0 if they have none. */
    public long pendingInviteExpiresAt(UUID playerId) {
        return livePendingInvite(playerId)
                .map(invite -> invite.createdAt() + inviteTtlMillis())
                .orElse(0L);
    }

    /**
     * Returns the player's pending invite if it is still valid, dropping it first
     * when it has expired or its party no longer exists.
     */
    private Optional<PendingInvite> livePendingInvite(UUID playerId) {
        PendingInvite invite = pendingInvites.get(playerId);
        if (invite == null) {
            return Optional.empty();
        }
        boolean expired = System.currentTimeMillis() - invite.createdAt() > inviteTtlMillis();
        if (expired || !parties.containsKey(invite.partyId())) {
            pendingInvites.remove(playerId);
            setDirty();
            if (expired && server != null) {
                ServerPlayer online = server.getPlayerList().getPlayer(playerId);
                if (online != null) {
                    online.sendSystemMessage(Component.literal("Your party invitation has expired."));
                }
            }
            return Optional.empty();
        }
        return Optional.of(invite);
    }

    /** Sweeps every player's invite, removing expired/orphaned entries. */
    public void purgeExpiredInvites() {
        for (UUID playerId : new ArrayList<>(pendingInvites.keySet())) {
            livePendingInvite(playerId);
        }
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    public Party createParty(UUID leaderId, long now) {
        Party party = new Party(UUID.randomUUID(), leaderId, now);
        parties.put(party.getId(), party);
        playerParty.put(leaderId, party.getId());
        setDirty();
        return party;
    }

    public void disbandParty(Party party) {
        for (UUID memberId : new ArrayList<>(party.getMembers())) {
            playerParty.remove(memberId);
        }
        pendingInvites.values().removeIf(invite -> invite.partyId().equals(party.getId()));
        parties.remove(party.getId());
        setDirty();
    }

    public void addMember(Party party, UUID playerId) {
        party.getMembers().add(playerId);
        playerParty.put(playerId, party.getId());
        pendingInvites.remove(playerId);
        setDirty();
    }

    /**
     * Removes a member from the party.
     * <p>If the removed player was the leader, leadership is transferred to the next member.
     * If the party becomes empty it is automatically disbanded.
     *
     * @return {@code true} if the party still exists after removal; {@code false} if it was disbanded.
     */
    public boolean removeMember(Party party, UUID playerId) {
        party.getMembers().remove(playerId);
        playerParty.remove(playerId);
        if (party.getMembers().isEmpty()) {
            parties.remove(party.getId());
            pendingInvites.values().removeIf(invite -> invite.partyId().equals(party.getId()));
            setDirty();
            return false;
        }
        if (party.isLeader(playerId)) {
            // Transfer leadership to the next member in insertion order
            UUID newLeader = party.getMembers().iterator().next();
            party.setLeader(newLeader);
        }
        setDirty();
        return true;
    }

    public void invite(UUID inviteeId, UUID partyId) {
        pendingInvites.put(inviteeId, new PendingInvite(partyId, System.currentTimeMillis()));
        setDirty();
    }

    public void clearInvite(UUID playerId) {
        pendingInvites.remove(playerId);
        setDirty();
    }

    // -------------------------------------------------------------------------
    // Client sync
    // -------------------------------------------------------------------------

    /**
     * Sends the party membership packet to a single player so their client can
     * render party indicators correctly.
     */
    public void syncToPlayer(ServerPlayer player) {
        if (server == null) return;
        Party party = partyOf(player.getUUID()).orElse(null);
        Set<UUID> mates = new HashSet<>();
        UUID leaderId = null;
        if (party != null) {
            leaderId = party.getLeader();
            for (UUID memberId : party.getMembers()) {
                if (!memberId.equals(player.getUUID())) {
                    mates.add(memberId);
                }
            }
        }
        com.raiiiden.hierarchy.network.NetworkHandler.CHANNEL.sendTo(
                new com.raiiiden.hierarchy.network.SyncPartyMembersPacket(mates, leaderId),
                player.connection.connection,
                net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
    }

    /** Sends party membership packets to every currently online player. */
    public void syncAll() {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncToPlayer(player);
        }
    }
}