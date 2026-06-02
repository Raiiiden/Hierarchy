package com.raiiiden.hierarchy.bounty.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Bounty {
  private final UUID id;
  private final UUID targetId;
  private String targetName;
  private long amount;
  private long expiresAt;
  private long pausedAt;
  private final Map<UUID, Long> contributions = new LinkedHashMap<>();

  public Bounty(UUID id, UUID targetId, String targetName, long amount, long expiresAt) {
    this.id = id;
    this.targetId = targetId;
    this.targetName = targetName;
    this.amount = amount;
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

  public long amount() {
    return amount;
  }

  public void amount(long amount) {
    this.amount = amount;
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

  public Map<UUID, Long> contributions() {
    return contributions;
  }

  public void contribute(UUID playerId, long amount) {
    this.amount += amount;
    contributions.merge(playerId, amount, Long::sum);
  }
}
