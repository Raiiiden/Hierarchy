package com.raiiiden.hierarchy.bounty.data;

import com.raiiiden.hierarchy.bounty.config.BountyConfig;
import com.raiiiden.hierarchy.bounty.model.Bounty;
import com.raiiiden.hierarchy.nameplate.NameplateUtil;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public class BountyData extends SavedData {
  private static final String DATA_NAME = "hierarchy_bounties";

  private final Map<UUID, Bounty> activeByTarget = new LinkedHashMap<>();
  private final Map<UUID, Long> cooldownUntil = new HashMap<>();
  private final Map<UUID, Long> recentPlayerKillUntil = new HashMap<>();

  public static BountyData get(MinecraftServer server) {
    return server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(BountyData::load, BountyData::new, DATA_NAME);
  }

  public static BountyData load(CompoundTag tag) {
    BountyData data = new BountyData();
    ListTag bounties = tag.getList("Bounties", 10);
    for (int i = 0; i < bounties.size(); i++) {
      CompoundTag entry = bounties.getCompound(i);
      Bounty bounty = new Bounty(entry.getUUID("Id"), entry.getUUID("Target"), entry.getString("TargetName"), entry.getLong("Amount"), entry.getLong("ExpiresAt"));
      bounty.pausedAt(entry.getLong("PausedAt"));
      ListTag contributions = entry.getList("Contributions", 10);
      for (int j = 0; j < contributions.size(); j++) {
        CompoundTag contribution = contributions.getCompound(j);
        bounty.contributions().put(contribution.getUUID("Player"), contribution.getLong("Amount"));
      }
      data.activeByTarget.put(bounty.targetId(), bounty);
    }
    readUuidLongMap(tag.getList("Cooldowns", 10), data.cooldownUntil);
    readUuidLongMap(tag.getList("RecentPlayerKills", 10), data.recentPlayerKillUntil);
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag) {
    ListTag bounties = new ListTag();
    for (Bounty bounty : activeByTarget.values()) {
      CompoundTag entry = new CompoundTag();
      entry.putUUID("Id", bounty.id());
      entry.putUUID("Target", bounty.targetId());
      entry.putString("TargetName", bounty.targetName());
      entry.putLong("Amount", bounty.amount());
      entry.putLong("ExpiresAt", bounty.expiresAt());
      entry.putLong("PausedAt", bounty.pausedAt());
      ListTag contributions = new ListTag();
      for (Map.Entry<UUID, Long> contribution : bounty.contributions().entrySet()) {
        CompoundTag contributionTag = new CompoundTag();
        contributionTag.putUUID("Player", contribution.getKey());
        contributionTag.putLong("Amount", contribution.getValue());
        contributions.add(contributionTag);
      }
      entry.put("Contributions", contributions);
      bounties.add(entry);
    }
    tag.put("Bounties", bounties);
    tag.put("Cooldowns", writeUuidLongMap(cooldownUntil));
    tag.put("RecentPlayerKills", writeUuidLongMap(recentPlayerKillUntil));
    return tag;
  }

  public Collection<Bounty> activeBounties() {
    return Collections.unmodifiableCollection(activeByTarget.values());
  }

  public Optional<Bounty> bounty(UUID targetId) {
    return Optional.ofNullable(activeByTarget.get(targetId));
  }

  public boolean hasActiveBounty(UUID targetId, long now) {
    expireIfNeeded(targetId, now);
    return activeByTarget.containsKey(targetId);
  }

  public Bounty place(UUID targetId, String targetName, UUID contributorId, long amount, long now) {
    Bounty bounty = new Bounty(UUID.randomUUID(), targetId, targetName, amount, now + BountyConfig.DURATION_SECONDS.get() * 1000L);
    bounty.contributions().put(contributorId, amount);
    activeByTarget.put(targetId, bounty);
    setDirty();
    return bounty;
  }

  public Bounty place(MinecraftServer server, UUID targetId, String targetName, UUID contributorId, long amount, long now) {
    Bounty bounty = place(targetId, targetName, contributorId, amount, now);
    refreshTargetNameplate(server, targetId);
    return bounty;
  }

  public void contribute(Bounty bounty, UUID contributorId, long amount) {
    bounty.contribute(contributorId, amount);
    setDirty();
  }

  public void clearAndCooldown(UUID targetId, long now) {
    activeByTarget.remove(targetId);
    cooldownUntil.put(targetId, now + BountyConfig.COOLDOWN_SECONDS.get() * 1000L);
    setDirty();
  }

  public void clearAndCooldown(UUID targetId, long now, MinecraftServer server) {
    clearAndCooldown(targetId, now);
    refreshTargetNameplate(server, targetId);
  }

  public void expireIfNeeded(UUID targetId, long now) {
    Bounty bounty = activeByTarget.get(targetId);
    if (bounty != null && bounty.pausedAt() <= 0L && bounty.expiresAt() <= now) {
      clearAndCooldown(targetId, now);
    }
  }

  public void expireIfNeeded(UUID targetId, long now, MinecraftServer server) {
    Bounty bounty = activeByTarget.get(targetId);
    if (bounty != null && bounty.pausedAt() <= 0L && bounty.expiresAt() <= now) {
      clearAndCooldown(targetId, now, server);
    }
  }

  public void expireAll(long now) {
    for (UUID targetId : activeByTarget.keySet().toArray(UUID[]::new)) {
      expireIfNeeded(targetId, now);
    }
  }

  public void expireAll(long now, MinecraftServer server) {
    for (UUID targetId : activeByTarget.keySet().toArray(UUID[]::new)) {
      expireIfNeeded(targetId, now, server);
    }
  }

  public boolean isCoolingDown(UUID targetId, long now) {
    return cooldownUntil.getOrDefault(targetId, 0L) > now;
  }

  public long cooldownUntil(UUID targetId) {
    return cooldownUntil.getOrDefault(targetId, 0L);
  }

  public void recordPlayerKill(UUID killerId, long now) {
    recentPlayerKillUntil.put(killerId, now + BountyConfig.RECENT_PLAYER_KILL_ELIGIBILITY_SECONDS.get() * 1000L);
    setDirty();
  }

  public boolean recentlyKilledPlayer(UUID playerId, long now) {
    return recentPlayerKillUntil.getOrDefault(playerId, 0L) > now;
  }

  public void onTargetLogout(UUID targetId, long now) {
    if (!BountyConfig.PAUSE_BOUNTY_WHILE_TARGET_OFFLINE.get()) {
      return;
    }
    Bounty bounty = activeByTarget.get(targetId);
    if (bounty != null && bounty.pausedAt() <= 0L) {
      bounty.pausedAt(now);
      setDirty();
    }
  }

  public void onTargetLogin(UUID targetId, String targetName, long now) {
    Bounty bounty = activeByTarget.get(targetId);
    if (bounty != null) {
      bounty.targetName(targetName);
      if (bounty.pausedAt() > 0L) {
        bounty.expiresAt(bounty.expiresAt() + now - bounty.pausedAt());
        bounty.pausedAt(0L);
      }
      setDirty();
    }
  }

  public boolean cancelContribution(UUID targetId, UUID contributorId, long now, MinecraftServer server) {
    Bounty bounty = activeByTarget.get(targetId);
    if (bounty == null) {
      return false;
    }
    Long contribution = bounty.contributions().remove(contributorId);
    if (contribution == null || contribution <= 0L) {
      return false;
    }
    long remaining = Math.max(0L, bounty.amount() - contribution);
    if (remaining <= 0L) {
      clearAndCooldown(targetId, now, server);
    } else {
      bounty.amount(remaining);
      setDirty();
    }
    return true;
  }

  private void refreshTargetNameplate(MinecraftServer server, UUID targetId) {
    if (server == null) {
      return;
    }
    ServerPlayer target = server.getPlayerList().getPlayer(targetId);
    if (target != null) {
      NameplateUtil.refresh(target);
    }
  }

  private static void readUuidLongMap(ListTag tag, Map<UUID, Long> target) {
    for (int i = 0; i < tag.size(); i++) {
      CompoundTag entry = tag.getCompound(i);
      target.put(entry.getUUID("Player"), entry.getLong("Value"));
    }
  }

  private static ListTag writeUuidLongMap(Map<UUID, Long> values) {
    ListTag tag = new ListTag();
    for (Map.Entry<UUID, Long> entry : values.entrySet()) {
      CompoundTag entryTag = new CompoundTag();
      entryTag.putUUID("Player", entry.getKey());
      entryTag.putLong("Value", entry.getValue());
      tag.add(entryTag);
    }
    return tag;
  }
}
