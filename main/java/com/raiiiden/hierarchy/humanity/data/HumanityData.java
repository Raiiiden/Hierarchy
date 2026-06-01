package com.raiiiden.hierarchy.humanity.data;

import com.raiiiden.hierarchy.humanity.config.HumanityConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public class HumanityData extends SavedData {
  private static final String DATA_NAME = "hierarchy_humanity";
  private final Map<UUID, Integer> humanity = new HashMap<>();

  public static HumanityData get(MinecraftServer server) {
    return server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(HumanityData::load, HumanityData::new, DATA_NAME);
  }

  public static HumanityData load(CompoundTag tag) {
    HumanityData data = new HumanityData();
    ListTag entries = tag.getList("Humanity", 10);
    for (int i = 0; i < entries.size(); i++) {
      CompoundTag entry = entries.getCompound(i);
      data.humanity.put(entry.getUUID("Player"), entry.getInt("Value"));
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag) {
    ListTag entries = new ListTag();
    for (Map.Entry<UUID, Integer> entry : humanity.entrySet()) {
      CompoundTag value = new CompoundTag();
      value.putUUID("Player", entry.getKey());
      value.putInt("Value", entry.getValue());
      entries.add(value);
    }
    tag.put("Humanity", entries);
    return tag;
  }

  public int humanity(UUID playerId) {
    return humanity.getOrDefault(playerId, 0);
  }

  public void add(UUID playerId, int amount) {
    int min = HumanityConfig.MIN_HUMANITY.get();
    int max = HumanityConfig.MAX_HUMANITY.get();
    humanity.put(playerId, Math.max(min, Math.min(max, humanity(playerId) + amount)));
    setDirty();
  }

  public void set(UUID playerId, int value) {
    int min = HumanityConfig.MIN_HUMANITY.get();
    int max = HumanityConfig.MAX_HUMANITY.get();
    humanity.put(playerId, Math.max(min, Math.min(max, value)));
    setDirty();
  }
}
