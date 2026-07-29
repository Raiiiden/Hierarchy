package com.raiiiden.hierarchy.humanity.data;

import com.raiiiden.hierarchy.clan.data.ClanData;
import com.raiiiden.hierarchy.humanity.HumanityRank;
import com.raiiiden.hierarchy.humanity.config.HumanityConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public class HumanityData extends SavedData {
  private static final String DATA_NAME = "hierarchy_humanity";
  private final Map<UUID, Double> humanity = new HashMap<>();
  private MinecraftServer server;

  public static HumanityData get(MinecraftServer server) {
    HumanityData data = server.getLevel(Level.OVERWORLD)
            .getDataStorage()
            .computeIfAbsent(HumanityData::load, HumanityData::new, DATA_NAME);
    data.server = server;
    return data;
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
    add(playerId, amount, "");
  }

  public void add(UUID playerId, double amount, String reason) {
    double oldValue = humanity(playerId);
    double min = HumanityConfig.MIN_HUMANITY.get();
    double max = HumanityConfig.MAX_HUMANITY.get();
    double newValue = Math.max(min, Math.min(max, oldValue + amount));
    humanity.put(playerId, newValue);
    setDirty();
    onChanged(playerId, oldValue, newValue, reason);
  }

  public void set(UUID playerId, double value) {
    set(playerId, value, "");
  }

  public void set(UUID playerId, double value, String reason) {
    double oldValue = humanity(playerId);
    double min = HumanityConfig.MIN_HUMANITY.get();
    double max = HumanityConfig.MAX_HUMANITY.get();
    double newValue = Math.max(min, Math.min(max, value));
    humanity.put(playerId, newValue);
    setDirty();
    onChanged(playerId, oldValue, newValue, reason);
  }

  // Records history, updates clan humanity totals, and notifies on rank changes.
  private void onChanged(UUID playerId, double oldValue, double newValue, String reason) {
    if (server == null) {
      return;
    }
    double delta = newValue - oldValue;
    if (Math.abs(delta) > 0.0001D) {
      HumanityHistoryData.get(server).record(playerId, delta, reason, System.currentTimeMillis());
      ClanData clans = ClanData.get(server);
      clans.clanOf(playerId).ifPresent(clan -> {
        if (delta > 0.0) {
          clan.addHumanityGained(delta);
        } else {
          clan.addHumanityLost(-delta);
        }
        clans.setDirty();
      });
    }
    if (HumanityConfig.ENABLE_RANK_NOTIFICATIONS.get()) {
      String oldRank = HumanityRank.displayName(oldValue);
      String newRank = HumanityRank.displayName(newValue);
      if (!oldRank.equals(newRank)) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
          String text = HumanityConfig.RANK_CHANGE_MESSAGE.get().replace("{rank}", newRank);
          player.displayClientMessage(
                  Component.literal(text).withStyle(HumanityRank.color(newValue)), true);
        }
      }
    }
  }

  public void clear() {
    humanity.clear();
    setDirty();
  }
}