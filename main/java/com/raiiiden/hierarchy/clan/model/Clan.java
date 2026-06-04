package com.raiiiden.hierarchy.clan.model;

import com.raiiiden.hierarchy.clan.bank.BankTransaction;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Clan {
  private final UUID id;
  private String name;
  private String tag = "";
  private UUID leader;
  private long bankBalance;
  private final Set<UUID> coLeaders = new LinkedHashSet<>();
  private final Set<UUID> lieutenants = new LinkedHashSet<>();
  private final Set<UUID> officers = new LinkedHashSet<>();
  private final Set<UUID> members = new LinkedHashSet<>();
  private final Set<UUID> allies = new LinkedHashSet<>();
  private final Set<UUID> enemies = new LinkedHashSet<>();
  private final LinkedList<BankTransaction> transactions = new LinkedList<>();
  private boolean internalFriendlyFire = false;
  public boolean isInternalFriendlyFireEnabled() { return internalFriendlyFire; }
  public void setInternalFriendlyFire(boolean enabled) { this.internalFriendlyFire = enabled; }

  public Clan(UUID id, String name, UUID leader) {
    this.id = id;
    this.name = name;
    this.leader = leader;
    this.members.add(leader);
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag == null ? "" : tag;
  }

  public UUID getLeader() {
    return leader;
  }

  public void setLeader(UUID leader) {
    this.leader = leader;
    members.add(leader);
    clearAssignedRole(leader);
  }

  public Set<UUID> getCoLeaders() {
    return coLeaders;
  }

  public Set<UUID> getLieutenants() {
    return lieutenants;
  }

  public Set<UUID> getOfficers() {
    return officers;
  }

  public Set<UUID> getMembers() {
    return members;
  }

  public Set<UUID> getAllies() {
    return allies;
  }

  public Set<UUID> getEnemies() {
    return enemies;
  }

  public long getBankBalance() {
    return bankBalance;
  }

  public void setBankBalance(long bankBalance) {
    this.bankBalance = Math.max(0L, bankBalance);
  }

  public List<BankTransaction> getTransactions() {
    return transactions;
  }

  public void addTransaction(BankTransaction transaction, int maxEntries) {
    transactions.add(transaction);
    while (transactions.size() > maxEntries) {
      transactions.removeFirst();
    }
  }

  public boolean contains(UUID playerId) {
    return members.contains(playerId);
  }

  public ClanRole roleOf(UUID playerId) {
    if (leader.equals(playerId)) {
      return ClanRole.LEADER;
    }
    if (coLeaders.contains(playerId)) {
      return ClanRole.CO_LEADER;
    }
    if (lieutenants.contains(playerId)) {
      return ClanRole.LIEUTENANT;
    }
    if (officers.contains(playerId)) {
      return ClanRole.OFFICER;
    }
    if (members.contains(playerId)) {
      return ClanRole.MEMBER;
    }
    return null;
  }

  public void setRole(UUID playerId, ClanRole role) {
    if (!members.contains(playerId) || role == null || role == ClanRole.LEADER) {
      return;
    }
    clearAssignedRole(playerId);
    switch (role) {
      case CO_LEADER -> coLeaders.add(playerId);
      case LIEUTENANT -> lieutenants.add(playerId);
      case OFFICER -> officers.add(playerId);
      case MEMBER -> {
      }
      default -> {
      }
    }
  }

  public void clearAssignedRole(UUID playerId) {
    coLeaders.remove(playerId);
    lieutenants.remove(playerId);
    officers.remove(playerId);
  }

  public boolean canManage(UUID playerId) {
    ClanRole role = roleOf(playerId);
    return role == ClanRole.LEADER || role == ClanRole.CO_LEADER || role == ClanRole.LIEUTENANT || role == ClanRole.OFFICER;
  }
}
