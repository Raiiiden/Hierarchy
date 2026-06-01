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
  private final Map<String, UUID> firstAttackers = new HashMap<>();
  private final Map<String, Long> firstAttackTimestamps = new HashMap<>();

  public static HumanityCombatData get(MinecraftServer server) {
    return server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(HumanityCombatData::load, HumanityCombatData::new, DATA_NAME);
  }

  public static HumanityCombatData load(CompoundTag tag) {
    HumanityCombatData data = new HumanityCombatData();
    ListTag entries = tag.getList("FirstAttacks", 10);
    for (int i = 0; i < entries.size(); i++) {
      CompoundTag entry = entries.getCompound(i);
      data.firstAttackers.put(entry.getString("Pair"), entry.getUUID("Attacker"));
      data.firstAttackTimestamps.put(entry.getString("Pair"), entry.getLong("Timestamp"));
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag) {
    ListTag entries = new ListTag();
    for (Map.Entry<String, UUID> entry : firstAttackers.entrySet()) {
      CompoundTag value = new CompoundTag();
      value.putString("Pair", entry.getKey());
      value.putUUID("Attacker", entry.getValue());
      value.putLong("Timestamp", firstAttackTimestamps.getOrDefault(entry.getKey(), 0L));
      entries.add(value);
    }
    tag.put("FirstAttacks", entries);
    return tag;
  }

  public void recordFirstAttack(String pair, UUID attacker, long now, long windowMillis) {
    long previous = firstAttackTimestamps.getOrDefault(pair, 0L);
    if (previous <= 0L || now - previous > windowMillis) {
      firstAttackers.put(pair, attacker);
      firstAttackTimestamps.put(pair, now);
      setDirty();
    }
  }

  public boolean attackedFirst(String pair, UUID attacker, long now, long windowMillis) {
    long timestamp = firstAttackTimestamps.getOrDefault(pair, 0L);
    return timestamp > 0L && now - timestamp <= windowMillis && attacker.equals(firstAttackers.get(pair));
  }
}
