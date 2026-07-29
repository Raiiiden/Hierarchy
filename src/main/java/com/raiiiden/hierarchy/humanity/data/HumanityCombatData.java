package com.raiiiden.hierarchy.humanity.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public class HumanityCombatData extends SavedData {
  private static final String DATA_NAME = "hierarchy_humanity_combat";

  // Key is "attackerId:victimId" — directional, not symmetric
  private final Map<String, Long> firstAttackTimestamps = new HashMap<>();

  public static HumanityCombatData get(MinecraftServer server) {
    return server.getLevel(Level.OVERWORLD)
            .getDataStorage()
            .computeIfAbsent(HumanityCombatData::load, HumanityCombatData::new, DATA_NAME);
  }

  public static HumanityCombatData load(CompoundTag tag) {
    HumanityCombatData data = new HumanityCombatData();
    ListTag entries = tag.getList("FirstAttacks", 10);
    for (int i = 0; i < entries.size(); i++) {
      CompoundTag entry = entries.getCompound(i);
      data.firstAttackTimestamps.put(entry.getString("Pair"), entry.getLong("Timestamp"));
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag) {
    ListTag entries = new ListTag();
    for (Map.Entry<String, Long> entry : firstAttackTimestamps.entrySet()) {
      CompoundTag value = new CompoundTag();
      value.putString("Pair", entry.getKey());
      value.putLong("Timestamp", entry.getValue());
      entries.add(value);
    }
    tag.put("FirstAttacks", entries);
    return tag;
  }

  // Directional key — attacker -> victim, not symmetric
  private static String directedKey(UUID attackerId, UUID victimId) {
    return attackerId + ">" + victimId;
  }

  // Record that attackerId hit victimId first, only if no recent record exists
  public void recordAttack(UUID attackerId, UUID victimId, long now, long windowMillis) {
    String key = directedKey(attackerId, victimId);
    long previous = firstAttackTimestamps.getOrDefault(key, 0L);
    if (previous <= 0L || now - previous > windowMillis) {
      firstAttackTimestamps.put(key, now);
      setDirty();
    }
  }

  // Did attackerId hit victimId first within the window?
  public boolean attackedFirst(UUID attackerId, UUID victimId, long now, long windowMillis) {
    String key = directedKey(attackerId, victimId);
    long timestamp = firstAttackTimestamps.getOrDefault(key, 0L);
    return timestamp > 0L && now - timestamp <= windowMillis;
  }

  // Call this after a kill so the next fight between these two starts fresh
  public void clearPair(UUID playerA, UUID playerB) {
    firstAttackTimestamps.remove(directedKey(playerA, playerB));
    firstAttackTimestamps.remove(directedKey(playerB, playerA));
    setDirty();
  }

  // Clear all records involving a player (clan leave, kick, etc)
  public void clearPlayer(UUID playerId) {
    String id = playerId.toString();
    firstAttackTimestamps.keySet().removeIf(key -> key.startsWith(id) || key.contains(">" + id));
    setDirty();
  }
}