package com.raiiiden.hierarchy.network;

import com.raiiiden.hierarchy.nameplate.ClientClanCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class SyncClanMembersPacket {
    final Set<UUID> clanmateIds;
    final Map<UUID, String> clanmateRoles;
    final Map<UUID, String> allPlayerRoles;

    public SyncClanMembersPacket(Set<UUID> clanmateIds) {
        this(clanmateIds, Map.of(), Map.of());
    }

    public SyncClanMembersPacket(Set<UUID> clanmateIds, Map<UUID, String> clanmateRoles) {
        this(clanmateIds, clanmateRoles, Map.of());
    }

    public SyncClanMembersPacket(Set<UUID> clanmateIds, Map<UUID, String> clanmateRoles, Map<UUID, String> allPlayerRoles) {
        this.clanmateIds = new HashSet<>(clanmateIds);
        this.clanmateRoles = new HashMap<>(clanmateRoles);
        this.allPlayerRoles = new HashMap<>(allPlayerRoles);
    }

    public static void encode(SyncClanMembersPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.clanmateIds.size());
        for (UUID id : pkt.clanmateIds) buf.writeUUID(id);
        buf.writeVarInt(pkt.clanmateRoles.size());
        for (Map.Entry<UUID, String> entry : pkt.clanmateRoles.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
        buf.writeVarInt(pkt.allPlayerRoles.size());
        for (Map.Entry<UUID, String> entry : pkt.allPlayerRoles.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }

    public static SyncClanMembersPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Set<UUID> ids = new HashSet<>(size);
        for (int i = 0; i < size; i++) ids.add(buf.readUUID());
        int roleSize = buf.readVarInt();
        Map<UUID, String> roles = new HashMap<>(roleSize);
        for (int i = 0; i < roleSize; i++) {
            roles.put(buf.readUUID(), buf.readUtf());
        }
        int allRoleSize = buf.readVarInt();
        Map<UUID, String> allRoles = new HashMap<>(allRoleSize);
        for (int i = 0; i < allRoleSize; i++) {
            allRoles.put(buf.readUUID(), buf.readUtf());
        }
        return new SyncClanMembersPacket(ids, roles, allRoles);
    }

    public static void handle(SyncClanMembersPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    ClientClanCache.setClanmates(pkt.clanmateIds, pkt.clanmateRoles);
                    ClientClanCache.setAllPlayerRoles(pkt.allPlayerRoles);
                }));
        ctx.get().setPacketHandled(true);
    }
}