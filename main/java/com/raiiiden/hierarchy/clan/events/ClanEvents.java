package com.raiiiden.hierarchy.clan.events;

import com.raiiiden.hierarchy.clan.combat.PvpDamageDecision;
import com.raiiiden.hierarchy.clan.combat.PvpRelationship;
import com.raiiiden.hierarchy.clan.config.ClanCombatConfig;
import com.raiiiden.hierarchy.clan.data.ClanData;
import com.raiiiden.hierarchy.nameplate.NameplateUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClanEvents {
  private static final long WARNING_COOLDOWN_MILLIS = 10_000L;
  private final Map<String, Long> warningCooldowns = new HashMap<>();

  @SubscribeEvent
  public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    ClanData data = ClanData.get(player.server);
    data.rememberPlayer(player.getUUID(), player.getGameProfile().getName());
    NameplateUtil.refresh(player);
    for (String message : data.drainNotifications(player.getUUID())) {
      player.sendSystemMessage(Component.literal(message));
    }
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
}
