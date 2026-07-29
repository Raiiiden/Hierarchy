package com.raiiiden.hierarchy.clan.currency;

import com.raiiiden.hierarchy.clan.config.ClanCombatConfig;
import com.raiiiden.hierarchy.clan.model.Clan;
import net.minecraft.server.level.ServerPlayer;

public record ActionCost(long currency, int xpLevels) {
  public boolean isFree() {
    return currency <= 0L && xpLevels <= 0;
  }

  public boolean canPay(ServerPlayer player, Clan clan) {
    if (currency > 0L) {
      if (ClanCombatConfig.USE_CLAN_BANK_FOR_ROE_COSTS.get() && ClanCombatConfig.ENABLE_CLAN_BANK.get() && clan != null) {
        if (!CurrencyManager.clanProvider().hasFunds(clan, currency)) {
          return false;
        }
      } else if (!CurrencyManager.hasPlayerFunds(player, currency)) {
        return false;
      }
    }
    return xpLevels <= 0 || (ClanCombatConfig.ENABLE_XP_COSTS.get() && player.experienceLevel >= xpLevels);
  }

  public boolean withdraw(ServerPlayer player, Clan clan) {
    if (!canPay(player, clan)) {
      return false;
    }
    if (currency > 0L) {
      if (ClanCombatConfig.USE_CLAN_BANK_FOR_ROE_COSTS.get() && ClanCombatConfig.ENABLE_CLAN_BANK.get() && clan != null) {
        if (!CurrencyManager.clanProvider().withdraw(clan, currency)) {
          return false;
        }
      } else if (!CurrencyManager.withdrawPlayer(player, currency)) {
        return false;
      }
    }
    if (xpLevels > 0) {
      player.giveExperienceLevels(-xpLevels);
    }
    return true;
  }
}
