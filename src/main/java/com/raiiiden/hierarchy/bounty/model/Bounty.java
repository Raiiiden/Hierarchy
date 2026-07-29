package com.raiiiden.hierarchy.bounty.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class Bounty {
  public static final int MAX_REWARD_SLOTS = 9;

  private final UUID id;
  private final UUID targetId;
  private String targetName;
  private long expiresAt;
  private long pausedAt;
  private final List<RewardItem> rewards = new ArrayList<>();
  private final Map<UUID, Integer> contributions = new LinkedHashMap<>();

  public Bounty(UUID id, UUID targetId, String targetName, long expiresAt) {
    this.id = id;
    this.targetId = targetId;
    this.targetName = targetName;
    this.expiresAt = expiresAt;
  }

  public UUID id() {
    return id;
  }

  public UUID targetId() {
    return targetId;
  }

  public String targetName() {
    return targetName;
  }

  public void targetName(String targetName) {
    this.targetName = targetName;
  }

  public List<RewardItem> rewards() {
    return Collections.unmodifiableList(rewards);
  }

  public Map<UUID, Integer> contributions() {
    return contributions;
  }

  public long expiresAt() {
    return expiresAt;
  }

  public void expiresAt(long expiresAt) {
    this.expiresAt = expiresAt;
  }

  public long pausedAt() {
    return pausedAt;
  }

  public void pausedAt(long pausedAt) {
    this.pausedAt = pausedAt;
  }

  public boolean hasRewardSpace(int count) {
    return rewards.size() + count <= MAX_REWARD_SLOTS;
  }

  public void addReward(UUID contributorId, ItemStack stack) {
    if (stack.isEmpty() || rewards.size() >= MAX_REWARD_SLOTS) {
      return;
    }
    rewards.add(new RewardItem(contributorId, rewards.size(), stack.copy()));
    rebuildContributionCounts();
  }

  public List<ItemStack> removeContributorRewards(UUID contributorId) {
    List<ItemStack> removed = new ArrayList<>();
    rewards.removeIf(reward -> {
      if (!reward.contributorId().equals(contributorId)) {
        return false;
      }
      removed.add(reward.stack().copy());
      return true;
    });
    reindexRewards();
    rebuildContributionCounts();
    return removed;
  }

  public void clearRewards() {
    rewards.clear();
    contributions.clear();
  }

  public void loadReward(RewardItem reward) {
    if (reward.stack().isEmpty() || rewards.size() >= MAX_REWARD_SLOTS) {
      return;
    }
    rewards.add(new RewardItem(reward.contributorId(), rewards.size(), reward.stack()));
    rebuildContributionCounts();
  }

  public List<ItemStack> rewardStacks() {
    List<ItemStack> stacks = new ArrayList<>();
    for (RewardItem reward : rewards) {
      stacks.add(reward.stack().copy());
    }
    return stacks;
  }

  private void reindexRewards() {
    for (int i = 0; i < rewards.size(); i++) {
      RewardItem reward = rewards.get(i);
      rewards.set(i, new RewardItem(reward.contributorId(), i, reward.stack()));
    }
  }

  private void rebuildContributionCounts() {
    contributions.clear();
    for (RewardItem reward : rewards) {
      contributions.merge(reward.contributorId(), 1, Integer::sum);
    }
  }

  public record RewardItem(UUID contributorId, int slot, ItemStack stack) {
    public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      tag.putUUID("Contributor", contributorId);
      tag.putInt("Slot", slot);
      tag.put("Item", stack.save(new CompoundTag()));
      return tag;
    }

    public static RewardItem load(CompoundTag tag) {
      return new RewardItem(tag.getUUID("Contributor"), tag.getInt("Slot"), ItemStack.of(tag.getCompound("Item")));
    }
  }
}
