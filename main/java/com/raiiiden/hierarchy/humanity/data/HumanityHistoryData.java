package com.raiiiden.hierarchy.humanity.data;

import com.raiiiden.hierarchy.humanity.config.HumanityConfig;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

// Per-player log of recent humanity changes (timestamp, reason, delta).
public class HumanityHistoryData extends SavedData {
  private static final String DATA_NAME = "hierarchy_humanity_history";

  public record Entry(long timestamp, String reason, double delta) {}

  private final Map<UUID, Deque<Entry>> history = new HashMap<>();

  public static HumanityHistoryData get(MinecraftServer server) {
    return server.getLevel(Level.OVERWORLD)
            .getDataStorage()
            .computeIfAbsent(HumanityHistoryData::load, HumanityHistoryData::new, DATA_NAME);
  }

  public static HumanityHistoryData load(CompoundTag tag) {
    HumanityHistoryData data = new HumanityHistoryData();
    ListTag players = tag.getList("History", 10);
    for (int i = 0; i < players.size(); i++) {
      CompoundTag playerTag = players.getCompound(i);
      Deque<Entry> entries = new ArrayDeque<>();
      ListTag entryTags = playerTag.getList("Entries", 10);
      for (int j = 0; j < entryTags.size(); j++) {
        CompoundTag entryTag = entryTags.getCompound(j);
        entries.addLast(new Entry(
                entryTag.getLong("Timestamp"),
                entryTag.getString("Reason"),
                entryTag.getDouble("Delta")));
      }
      data.history.put(playerTag.getUUID("Player"), entries);
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag) {
    ListTag players = new ListTag();
    for (Map.Entry<UUID, Deque<Entry>> entry : history.entrySet()) {
      CompoundTag playerTag = new CompoundTag();
      playerTag.putUUID("Player", entry.getKey());
      ListTag entryTags = new ListTag();
      for (Entry e : entry.getValue()) {
        CompoundTag entryTag = new CompoundTag();
        entryTag.putLong("Timestamp", e.timestamp());
        entryTag.putString("Reason", e.reason());
        entryTag.putDouble("Delta", e.delta());
        entryTags.add(entryTag);
      }
      playerTag.put("Entries", entryTags);
      players.add(playerTag);
    }
    tag.put("History", players);
    return tag;
  }

  public void record(UUID playerId, double delta, String reason, long timestamp) {
    Deque<Entry> entries = history.computeIfAbsent(playerId, id -> new ArrayDeque<>());
    entries.addLast(new Entry(timestamp, reason == null ? "" : reason, delta));
    int max = HumanityConfig.HISTORY_MAX_ENTRIES.get();
    while (entries.size() > max) {
      entries.removeFirst();
    }
    setDirty();
  }

  // Most recent entries first, up to {@code limit}.
  public List<Entry> recent(UUID playerId, int limit) {
    Deque<Entry> entries = history.get(playerId);
    if (entries == null || entries.isEmpty()) {
      return List.of();
    }
    List<Entry> all = new ArrayList<>(entries);
    List<Entry> result = new ArrayList<>();
    for (int i = all.size() - 1; i >= 0 && result.size() < limit; i--) {
      result.add(all.get(i));
    }
    return result;
  }

  public void clear() {
    history.clear();
    setDirty();
  }
}
