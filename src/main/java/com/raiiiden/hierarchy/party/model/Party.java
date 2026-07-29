package com.raiiiden.hierarchy.party.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Party {
    private final UUID id;
    private UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final long createdAt;

    public Party(UUID id, UUID leader, long createdAt) {
        this.id = id;
        this.leader = leader;
        this.createdAt = createdAt;
        this.members.add(leader);
    }

    public UUID getId() { return id; }

    public UUID getLeader() { return leader; }

    public void setLeader(UUID leader) { this.leader = leader; }

    public Set<UUID> getMembers() { return members; }

    public long getCreatedAt() { return createdAt; }

    public boolean contains(UUID playerId) { return members.contains(playerId); }

    public boolean isLeader(UUID playerId) { return leader.equals(playerId); }

    public boolean isFull(int maxSize) { return members.size() >= maxSize; }
}