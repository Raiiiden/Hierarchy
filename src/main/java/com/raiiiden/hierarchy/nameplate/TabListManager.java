package com.raiiiden.hierarchy.nameplate;

import com.raiiiden.hierarchy.clan.data.ClanData;
import com.raiiiden.hierarchy.clan.model.Clan;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.util.UUID;

public final class TabListManager {
    private TabListManager() {}

    private static final Field FIELD_TAB_LIST_DISPLAY_NAME;
    private static final Field FIELD_HAS_TAB_LIST_NAME;

    static {
        try {
            FIELD_TAB_LIST_DISPLAY_NAME = ServerPlayer.class.getDeclaredField("tabListDisplayName");
            FIELD_TAB_LIST_DISPLAY_NAME.setAccessible(true);
            FIELD_HAS_TAB_LIST_NAME = ServerPlayer.class.getDeclaredField("hasTabListName");
            FIELD_HAS_TAB_LIST_NAME.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Could not find ServerPlayer tab list fields", e);
        }
    }

    public static void onPlayerJoined(ServerPlayer joined) {
        ClanData data = ClanData.get(joined.server);
        for (ServerPlayer other : joined.server.getPlayerList().getPlayers()) {
            if (other == joined) continue;
            sendUpdate(joined, other, data);
            sendUpdate(other, joined, data);
        }
    }

    public static void refreshAll(MinecraftServer server) {
        ClanData data = ClanData.get(server);
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            for (ServerPlayer target : server.getPlayerList().getPlayers()) {
                if (viewer == target) continue;
                sendUpdate(viewer, target, data);
            }
        }
    }

    private static void sendUpdate(ServerPlayer viewer, ServerPlayer target, ClanData data) {
        Clan viewerClan = data.clanOf(viewer.getUUID()).orElse(null);
        Component displayName = buildTabName(data, viewerClan, target);

        try {
            Component previousName = (Component) FIELD_TAB_LIST_DISPLAY_NAME.get(target);
            boolean previousHas = (boolean) FIELD_HAS_TAB_LIST_NAME.get(target);

            FIELD_TAB_LIST_DISPLAY_NAME.set(target, displayName);
            FIELD_HAS_TAB_LIST_NAME.set(target, true);

            ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                    target
            );

            FIELD_TAB_LIST_DISPLAY_NAME.set(target, previousName);
            FIELD_HAS_TAB_LIST_NAME.set(target, previousHas);

            viewer.connection.send(packet);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not access ServerPlayer tab list fields", e);
        }
    }

    private static Component buildTabName(ClanData data, Clan viewerClan, ServerPlayer target) {
        Clan targetClan = data.clanOf(target.getUUID()).orElse(null);
        String username = target.getGameProfile().getName();

        if (targetClan == null || targetClan.getTag().isBlank()) {
            return Component.literal(username).withStyle(ChatFormatting.WHITE);
        }

        ChatFormatting color = tagColor(viewerClan, targetClan, data);
        MutableComponent prefix = Component.literal("[" + targetClan.getTag() + "] ").withStyle(color);
        if (NameplateConfig.SHOW_CLAN_NAME_IN_TABLIST.get() && !targetClan.getName().isBlank()) {
            String name = truncate(targetClan.getName(), NameplateConfig.TABLIST_MAX_CLAN_NAME_LENGTH.get());
            prefix.append(Component.literal(name + " ").withStyle(color));
        }
        return prefix.append(Component.literal(username).withStyle(ChatFormatting.WHITE));
    }

    // Truncates a clan name to {@code max} characters, appending an ellipsis when shortened.
    public static String truncate(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        if (max <= 1) return value.substring(0, Math.max(0, max));
        return value.substring(0, max - 1) + "…";
    }

    // Shared color logic used by tab list, chat, and nameplates.
    // GREEN = same clan, BLUE = allied, RED = everything else.
    public static ChatFormatting tagColor(Clan viewerClan, Clan targetClan, ClanData data) {
        if (viewerClan == null) return ChatFormatting.RED;
        UUID vId = viewerClan.getId();
        UUID tId = targetClan.getId();
        if (vId.equals(tId)) return ChatFormatting.GREEN;
        if (data.areAllied(vId, tId)) return ChatFormatting.BLUE;
        return ChatFormatting.RED;
    }
}