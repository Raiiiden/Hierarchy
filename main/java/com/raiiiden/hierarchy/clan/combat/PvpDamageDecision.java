package com.raiiiden.hierarchy.clan.combat;

public record PvpDamageDecision(boolean allowed, boolean validPvp, PvpRelationship relationship, long pvpAllowedAfterTimestamp) {
}
