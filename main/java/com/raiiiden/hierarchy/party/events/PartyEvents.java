package com.raiiiden.hierarchy.party.events;

import com.raiiiden.hierarchy.party.data.PartyData;
import com.raiiiden.hierarchy.party.model.Party;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

public class PartyEvents {

    /**
     * On login: sync party state to the newly joined player, and refresh the
     * other party members' caches so they see the returning player.
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PartyData data = PartyData.get(player.server);
        // Sync this player's party state to their client
        data.syncToPlayer(player);
        // If they are in a party, sync all party members so everyone's cache
        // reflects this player being back online
        data.partyOf(player.getUUID()).ifPresent(party -> {
            for (UUID memberId : party.getMembers()) {
                if (memberId.equals(player.getUUID())) continue;
                ServerPlayer online = player.server.getPlayerList().getPlayer(memberId);
                if (online != null) data.syncToPlayer(online);
            }
        });
    }

    /**
     * On logout: remove the player from their party and notify remaining members.
     * If the player was leader, leadership transfers automatically inside removeMember.
     */
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PartyData data = PartyData.get(player.server);
        Party party = data.partyOf(player.getUUID()).orElse(null);
        if (party == null) return;

        boolean wasLeader = party.isLeader(player.getUUID());
        boolean partyStillExists = data.removeMember(party, player.getUUID());

        if (partyStillExists) {
            String playerName = player.getGameProfile().getName();
            for (UUID memberId : party.getMembers()) {
                ServerPlayer online = player.server.getPlayerList().getPlayer(memberId);
                if (online == null) continue;
                online.sendSystemMessage(Component.literal(playerName + " left the party."));
                if (wasLeader) {
                    ServerPlayer newLeaderPlayer = player.server.getPlayerList().getPlayer(party.getLeader());
                    String newLeaderName = newLeaderPlayer != null
                            ? newLeaderPlayer.getGameProfile().getName() : "another member";
                    online.sendSystemMessage(Component.literal(newLeaderName + " is now the party leader."));
                }
            }
            data.syncAll();
        }
    }
}