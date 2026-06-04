package com.raiiiden.hierarchy.network;

import com.raiiiden.hierarchy.nameplate.ClientClanCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncNameplateConfigPacket {
    final double maxRender;
    final double teammateRender;
    final double arrowRender;

    public SyncNameplateConfigPacket(double maxRender, double teammateRender, double arrowRender) {
        this.maxRender = maxRender;
        this.teammateRender = teammateRender;
        this.arrowRender = arrowRender;
    }

    public static void encode(SyncNameplateConfigPacket pkt, FriendlyByteBuf buf) {
        buf.writeDouble(pkt.maxRender);
        buf.writeDouble(pkt.teammateRender);
        buf.writeDouble(pkt.arrowRender);
    }

    public static SyncNameplateConfigPacket decode(FriendlyByteBuf buf) {
        return new SyncNameplateConfigPacket(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(SyncNameplateConfigPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientClanCache.setNameplateDistances(
                                pkt.maxRender, pkt.teammateRender, pkt.arrowRender)));
        ctx.get().setPacketHandled(true);
    }
}