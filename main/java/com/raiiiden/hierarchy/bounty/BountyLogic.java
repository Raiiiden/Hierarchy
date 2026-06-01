package com.raiiiden.hierarchy.bounty;

import com.raiiiden.hierarchy.bounty.config.BountyConfig;
import com.raiiiden.hierarchy.bounty.data.BountyData;
import com.raiiiden.hierarchy.clan.data.ClanData;
import com.raiiiden.hierarchy.clan.model.Clan;
import com.raiiiden.hierarchy.humanity.config.HumanityConfig;
import com.raiiiden.hierarchy.humanity.data.HumanityData;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class BountyLogic {
  private BountyLogic() {
  }

  public static boolean relationshipAllowsBounty(MinecraftServer server, UUID placerId, UUID targetId) {
    ClanData clans = ClanData.get(server);
    Optional<Clan> placerClan = clans.clanOf(placerId);
    Optional<Clan> targetClan = clans.clanOf(targetId);
    if (placerClan.isEmpty() || targetClan.isEmpty()) {
      return true;
    }
    if (placerClan.get().getId().equals(targetClan.get().getId())) {
      return false;
    }
    return !clans.areAllied(placerClan.get().getId(), targetClan.get().getId());
  }

  public static boolean relationshipAllowsDogTag(MinecraftServer server, UUID killerId, UUID targetId) {
    return relationshipAllowsBounty(server, killerId, targetId);
  }

  public static boolean isEligibleTarget(MinecraftServer server, UUID targetId, long now) {
    BountyData bountyData = BountyData.get(server);
    HumanityData humanityData = HumanityData.get(server);
    int humanity = humanityData.humanity(targetId);
    if (HumanityConfig.ENABLE_HUMANITY.get() && humanity >= HumanityConfig.HIGH_HUMANITY_PROTECTED_THRESHOLD.get()) {
      return false;
    }
    if (bountyData.recentlyKilledPlayer(targetId, now)) {
      return true;
    }
    return HumanityConfig.ENABLE_HUMANITY.get() && humanity <= HumanityConfig.LOW_HUMANITY_BOUNTY_THRESHOLD.get();
  }

  public static long taxFor(long amount) {
    return Math.floorDiv(Math.round(amount * BountyConfig.TAX_PERCENT.get()), 100L);
  }
}
