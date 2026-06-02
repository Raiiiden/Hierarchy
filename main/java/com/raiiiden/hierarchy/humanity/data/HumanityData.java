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
  private final Map<UUID, Double> humanity = new HashMap<>();

  public static HumanityData get(MinecraftServer server) {
    return server.getLevel(Level.OVERWORLD)
            .getDataStorage()
            .computeIfAbsent(HumanityData::load, HumanityData::new, DATA_NAME);
  }

  public static HumanityData load(CompoundTag tag) {
    HumanityData data = new HumanityData();
    ListTag entries = tag.getList("Humanity", 10);
    for (int i = 0; i < entries.size(); i++) {
      CompoundTag entry = entries.getCompound(i);
      // getDouble falls back gracefully if old data was stored as int
      data.humanity.put(entry.getUUID("Player"), entry.getDouble("Value"));
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag) {
    ListTag entries = new ListTag();
    for (Map.Entry<UUID, Double> entry : humanity.entrySet()) {
      CompoundTag value = new CompoundTag();
      value.putUUID("Player", entry.getKey());
      value.putDouble("Value", entry.getValue());
      entries.add(value);
    }
    tag.put("Humanity", entries);
    return tag;
  }

  public double humanity(UUID playerId) {
    return humanity.getOrDefault(playerId, 0.0);
  }

  public int humanityDisplay(UUID playerId) {
    return (int) Math.round(humanity(playerId));
  }

  public void add(UUID playerId, double amount) {
    double min = HumanityConfig.MIN_HUMANITY.get();
    double max = HumanityConfig.MAX_HUMANITY.get();
    humanity.put(playerId, Math.max(min, Math.min(max, humanity(playerId) + amount)));
    setDirty();
  }

  public void set(UUID playerId, double value) {
    double min = HumanityConfig.MIN_HUMANITY.get();
    double max = HumanityConfig.MAX_HUMANITY.get();
    humanity.put(playerId, Math.max(min, Math.min(max, value)));
    setDirty();
  }

  public void clear() {
    humanity.clear();
    setDirty();
  }
}