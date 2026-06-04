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

public final class SyncClanMembersPacket {
    final Set<UUID> clanmateIds;

    public SyncClanMembersPacket(Set<UUID> clanmateIds) {
        this.clanmateIds = new HashSet<>(clanmateIds);
    }

    public static void encode(SyncClanMembersPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.clanmateIds.size());
        for (UUID id : pkt.clanmateIds) buf.writeUUID(id);
    }

    public static SyncClanMembersPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Set<UUID> ids = new HashSet<>(size);
        for (int i = 0; i < size; i++) ids.add(buf.readUUID());
        return new SyncClanMembersPacket(ids);
    }

    public static void handle(SyncClanMembersPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientClanCache.setClanmates(pkt.clanmateIds)));
        ctx.get().setPacketHandled(true);
    }
}