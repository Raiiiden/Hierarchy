package com.raiiiden.hierarchy.network;

import com.raiiiden.hierarchy.nameplate.ClientClanCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class SyncPartyMembersPacket {
    final Set<UUID> partymates;
    final UUID leaderId; // null when the receiving player is not in a party

    public SyncPartyMembersPacket(Set<UUID> partymates, UUID leaderId) {
        this.partymates = new HashSet<>(partymates);
        this.leaderId = leaderId;
    }

    public static void encode(SyncPartyMembersPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.partymates.size());
        for (UUID id : pkt.partymates) buf.writeUUID(id);
        buf.writeBoolean(pkt.leaderId != null);
        if (pkt.leaderId != null) buf.writeUUID(pkt.leaderId);
    }

    public static SyncPartyMembersPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Set<UUID> ids = new HashSet<>(size);
        for (int i = 0; i < size; i++) ids.add(buf.readUUID());
        UUID leaderId = buf.readBoolean() ? buf.readUUID() : null;
        return new SyncPartyMembersPacket(ids, leaderId);
    }

    public static void handle(SyncPartyMembersPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientClanCache.setPartymates(pkt.partymates, pkt.leaderId)));
        ctx.get().setPacketHandled(true);
    }
}