package com.raiiiden.hierarchy.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class NetworkHandler {
    private NetworkHandler() {}

    public static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("hierarchy", "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int nextId = 0;

    public static void register() {
        CHANNEL.registerMessage(nextId++,
                SyncClanMembersPacket.class,
                SyncClanMembersPacket::encode,
                SyncClanMembersPacket::decode,
                SyncClanMembersPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(nextId++,
                SyncNameplateConfigPacket.class,
                SyncNameplateConfigPacket::encode,
                SyncNameplateConfigPacket::decode,
                SyncNameplateConfigPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}