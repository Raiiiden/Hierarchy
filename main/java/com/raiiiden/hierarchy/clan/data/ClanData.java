package com.raiiiden.hierarchy.clan.data;

import com.raiiiden.hierarchy.clan.bank.BankTransaction;
import com.raiiiden.hierarchy.clan.bank.BankTransactionType;
import com.raiiiden.hierarchy.clan.combat.CombatSnapshot;
import com.raiiiden.hierarchy.clan.combat.PvpDamageDecision;
import com.raiiiden.hierarchy.clan.combat.PvpRelationship;
import com.raiiiden.hierarchy.clan.config.ClanCombatConfig;
import com.raiiiden.hierarchy.clan.model.Clan;
import com.raiiiden.hierarchy.clan.model.ClanRole;
import com.raiiiden.hierarchy.clan.model.ClanPair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public class ClanData extends SavedData {
  private static final String DATA_NAME = "hierarchy_clans";
  private static final long COMBAT_SNAPSHOT_TTL_MILLIS = 15L * 1000L;
  private MinecraftServer server;

  private final Map<UUID, Clan> clans = new LinkedHashMap<>();
  private final Map<UUID, UUID> playerClans = new HashMap<>();
  private final Map<String, String> clanNameIndex = new HashMap<>();
  // player -> (clan -> invitedAt millis). A player may hold invites from several clans.
  private final Map<UUID, Map<UUID, Long>> pendingInvites = new HashMap<>();
  private final Map<String, Boolean> friendlyFire = new HashMap<>();
  private final Map<String, Long> pvpDelayUntil = new HashMap<>();
  private final Map<UUID, List<String>> offlineNotifications = new HashMap<>();
  private final Map<String, Long> playerClanPvpDelayUntil = new HashMap<>();
  private final Map<UUID, String> playerNames = new HashMap<>();
  private final Map<String, UUID> playerNameIndex = new HashMap<>();
  private final Map<String, CombatSnapshot> combatSnapshots = new HashMap<>();
  private final Map<String, Boolean> lastPvpDamageValidity = new HashMap<>();
  private final Set<String> pendingAllianceRequests = new java.util.LinkedHashSet<>();
  private final Set<String> pendingFriendlyFireRequests = new java.util.LinkedHashSet<>();

  public static ClanData get(MinecraftServer server) {
    ClanData data = server.getLevel(Level.OVERWORLD)
            .getDataStorage()
            .computeIfAbsent(ClanData::load, ClanData::new, DATA_NAME);
    data.server = server;
    return data;
  }
  private PlayerTeam getOrCreateTeam(Clan clan) {
    if (server == null) return null;
    Scoreboard scoreboard = server.getScoreboard();
    String teamName = "clan_" + clan.getId().toString().replace("-", "").substring(0, 16);
    PlayerTeam team = scoreboard.getPlayerTeam(teamName);
    if (team == null) {
      team = scoreboard.addPlayerTeam(teamName);
      team.setDisplayName(Component.literal(clan.getName()));
    }
    return team;
  }

  public static ClanData load(CompoundTag tag) {
    ClanData data = new ClanData();
    ListTag clanTags = tag.getList("Clans", 10);
    for (int i = 0; i < clanTags.size(); i++) {
      CompoundTag clanTag = clanTags.getCompound(i);
      Clan clan = new Clan(clanTag.getUUID("Id"), clanTag.getString("Name"), clanTag.getUUID("Leader"));
      clan.setTag(clanTag.getString("Tag"));
      clan.setDescription(clanTag.getString("Description"));
      readUuidSet(clanTag.getList("CoLeaders", 8), clan.getCoLeaders());
      readUuidSet(clanTag.getList("Lieutenants", 8), clan.getLieutenants());
      readUuidSet(clanTag.getList("Officers", 8), clan.getOfficers());
      clan.getMembers().clear();
      readUuidSet(clanTag.getList("Members", 8), clan.getMembers());
      readUuidSet(clanTag.getList("Allies", 8), clan.getAllies());
      readUuidSet(clanTag.getList("Enemies", 8), clan.getEnemies());
      clan.setBankBalance(clanTag.getLong("BankBalance"));
      clan.setCreatedAt(clanTag.contains("CreatedAt") ? clanTag.getLong("CreatedAt") : System.currentTimeMillis());
      clan.setKills(clanTag.getInt("Kills"));
      clan.setDeaths(clanTag.getInt("Deaths"));
      clan.setBountiesClaimed(clanTag.getInt("BountiesClaimed"));
      clan.setBountiesPlaced(clanTag.getInt("BountiesPlaced"));
      clan.setTotalMembersJoined(clanTag.contains("TotalMembersJoined") ? clanTag.getInt("TotalMembersJoined") : clan.getMembers().size());
      clan.setTotalHumanityGained(clanTag.getDouble("TotalHumanityGained"));
      clan.setTotalHumanityLost(clanTag.getDouble("TotalHumanityLost"));
      ListTag transactionTags = clanTag.getList("Transactions", 10);
      clan.setInternalFriendlyFire(clanTag.getBoolean("InternalFF"));
      for (int j = 0; j < transactionTags.size(); j++) {
        CompoundTag transactionTag = transactionTags.getCompound(j);
        clan.getTransactions().add(new BankTransaction(
            BankTransactionType.valueOf(transactionTag.getString("Type")),
            transactionTag.getUUID("Player"),
            transactionTag.getString("PlayerName"),
            transactionTag.getLong("Amount"),
            transactionTag.getLong("Timestamp"),
            transactionTag.getString("Reason")));
      }
      data.clans.put(clan.getId(), clan);
      data.clanNameIndex.put(normalize(clan.getName()), clan.getId().toString());
      for (UUID member : clan.getMembers()) {
        data.playerClans.put(member, clan.getId());
      }
    }
    ListTag inviteTags = tag.getList("PendingInvites", 10);
    for (int i = 0; i < inviteTags.size(); i++) {
      CompoundTag playerTag = inviteTags.getCompound(i);
      UUID playerId = playerTag.getUUID("Player");
      Map<UUID, Long> invites = new HashMap<>();
      ListTag clanList = playerTag.getList("Invites", 10);
      for (int j = 0; j < clanList.size(); j++) {
        CompoundTag inviteTag = clanList.getCompound(j);
        invites.put(inviteTag.getUUID("Clan"), inviteTag.getLong("Timestamp"));
      }
      if (!invites.isEmpty()) {
        data.pendingInvites.put(playerId, invites);
      }
    }
    readStringBooleanMap(tag.getList("FriendlyFire", 10), data.friendlyFire);
    readStringLongMap(tag.getList("PvpDelays", 10), data.pvpDelayUntil);
    readStringLongMap(tag.getList("PlayerClanPvpDelays", 10), data.playerClanPvpDelayUntil);
    readStringSet(tag.getList("AllianceRequests", 8), data.pendingAllianceRequests);
    readStringSet(tag.getList("FriendlyFireRequests", 8), data.pendingFriendlyFireRequests);
    ListTag notificationTags = tag.getList("OfflineNotifications", 10);
    for (int i = 0; i < notificationTags.size(); i++) {
      CompoundTag entry = notificationTags.getCompound(i);
      UUID playerId = entry.getUUID("Player");
      List<String> messages = new ArrayList<>();
      ListTag messagesTag = entry.getList("Messages", 8);
      for (int j = 0; j < messagesTag.size(); j++) {
        messages.add(messagesTag.getString(j));
      }
      data.offlineNotifications.put(playerId, messages);
    }
    ListTag nameTags = tag.getList("PlayerNames", 10);
    for (int i = 0; i < nameTags.size(); i++) {
      CompoundTag entry = nameTags.getCompound(i);
      data.rememberPlayer(entry.getUUID("Player"), entry.getString("Name"));
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag) {
    ListTag clanTags = new ListTag();
    for (Clan clan : clans.values()) {
      CompoundTag clanTag = new CompoundTag();
      clanTag.putUUID("Id", clan.getId());
      clanTag.putString("Name", clan.getName());
      clanTag.putString("Tag", clan.getTag());
      clanTag.putString("Description", clan.getDescription());
      clanTag.putUUID("Leader", clan.getLeader());
      clanTag.put("CoLeaders", writeUuidSet(clan.getCoLeaders()));
      clanTag.put("Lieutenants", writeUuidSet(clan.getLieutenants()));
      clanTag.put("Officers", writeUuidSet(clan.getOfficers()));
      clanTag.put("Members", writeUuidSet(clan.getMembers()));
      clanTag.put("Allies", writeUuidSet(clan.getAllies()));
      clanTag.put("Enemies", writeUuidSet(clan.getEnemies()));
      clanTag.putLong("BankBalance", clan.getBankBalance());
      clanTag.putLong("CreatedAt", clan.getCreatedAt());
      clanTag.putInt("Kills", clan.getKills());
      clanTag.putInt("Deaths", clan.getDeaths());
      clanTag.putInt("BountiesClaimed", clan.getBountiesClaimed());
      clanTag.putInt("BountiesPlaced", clan.getBountiesPlaced());
      clanTag.putInt("TotalMembersJoined", clan.getTotalMembersJoined());
      clanTag.putDouble("TotalHumanityGained", clan.getTotalHumanityGained());
      clanTag.putDouble("TotalHumanityLost", clan.getTotalHumanityLost());
      clanTag.putBoolean("InternalFF", clan.isInternalFriendlyFireEnabled());
      ListTag transactionTags = new ListTag();
      for (BankTransaction transaction : clan.getTransactions()) {
        CompoundTag transactionTag = new CompoundTag();
        transactionTag.putString("Type", transaction.type().name());
        transactionTag.putUUID("Player", transaction.playerId());
        transactionTag.putString("PlayerName", transaction.playerName());
        transactionTag.putLong("Amount", transaction.amount());
        transactionTag.putLong("Timestamp", transaction.timestamp());
        transactionTag.putString("Reason", transaction.reason() == null ? "" : transaction.reason());
        transactionTags.add(transactionTag);
      }
      clanTag.put("Transactions", transactionTags);
      clanTags.add(clanTag);
    }
    tag.put("Clans", clanTags);
    ListTag inviteTags = new ListTag();
    for (Map.Entry<UUID, Map<UUID, Long>> entry : pendingInvites.entrySet()) {
      CompoundTag playerTag = new CompoundTag();
      playerTag.putUUID("Player", entry.getKey());
      ListTag clanList = new ListTag();
      for (Map.Entry<UUID, Long> invite : entry.getValue().entrySet()) {
        CompoundTag inviteTag = new CompoundTag();
        inviteTag.putUUID("Clan", invite.getKey());
        inviteTag.putLong("Timestamp", invite.getValue());
        clanList.add(inviteTag);
      }
      playerTag.put("Invites", clanList);
      inviteTags.add(playerTag);
    }
    tag.put("PendingInvites", inviteTags);
    tag.put("FriendlyFire", writeStringBooleanMap(friendlyFire));
    tag.put("PvpDelays", writeStringLongMap(pvpDelayUntil));
    tag.put("PlayerClanPvpDelays", writeStringLongMap(playerClanPvpDelayUntil));
    tag.put("AllianceRequests", writeStringSet(pendingAllianceRequests));
    tag.put("FriendlyFireRequests", writeStringSet(pendingFriendlyFireRequests));
    ListTag notificationTags = new ListTag();
    for (Map.Entry<UUID, List<String>> entry : offlineNotifications.entrySet()) {
      CompoundTag notificationTag = new CompoundTag();
      notificationTag.putUUID("Player", entry.getKey());
      ListTag messagesTag = new ListTag();
      for (String message : entry.getValue()) {
        messagesTag.add(StringTag.valueOf(message));
      }
      notificationTag.put("Messages", messagesTag);
      notificationTags.add(notificationTag);
    }
    tag.put("OfflineNotifications", notificationTags);
    ListTag nameTags = new ListTag();
    for (Map.Entry<UUID, String> entry : playerNames.entrySet()) {
      CompoundTag nameTag = new CompoundTag();
      nameTag.putUUID("Player", entry.getKey());
      nameTag.putString("Name", entry.getValue());
      nameTags.add(nameTag);
    }
    tag.put("PlayerNames", nameTags);
    return tag;
  }

  public Collection<Clan> clans() {
    return Collections.unmodifiableCollection(clans.values());
  }

  public Optional<Clan> clan(UUID clanId) {
    return Optional.ofNullable(clans.get(clanId));
  }

  public Optional<Clan> clanByName(String name) {
    String id = clanNameIndex.get(normalize(name));
    return id == null ? Optional.empty() : Optional.ofNullable(clans.get(UUID.fromString(id)));
  }

  public Optional<Clan> clanOf(UUID playerId) {
    UUID clanId = playerClans.get(playerId);
    return clanId == null ? Optional.empty() : Optional.ofNullable(clans.get(clanId));
  }

  public Clan createClan(String name, UUID leader) {
    Clan clan = new Clan(UUID.randomUUID(), name, leader);
    clans.put(clan.getId(), clan);
    clanNameIndex.put(normalize(name), clan.getId().toString());
    playerClans.put(leader, clan.getId());
    if (ClanCombatConfig.USE_VANILLA_TEAMS.get()) {
      PlayerTeam team = getOrCreateTeam(clan);
      if (team != null && server != null) {
        server.getScoreboard().addPlayerToTeam(playerName(leader), team);
      }
    }
    setDirty();
    syncClanMembersToAllOnlinePlayers();
    return clan;
  }

  public void deleteClan(Clan clan) {
    java.util.List<UUID> formerMembers = new java.util.ArrayList<>(clan.getMembers());

    for (UUID allyId : new ArrayList<>(clan.getAllies())) {
      Clan ally = clans.get(allyId);
      if (ally != null) ally.getAllies().remove(clan.getId());
      String pair = ClanPair.key(clan.getId(), allyId);
      friendlyFire.remove(pair);
      pvpDelayUntil.remove(pair);
      pendingFriendlyFireRequests.remove(pair);
    }
    String id = clan.getId().toString();
    pendingAllianceRequests.removeIf(key -> key.contains(id));
    pendingFriendlyFireRequests.removeIf(key -> key.contains(id));
    for (UUID member : clan.getMembers()) {
      playerClans.remove(member);
    }
    clans.remove(clan.getId());
    clanNameIndex.remove(normalize(clan.getName()));
    pendingInvites.values().forEach(invites -> invites.remove(clan.getId()));
    pendingInvites.values().removeIf(Map::isEmpty);
    if (ClanCombatConfig.USE_VANILLA_TEAMS.get() && server != null) {
      String teamName = "clan_" + clan.getId().toString().replace("-", "").substring(0, 16);
      PlayerTeam team = server.getScoreboard().getPlayerTeam(teamName);
      if (team != null) server.getScoreboard().removePlayerTeam(team);
    }
    setDirty();

    // clanOf(member) is now empty for all former members → sends empty set to each
    for (UUID member : formerMembers) {
      syncClanMembersToPlayer(member);
    }
  }

  public void addMember(Clan clan, UUID playerId) {
    boolean isNewMember = clan.getMembers().add(playerId);
    if (isNewMember) {
      clan.addMemberJoined();
    }
    playerClans.put(playerId, clan.getId());
    clearInvites(playerId);  // joining a clan drops any other pending invites
    clearCombatSnapshots(playerId);
    if (ClanCombatConfig.USE_VANILLA_TEAMS.get()) {
      PlayerTeam team = getOrCreateTeam(clan);
      if (team != null && server != null) {
        server.getScoreboard().addPlayerToTeam(playerName(playerId), team);
      }
    }
    setDirty();
    syncClanMembersToAllOnlinePlayers();
  }

  public void removeMember(Clan clan, UUID playerId) {
    clan.getMembers().remove(playerId);
    clan.clearAssignedRole(playerId);
    playerClans.remove(playerId);
    clearCombatSnapshots(playerId);
    if (ClanCombatConfig.USE_VANILLA_TEAMS.get() && server != null) {
      server.getScoreboard().removePlayerFromTeam(playerName(playerId));
    }
    setDirty();
    syncClanMembersToAllOnlinePlayers();
  }

  public void removeMemberWithPvpDelay(Clan clan, UUID playerId, long delayUntil) {
    removeMember(clan, playerId);
    setPlayerClanPvpDelay(playerId, clan.getId(), delayUntil);
  }

  /** A pending clan invite paired with the absolute time (millis) it expires. */
  public record PendingClanInvite(Clan clan, long expiresAtMillis) {}

  private static long inviteTtlMillis() {
    return ClanCombatConfig.INVITE_EXPIRY_HOURS.get() * 3_600_000L;
  }

  public void invite(UUID playerId, Clan clan) {
    pendingInvites.computeIfAbsent(playerId, ignored -> new HashMap<>())
            .put(clan.getId(), System.currentTimeMillis());
    setDirty();
  }

  /**
   * Drops any of the player's invites that have expired (older than the configured
   * expiry) or whose clan no longer exists. Returns the surviving invites, never null.
   */
  private Map<UUID, Long> liveInvites(UUID playerId) {
    Map<UUID, Long> invites = pendingInvites.get(playerId);
    if (invites == null) {
      return Collections.emptyMap();
    }
    long now = System.currentTimeMillis();
    long ttl = inviteTtlMillis();
    List<UUID> expiredClans = new ArrayList<>();
    boolean changed = invites.entrySet().removeIf(entry -> {
      boolean expired = now - entry.getValue() > ttl;
      if (expired) {
        expiredClans.add(entry.getKey());
      }
      return expired || !clans.containsKey(entry.getKey());
    });
    for (UUID clanId : expiredClans) {
      Clan clan = clans.get(clanId);
      notifyInviteExpired(playerId, clan != null ? clan.getName() : "a clan");
    }
    if (invites.isEmpty()) {
      pendingInvites.remove(playerId);
    }
    if (changed) {
      setDirty();
    }
    return invites;
  }

  // Tells the invited player their invite lapsed — directly if online, queued if not.
  private void notifyInviteExpired(UUID playerId, String clanName) {
    String message = "Your invitation to join " + clanName + " has expired.";
    ServerPlayer online = server == null ? null : server.getPlayerList().getPlayer(playerId);
    if (online != null) {
      online.sendSystemMessage(Component.literal(message));
    } else {
      queueNotification(playerId, message);
    }
  }

  /** All (non-expired) invites the player currently holds, with their expiry times. */
  public List<PendingClanInvite> pendingInvites(UUID playerId) {
    long ttl = inviteTtlMillis();
    List<PendingClanInvite> result = new ArrayList<>();
    for (Map.Entry<UUID, Long> entry : liveInvites(playerId).entrySet()) {
      Clan clan = clans.get(entry.getKey());
      if (clan != null) {
        result.add(new PendingClanInvite(clan, entry.getValue() + ttl));
      }
    }
    return result;
  }

  /** Sweeps every player's invite list, removing expired/orphaned entries. */
  public void purgeExpiredInvites() {
    for (UUID playerId : new ArrayList<>(pendingInvites.keySet())) {
      liveInvites(playerId);
    }
  }

  public boolean hasInvite(UUID playerId, UUID clanId) {
    return liveInvites(playerId).containsKey(clanId);
  }

  /** Removes a single invite (e.g. on deny). */
  public void clearInvite(UUID playerId, UUID clanId) {
    Map<UUID, Long> invites = pendingInvites.get(playerId);
    if (invites != null && invites.remove(clanId) != null) {
      if (invites.isEmpty()) {
        pendingInvites.remove(playerId);
      }
      setDirty();
    }
  }

  /** Removes every invite for the player (e.g. once they join a clan). */
  public void clearInvites(UUID playerId) {
    if (pendingInvites.remove(playerId) != null) {
      setDirty();
    }
  }

  public void addAlliance(Clan first, Clan second) {
    first.getAllies().add(second.getId());
    second.getAllies().add(first.getId());
    first.getEnemies().remove(second.getId());
    second.getEnemies().remove(first.getId());
    clearAllianceRequest(first.getId(), second.getId());
    clearAllianceRequest(second.getId(), first.getId());
    friendlyFire.put(ClanPair.key(first.getId(), second.getId()), false);
    clearCombatSnapshots(first, second);
    setDirty();
  }

  public void setEnemy(Clan first, Clan second) {
    first.getEnemies().add(second.getId());
    first.getAllies().remove(second.getId());
    second.getAllies().remove(first.getId());
    clearAllianceRequest(first.getId(), second.getId());
    clearAllianceRequest(second.getId(), first.getId());
    friendlyFire.remove(ClanPair.key(first.getId(), second.getId()));
    clearCombatSnapshots(first, second);
    setDirty();
  }

  public void clearEnemy(Clan first, Clan second) {
    first.getEnemies().remove(second.getId());
    setDirty();
  }

  public void removeAlliance(Clan first, Clan second, long delayUntil) {
    first.getAllies().remove(second.getId());
    second.getAllies().remove(first.getId());
    String pair = ClanPair.key(first.getId(), second.getId());
    friendlyFire.remove(pair);
    pendingFriendlyFireRequests.remove(pair);
    clearAllianceRequest(first.getId(), second.getId());
    clearAllianceRequest(second.getId(), first.getId());
    pvpDelayUntil.put(pair, delayUntil);
    clearCombatSnapshots(first, second);
    setDirty();
  }

  public boolean areAllied(UUID firstClanId, UUID secondClanId) {
    Clan first = clans.get(firstClanId);
    return first != null && first.getAllies().contains(secondClanId);
  }

  public void requestAlliance(UUID requesterClanId, UUID targetClanId) {
    pendingAllianceRequests.add(directedClanKey(requesterClanId, targetClanId));
    setDirty();
  }

  public boolean hasAllianceRequest(UUID requesterClanId, UUID targetClanId) {
    return pendingAllianceRequests.contains(directedClanKey(requesterClanId, targetClanId));
  }

  public void clearAllianceRequest(UUID requesterClanId, UUID targetClanId) {
    if (pendingAllianceRequests.remove(directedClanKey(requesterClanId, targetClanId))) {
      setDirty();
    }
  }

  public boolean friendlyFireEnabled(UUID firstClanId, UUID secondClanId) {
    return ClanCombatConfig.ENABLE_FRIENDLY_FIRE_SYSTEM.get() && friendlyFire.getOrDefault(ClanPair.key(firstClanId, secondClanId), false);
  }

  public void requestFriendlyFire(UUID firstClanId, UUID secondClanId) {
    pendingFriendlyFireRequests.add(ClanPair.key(firstClanId, secondClanId));
    setDirty();
  }

  public boolean hasFriendlyFireRequest(UUID firstClanId, UUID secondClanId) {
    return pendingFriendlyFireRequests.contains(ClanPair.key(firstClanId, secondClanId));
  }

  public void setFriendlyFire(UUID firstClanId, UUID secondClanId, boolean enabled) {
    String pair = ClanPair.key(firstClanId, secondClanId);
    pendingFriendlyFireRequests.remove(pair);
    friendlyFire.put(pair, enabled);
    clearCombatSnapshots(firstClanId, secondClanId);
    setDirty();
  }

  public void clearFriendlyFireRequest(UUID firstClanId, UUID secondClanId) {
    pendingFriendlyFireRequests.remove(ClanPair.key(firstClanId, secondClanId));
    setDirty();
  }

  public void revokeFriendlyFire(UUID firstClanId, UUID secondClanId, long delayUntil) {
    String pair = ClanPair.key(firstClanId, secondClanId);
    friendlyFire.put(pair, false);
    pendingFriendlyFireRequests.remove(pair);
    pvpDelayUntil.put(pair, delayUntil);
    clearCombatSnapshots(firstClanId, secondClanId);
    setDirty();
  }

  public long pvpDelayUntil(UUID firstClanId, UUID secondClanId) {
    return pvpDelayUntil.getOrDefault(ClanPair.key(firstClanId, secondClanId), 0L);
  }

  public long playerClanPvpDelayUntil(UUID playerId, UUID clanId) {
    return playerClanPvpDelayUntil.getOrDefault(playerClanDelayKey(playerId, clanId), 0L);
  }

  public void setPlayerClanPvpDelay(UUID playerId, UUID clanId, long delayUntil) {
    playerClanPvpDelayUntil.put(playerClanDelayKey(playerId, clanId), delayUntil);
    clearCombatSnapshots(playerId);
    setDirty();
  }

  public void clearCombatSnapshots(UUID playerId) {
    String id = playerId.toString();
    combatSnapshots.keySet().removeIf(key -> key.contains(id));
    lastPvpDamageValidity.keySet().removeIf(key -> key.contains(id));
  }

  private void clearCombatSnapshots(Clan first, Clan second) {
    for (UUID playerId : first.getMembers()) {
      clearCombatSnapshots(playerId);
    }
    for (UUID playerId : second.getMembers()) {
      clearCombatSnapshots(playerId);
    }
  }

  private void clearCombatSnapshots(UUID firstClanId, UUID secondClanId) {
    Clan first = clans.get(firstClanId);
    Clan second = clans.get(secondClanId);
    if (first != null && second != null) {
      clearCombatSnapshots(first, second);
    }
  }

  public PvpDamageDecision evaluateDamage(UUID attackerId, UUID victimId, long timestamp) {
    String combatKey = ClanPair.key(attackerId, victimId);
    CombatSnapshot snapshot = combatSnapshots.get(combatKey);
    if (!ClanCombatConfig.ENABLE_RELATIONSHIP_SNAPSHOTS.get() || snapshot == null || timestamp - snapshot.lastDamageTimestamp() > COMBAT_SNAPSHOT_TTL_MILLIS) {
      snapshot = createCombatSnapshot(attackerId, victimId, timestamp);
    } else {
      snapshot = snapshot.touch(timestamp);
    }
    combatSnapshots.put(combatKey, snapshot);
    boolean allowed = snapshot.relationship() != PvpRelationship.SAME_CLAN && snapshot.relationship() != PvpRelationship.ALLIED_FRIENDLY_FIRE_OFF;
    boolean validPvp = allowed && snapshot.relationship() != PvpRelationship.DELAYED;
    lastPvpDamageValidity.put(combatKey, validPvp);
    return new PvpDamageDecision(allowed, validPvp, snapshot.relationship(), snapshot.pvpAllowedAfterTimestamp());
  }

  public boolean isLastPvpDamageValid(UUID firstPlayer, UUID secondPlayer) {
    return lastPvpDamageValidity.getOrDefault(ClanPair.key(firstPlayer, secondPlayer), false);
  }

  private CombatSnapshot createCombatSnapshot(UUID attackerId, UUID victimId, long timestamp) {
    Clan attackerClan = clanOf(attackerId).orElse(null);
    Clan victimClan = clanOf(victimId).orElse(null);
    UUID attackerClanId = attackerClan == null ? null : attackerClan.getId();
    UUID victimClanId = victimClan == null ? null : victimClan.getId();
    PvpRelationship relationship = PvpRelationship.HOSTILE;
    long pvpAllowedAfterTimestamp = 0L;
    if (attackerClanId != null && victimClanId != null) {
      if (attackerClanId.equals(victimClanId)) {
        relationship = PvpRelationship.SAME_CLAN;
      } else if (areAllied(attackerClanId, victimClanId)) {
        relationship = friendlyFireEnabled(attackerClanId, victimClanId) ? PvpRelationship.ALLIED_FRIENDLY_FIRE_ON : PvpRelationship.ALLIED_FRIENDLY_FIRE_OFF;
      } else {
        pvpAllowedAfterTimestamp = pvpDelayUntil(attackerClanId, victimClanId);
        if (pvpAllowedAfterTimestamp > timestamp) {
          relationship = PvpRelationship.DELAYED;
        }
      }
    }
    if (relationship == PvpRelationship.HOSTILE && victimClanId != null) {
      pvpAllowedAfterTimestamp = Math.max(pvpAllowedAfterTimestamp, playerClanPvpDelayUntil(attackerId, victimClanId));
    }
    if (relationship == PvpRelationship.HOSTILE && attackerClanId != null) {
      pvpAllowedAfterTimestamp = Math.max(pvpAllowedAfterTimestamp, playerClanPvpDelayUntil(victimId, attackerClanId));
    }
    if (relationship == PvpRelationship.HOSTILE && pvpAllowedAfterTimestamp > timestamp) {
      relationship = PvpRelationship.DELAYED;
    }
    return new CombatSnapshot(attackerId, victimId, attackerClanId, victimClanId, relationship, pvpAllowedAfterTimestamp, timestamp);
  }

  public void rememberPlayer(UUID playerId, String name) {
    String previous = playerNames.put(playerId, name);
    if (previous != null) {
      playerNameIndex.remove(normalize(previous));
    }
    playerNameIndex.put(normalize(name), playerId);
    setDirty();
  }

  public Optional<UUID> playerByName(String name) {
    return Optional.ofNullable(playerNameIndex.get(normalize(name)));
  }

  /** Every player name the server has remembered (snapshot copy, safe to stream). */
  public Collection<String> knownPlayerNames() {
    return List.copyOf(playerNames.values());
  }

  public String playerName(UUID playerId) {
    return playerNames.getOrDefault(playerId, playerId.toString());
  }

  public void queueNotification(UUID playerId, String message) {
    if (!ClanCombatConfig.ENABLE_NOTIFICATIONS.get()) {
      return;
    }
    offlineNotifications.computeIfAbsent(playerId, ignored -> new ArrayList<>()).add(message);
    setDirty();
  }

  public List<String> drainNotifications(UUID playerId) {
    List<String> messages = offlineNotifications.remove(playerId);
    if (messages != null) {
      setDirty();
      return messages;
    }
    return Collections.emptyList();
  }

  public void recordTransaction(Clan clan, BankTransaction transaction) {
    clan.addTransaction(transaction, ClanCombatConfig.MAX_TRANSACTION_ENTRIES.get());
    setDirty();
  }

  public static String normalize(String value) {
    return value.toLowerCase(Locale.ROOT);
  }

  private static String playerClanDelayKey(UUID playerId, UUID clanId) {
    return playerId + ":" + clanId;
  }

  private static String directedClanKey(UUID firstClanId, UUID secondClanId) {
    return firstClanId + ">" + secondClanId;
  }

  private static void readUuidSet(ListTag tag, Set<UUID> target) {
    for (int i = 0; i < tag.size(); i++) {
      target.add(UUID.fromString(tag.getString(i)));
    }
  }

  private static ListTag writeUuidSet(Collection<UUID> values) {
    ListTag tag = new ListTag();
    for (UUID value : values) {
      tag.add(StringTag.valueOf(value.toString()));
    }
    return tag;
  }

  private static void readStringBooleanMap(ListTag tag, Map<String, Boolean> target) {
    for (int i = 0; i < tag.size(); i++) {
      CompoundTag entry = tag.getCompound(i);
      target.put(entry.getString("Key"), entry.getBoolean("Value"));
    }
  }

  private static ListTag writeStringBooleanMap(Map<String, Boolean> values) {
    ListTag tag = new ListTag();
    for (Map.Entry<String, Boolean> entry : values.entrySet()) {
      CompoundTag entryTag = new CompoundTag();
      entryTag.putString("Key", entry.getKey());
      entryTag.putBoolean("Value", entry.getValue());
      tag.add(entryTag);
    }
    return tag;
  }

  private static void readStringLongMap(ListTag tag, Map<String, Long> target) {
    for (int i = 0; i < tag.size(); i++) {
      CompoundTag entry = tag.getCompound(i);
      target.put(entry.getString("Key"), entry.getLong("Value"));
    }
  }

  private static ListTag writeStringLongMap(Map<String, Long> values) {
    ListTag tag = new ListTag();
    for (Map.Entry<String, Long> entry : values.entrySet()) {
      CompoundTag entryTag = new CompoundTag();
      entryTag.putString("Key", entry.getKey());
      entryTag.putLong("Value", entry.getValue());
      tag.add(entryTag);
    }
    return tag;
  }

  private static void readStringSet(ListTag tag, Set<String> target) {
    for (int i = 0; i < tag.size(); i++) {
      target.add(tag.getString(i));
    }
  }

  private static ListTag writeStringSet(Collection<String> values) {
    ListTag tag = new ListTag();
    for (String value : values) {
      tag.add(StringTag.valueOf(value));
    }
    return tag;
  }
  public void resyncTeams() {
    if (server == null) return;
    if (ClanCombatConfig.USE_VANILLA_TEAMS.get()) {
      for (Clan clan : clans.values()) {
        PlayerTeam team = getOrCreateTeam(clan);
        if (team == null) continue;
        for (UUID memberId : clan.getMembers()) {
          server.getScoreboard().addPlayerToTeam(playerName(memberId), team);
        }
      }
    }
    // Always re-push packet cache so the client arrow is correct regardless of teams setting
    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      syncClanMembersToPlayer(player.getUUID());
    }
  }
  public void clearAllPvpDelays() {
    pvpDelayUntil.clear();
    playerClanPvpDelayUntil.clear();
    combatSnapshots.clear();
    lastPvpDamageValidity.clear();
    setDirty();
  }
  public void clearPlayerPvpDelays(UUID playerId) {
    String id = playerId.toString();
    playerClanPvpDelayUntil.keySet().removeIf(key -> key.startsWith(id));
    clearCombatSnapshots(playerId);
    setDirty();
  }
  public void setInternalFriendlyFire(Clan clan, boolean enabled) {
    clan.setInternalFriendlyFire(enabled);
    clearCombatSnapshots(clan, clan); // bust snapshots for all members
    setDirty();
  }
  public void syncClanMembersToPlayer(UUID playerId) {
    if (server == null) return;
    ServerPlayer online = server.getPlayerList().getPlayer(playerId);
    if (online == null) return;

    Clan clan = clanOf(playerId).orElse(null);
    java.util.Set<UUID> mates = new java.util.HashSet<>();
    java.util.Map<UUID, String> roles = new java.util.HashMap<>();
    if (clan != null) {
      for (UUID memberId : clan.getMembers()) {
        if (!memberId.equals(playerId)) {
          mates.add(memberId);
          ClanRole role = clan.roleOf(memberId);
          if (role != null) roles.put(memberId, role.name());
        }
      }
    }

    java.util.Map<UUID, String> allRoles = new java.util.HashMap<>();
    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
      UUID pid = p.getUUID();
      Clan pClan = clanOf(pid).orElse(null);
      if (pClan != null) {
        ClanRole role = pClan.roleOf(pid);
        if (role != null) allRoles.put(pid, role.name());
      }
    }

    com.raiiiden.hierarchy.network.NetworkHandler.CHANNEL.sendTo(
            new com.raiiiden.hierarchy.network.SyncClanMembersPacket(mates, roles, allRoles),
            online.connection.connection,
            net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
  }
  public void syncClanMembersToAllOnlinePlayers() {
    if (server == null) return;
    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      syncClanMembersToPlayer(player.getUUID());
    }
  }
}
