package com.raiiiden.hierarchy.clan.combat;

import java.util.UUID;

public record CombatSnapshot(
    UUID firstPlayer,
    UUID secondPlayer,
    UUID firstClan,
    UUID secondClan,
    PvpRelationship relationship,
    long pvpAllowedAfterTimestamp,
    long lastDamageTimestamp
) {
  public CombatSnapshot touch(long timestamp) {
    return new CombatSnapshot(firstPlayer, secondPlayer, firstClan, secondClan, relationship, pvpAllowedAfterTimestamp, timestamp);
  }
}
