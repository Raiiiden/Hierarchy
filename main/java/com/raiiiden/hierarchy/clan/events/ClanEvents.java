package com.raiiiden.hierarchy.clan.events;

import com.raiiiden.hierarchy.clan.combat.PvpDamageDecision;
import com.raiiiden.hierarchy.clan.combat.PvpRelationship;
import com.raiiiden.hierarchy.clan.config.ClanCombatConfig;
import com.raiiiden.hierarchy.clan.data.ClanData;
import com.raiiiden.hierarchy.clan.model.Clan;
import com.raiiiden.hierarchy.nameplate.NameplateConfig;
import com.raiiiden.hierarchy.nameplate.NameplateUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.raiiiden.hierarchy.nameplate.TabListManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClanEvents {
  private static final long WARNING_COOLDOWN_MILLIS = 10_000L;
  private final Map<String, Long> warningCooldowns = new HashMap<>();

  @SubscribeEvent
  public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    ClanData data = ClanData.get(player.server);
    data.rememberPlayer(player.getUUID(), player.getGameProfile().getName());
    NameplateUtil.refresh(player);
    for (String message : data.drainNotifications(player.getUUID())) {
      player.sendSystemMessage(Component.literal(message));
    }
    TabListManager.onPlayerJoined(player);
    for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
      if (other == player) continue;
      NameplateUtil.refreshForViewer(player, other);
      NameplateUtil.refreshForViewer(other, player);
    }

    // Sync server-side nameplate distances so the client uses the server's config values
    com.raiiiden.hierarchy.network.NetworkHandler.CHANNEL.sendTo(
            new com.raiiiden.hierarchy.network.SyncNameplateConfigPacket(
                    NameplateConfig.MAX_RENDER_DISTANCE_BLOCKS.get(),
                    NameplateConfig.TEAMMATE_RENDER_DISTANCE_BLOCKS.get(),
                    NameplateConfig.ARROW_RENDER_DISTANCE_BLOCKS.get()),
            player.connection.connection,
            net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);

    // Populate client clan cache — fixes the "green arrow on everyone" bug
    data.syncClanMembersToAllOnlinePlayers();
  }

  @SubscribeEvent
  public void onTabListName(PlayerEvent.TabListNameFormat event) {
    if (!ClanCombatConfig.ENABLE_CLANS.get()) return;
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    ClanData data = ClanData.get(player.server);
    Clan clan = data.clanOf(player.getUUID()).orElse(null);
    if (clan == null || clan.getTag().isBlank()) return;
    event.setDisplayName(
            Component.literal("[" + clan.getTag() + "] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(player.getGameProfile().getName())
                            .withStyle(ChatFormatting.WHITE))
    );
  }

  @SubscribeEvent
  public void onServerChat(ServerChatEvent event) {
    if (!ClanCombatConfig.ENABLE_CLANS.get()) return;

    ServerPlayer sender = event.getPlayer();
    ClanData data = ClanData.get(sender.server);
    Clan senderClan = data.clanOf(sender.getUUID()).orElse(null);

    if (senderClan == null || senderClan.getTag().isBlank()) return;

    event.setCanceled(true);

    String tag      = "[" + senderClan.getTag() + "] ";
    String name     = sender.getGameProfile().getName();
    Component body  = event.getMessage();

    for (ServerPlayer viewer : sender.server.getPlayerList().getPlayers()) {
      Clan viewerClan = data.clanOf(viewer.getUUID()).orElse(null);
      ChatFormatting color = TabListManager.tagColor(viewerClan, senderClan, data);

      MutableComponent line = Component.literal(tag).withStyle(color)
              .append(Component.literal("<" + name + "> ").withStyle(ChatFormatting.WHITE))
              .append(body.copy().withStyle(ChatFormatting.WHITE));
      viewer.sendSystemMessage(line);
    }

    // Keep the console log that canceling would otherwise suppress
    sender.server.sendSystemMessage(
            Component.literal("<" + name + "> " + body.getString())
    );
  }

  @SubscribeEvent
  public void onLivingAttack(LivingAttackEvent event) {
    if (!ClanCombatConfig.ENABLE_CLANS.get()) {
      return;
    }
    if (!(event.getEntity() instanceof Player victim)) {
      return;
    }
    ServerPlayer attacker = resolveAttacker(event.getSource());
    if (attacker == null || attacker.server == null) {
      return;
    }
    if (victim.getUUID().equals(attacker.getUUID())) {
      return;
    }
    ClanData data = ClanData.get(attacker.server);
    long now = System.currentTimeMillis();
    PvpDamageDecision decision = data.evaluateDamage(attacker.getUUID(), victim.getUUID(), now);
    if (decision.relationship() == PvpRelationship.SAME_CLAN) {
      Clan clan = data.clanOf(attacker.getUUID()).orElse(null);
      if (clan != null && clan.isInternalFriendlyFireEnabled()) {
        // FF is on — allow the hit, don't cancel
        return;
      }
      event.setCanceled(true);
      warn(attacker, victim.getUUID(), "same_clan", "You cannot damage members of your own clan.", now);
      return;
    }
    if (decision.relationship() == PvpRelationship.ALLIED_FRIENDLY_FIRE_OFF) {
      event.setCanceled(true);
      warn(attacker, victim.getUUID(), "friendly_fire", "Friendly fire is disabled.", now);
      return;
    }
    if (decision.relationship() == PvpRelationship.DELAYED) {
      event.setCanceled(true);
      warn(attacker, victim.getUUID(), "pvp_delay", "PvP between these clans is temporarily disabled.", now);
    }
  }

  private void warn(ServerPlayer attacker, UUID victimId, String type, String message, long now) {
    String key = attacker.getUUID() + ":" + victimId + ":" + type;
    long nextAllowed = warningCooldowns.getOrDefault(key, 0L);
    if (nextAllowed <= now) {
      attacker.sendSystemMessage(Component.literal(message));
      warningCooldowns.put(key, now + WARNING_COOLDOWN_MILLIS);
    }
  }

  private ServerPlayer resolveAttacker(DamageSource source) {
    Entity sourceEntity = source.getEntity();
    if (sourceEntity instanceof ServerPlayer player && shouldHandle(source)) {
      return player;
    }
    Entity directEntity = source.getDirectEntity();
    if (directEntity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player && shouldHandle(source)) {
      return player;
    }
    if (directEntity instanceof PrimedTnt tnt && tnt.getOwner() instanceof ServerPlayer player && ClanCombatConfig.HANDLE_EXPLOSIONS.get()) {
      return player;
    }
    if (sourceEntity instanceof LivingEntity living && living.getLastHurtByMob() instanceof ServerPlayer player && ClanCombatConfig.HANDLE_ENVIRONMENTAL_ATTRIBUTION.get()) {
      return player;
    }
    return null;
  }

  private boolean shouldHandle(DamageSource source) {
    if (source.is(DamageTypeTags.IS_EXPLOSION)) {
      return ClanCombatConfig.HANDLE_EXPLOSIONS.get();
    }
    if (source.is(DamageTypeTags.IS_FIRE)) {
      return ClanCombatConfig.HANDLE_FIRE.get();
    }
    return true;
  }
  @SubscribeEvent
  public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    // Respawn recreates the player entity with empty synched data, so re-sync the
    // nameplate in BOTH directions (the respawned player's plate out to everyone,
    // and everyone else's plates back to the respawned player) — same as a fresh join.
    NameplateUtil.refreshOnJoin(player);
  }
  @SubscribeEvent
  public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    // Changing dimension re-tracks every entity on the client, dropping per-viewer
    // overrides — re-sync nameplates in both directions just like respawn/join.
    NameplateUtil.refreshOnJoin(player);
  }

  @SubscribeEvent
  public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      // Don't leave an armed /clan disband confirmation hanging across sessions.
      com.raiiiden.hierarchy.clan.commands.ClanCommands.clearPendingDisband(player.getUUID());
    }
  }
}
