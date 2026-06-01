package com.raiiiden.hierarchy.bounty.events;

import com.raiiiden.hierarchy.bounty.BountyLogic;
import com.raiiiden.hierarchy.bounty.config.BountyConfig;
import com.raiiiden.hierarchy.bounty.data.BountyData;
import com.raiiiden.hierarchy.bounty.model.Bounty;
import com.raiiiden.hierarchy.clan.data.ClanData;
import com.raiiiden.hierarchy.clan.model.ClanPair;
import com.raiiiden.hierarchy.humanity.config.HumanityConfig;
import com.raiiiden.hierarchy.humanity.data.HumanityCombatData;
import com.raiiiden.hierarchy.humanity.data.HumanityData;
import com.raiiiden.hierarchy.nameplate.NameplateUtil;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class BountyEvents {
  private enum HumanityStanding {
    LOW,
    NEUTRAL,
    HIGH
  }

  @SubscribeEvent
  public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      BountyData.get(player.server).onTargetLogin(player.getUUID(), player.getGameProfile().getName(), System.currentTimeMillis());
    }
  }

  @SubscribeEvent
  public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      BountyData.get(player.server).onTargetLogout(player.getUUID(), System.currentTimeMillis());
    }
  }

  @SubscribeEvent
  public void onLivingAttack(LivingAttackEvent event) {
    if (!(event.getEntity() instanceof Player victim)) {
      return;
    }
    Entity source = event.getSource().getEntity();
    if (!(source instanceof ServerPlayer attacker) || attacker.getUUID().equals(victim.getUUID())) {
      return;
    }
    ClanData clans = ClanData.get(attacker.server);
    if (!clans.isLastPvpDamageValid(attacker.getUUID(), victim.getUUID())) {
      return;
    }
    HumanityCombatData.get(attacker.server).recordFirstAttack(ClanPair.key(attacker.getUUID(), victim.getUUID()), attacker.getUUID(), System.currentTimeMillis(), selfDefenseWindowMillis());
  }

  @SubscribeEvent
  public void onLivingDeath(LivingDeathEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer target)) {
      adjustHumanityForMobKill(event.getEntity(), event.getSource().getEntity());
      return;
    }
    Entity source = event.getSource().getEntity();
    if (!(source instanceof ServerPlayer killer) || killer.getUUID().equals(target.getUUID())) {
      return;
    }
    long now = System.currentTimeMillis();
    ClanData clans = ClanData.get(target.server);
    boolean validPvp = clans.isLastPvpDamageValid(killer.getUUID(), target.getUUID());
    if (!validPvp) {
      return;
    }
    BountyData bountyData = BountyData.get(target.server);
    bountyData.recordPlayerKill(killer.getUUID(), now);
    adjustHumanityForKill(killer, target, now);
    bountyData.expireIfNeeded(target.getUUID(), now);
    Bounty bounty = bountyData.bounty(target.getUUID()).orElse(null);
    if (bounty != null && BountyConfig.ENABLE_DOG_TAGS.get() && BountyLogic.relationshipAllowsDogTag(target.server, killer.getUUID(), target.getUUID())) {
      target.level().addFreshEntity(new ItemEntity(target.level(), target.getX(), target.getY(), target.getZ(), createDogTag(bounty, killer.getUUID())));
    }
  }

  private void adjustHumanityForKill(ServerPlayer killer, ServerPlayer target, long now) {
    if (!HumanityConfig.ENABLE_HUMANITY.get()) {
      return;
    }
    if (!BountyLogic.relationshipAllowsDogTag(killer.server, killer.getUUID(), target.getUUID())) {
      return;
    }
    String pair = ClanPair.key(killer.getUUID(), target.getUUID());
    HumanityCombatData combatData = HumanityCombatData.get(killer.server);
    long selfDefenseWindowMillis = selfDefenseWindowMillis();
    boolean killerAttackedFirst = combatData.attackedFirst(pair, killer.getUUID(), now, selfDefenseWindowMillis);
    boolean targetAttackedFirst = combatData.attackedFirst(pair, target.getUUID(), now, selfDefenseWindowMillis);
    int loss = playerKillLossFor(target, killerAttackedFirst, targetAttackedFirst);
    if (loss > 0) {
      HumanityData.get(killer.server).add(killer.getUUID(), -loss);
      NameplateUtil.refresh(killer);
    }
  }

  private void adjustHumanityForMobKill(LivingEntity target, Entity source) {
    if (!HumanityConfig.ENABLE_HUMANITY.get() || !(source instanceof ServerPlayer killer)) {
      return;
    }
    int gain = mobKillHumanityGain(target);
    if (gain <= 0) {
      return;
    }
    HumanityData.get(killer.server).add(killer.getUUID(), gain);
    NameplateUtil.refresh(killer);
  }

  private int mobKillHumanityGain(LivingEntity target) {
    ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
    if (entityId == null) {
      return 0;
    }
    String entityIdString = entityId.toString();
    for (String entry : HumanityConfig.MOB_KILL_HUMANITY_GAINS.get()) {
      int separator = entry.lastIndexOf('=');
      if (separator <= 0 || separator >= entry.length() - 1) {
        continue;
      }
      String configuredEntityId = entry.substring(0, separator).trim();
      if (!configuredEntityId.equals(entityIdString)) {
        continue;
      }
      try {
        return Integer.parseInt(entry.substring(separator + 1).trim());
      } catch (NumberFormatException ignored) {
        return 0;
      }
    }
    return 0;
  }

  private int playerKillLossFor(ServerPlayer target, boolean killerAttackedFirst, boolean targetAttackedFirst) {
    if (!killerAttackedFirst && !targetAttackedFirst) {
      return 0;
    }
    HumanityStanding standing = standingFor(HumanityData.get(target.server).humanity(target.getUUID()));
    if (targetAttackedFirst) {
      return switch (standing) {
        case LOW -> HumanityConfig.PLAYER_KILL_LOW_STANDING_LOSS_SELF_DEFENSE.get();
        case NEUTRAL -> HumanityConfig.PLAYER_KILL_NEUTRAL_STANDING_LOSS_SELF_DEFENSE.get();
        case HIGH -> HumanityConfig.PLAYER_KILL_HIGH_STANDING_LOSS_SELF_DEFENSE.get();
      };
    }
    return switch (standing) {
      case LOW -> HumanityConfig.PLAYER_KILL_LOW_STANDING_LOSS_ATTACKER_FIRST.get();
      case NEUTRAL -> HumanityConfig.PLAYER_KILL_NEUTRAL_STANDING_LOSS_ATTACKER_FIRST.get();
      case HIGH -> HumanityConfig.PLAYER_KILL_HIGH_STANDING_LOSS_ATTACKER_FIRST.get();
    };
  }

  private HumanityStanding standingFor(int humanity) {
    if (humanity <= HumanityConfig.LOW_HUMANITY_BOUNTY_THRESHOLD.get()) {
      return HumanityStanding.LOW;
    }
    if (humanity >= HumanityConfig.HIGH_HUMANITY_PROTECTED_THRESHOLD.get()) {
      return HumanityStanding.HIGH;
    }
    return HumanityStanding.NEUTRAL;
  }

  private long selfDefenseWindowMillis() {
    return HumanityConfig.SELF_DEFENSE_WINDOW_SECONDS.get() * 1000L;
  }

  public static ItemStack createDogTag(Bounty bounty, UUID killerId) {
    ItemStack stack = new ItemStack(Items.PAPER);
    stack.setHoverName(Component.literal("Dog Tag: " + bounty.targetName()).withStyle(ChatFormatting.GOLD));
    stack.getOrCreateTag().putUUID("BountyId", bounty.id());
    stack.getOrCreateTag().putUUID("BountyTarget", bounty.targetId());
    stack.getOrCreateTag().putUUID("BountyKiller", killerId);
    return stack;
  }

  public static ItemStack findDogTag(ServerPlayer player) {
    for (ItemStack stack : player.getInventory().items) {
      if (!stack.isEmpty() && stack.hasTag() && stack.getOrCreateTag().hasUUID("BountyTarget")) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }
}
