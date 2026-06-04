package com.raiiiden.hierarchy.nameplate;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public final class ClientClanCache {
    private ClientClanCache() {}

    private static final Set<UUID> clanmates =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<UUID, String> clanmateRoles = new ConcurrentHashMap<>();
    private static final Map<UUID, String> allPlayerRoles = new ConcurrentHashMap<>();

    private static volatile double syncedMaxRender      = -1;
    private static volatile double syncedTeammateRender = -1;
    private static volatile double syncedArrowRender    = -1;

    public static void setClanmates(Set<UUID> ids) {
        clanmates.clear();
        clanmates.addAll(ids);
        clanmateRoles.clear();
    }

    public static void setClanmates(Set<UUID> ids, Map<UUID, String> roles) {
        clanmates.clear();
        clanmates.addAll(ids);
        clanmateRoles.clear();
        clanmateRoles.putAll(roles);
    }

    public static void setAllPlayerRoles(Map<UUID, String> roles) {
        allPlayerRoles.clear();
        allPlayerRoles.putAll(roles);
    }

    public static boolean isClanmate(UUID id) {
        return clanmates.contains(id);
    }

    public static String clanmateRole(UUID id) {
        return clanmateRoles.getOrDefault(id, "");
    }

    public static String playerRole(UUID id) {
        return allPlayerRoles.getOrDefault(id, "");
    }

    public static void setNameplateDistances(double max, double teammate, double arrow) {
        syncedMaxRender      = max;
        syncedTeammateRender = teammate;
        syncedArrowRender    = arrow;
    }

    public static double maxRenderDistance() {
        return syncedMaxRender >= 0 ? syncedMaxRender
                : NameplateConfig.MAX_RENDER_DISTANCE_BLOCKS.get();
    }

    public static double teammateRenderDistance() {
        return syncedTeammateRender >= 0 ? syncedTeammateRender
                : NameplateConfig.TEAMMATE_RENDER_DISTANCE_BLOCKS.get();
    }

    public static double arrowRenderDistance() {
        return syncedArrowRender >= 0 ? syncedArrowRender
                : NameplateConfig.ARROW_RENDER_DISTANCE_BLOCKS.get();
    }

    public static void clear() {
        clanmates.clear();
        clanmateRoles.clear();
        allPlayerRoles.clear();
        syncedMaxRender      = -1;
        syncedTeammateRender = -1;
        syncedArrowRender    = -1;
    }
}