package com.raiiiden.hierarchy.clan.currency;

import net.minecraft.server.level.ServerPlayer;

public class XpCurrencyProvider implements CurrencyProvider {
  @Override
  public boolean hasFunds(ServerPlayer player, long amount) {
    return amount <= Integer.MAX_VALUE && player.experienceLevel >= amount;
  }

  @Override
  public boolean withdraw(ServerPlayer player, long amount) {
    if (!hasFunds(player, amount)) {
      return false;
    }
    player.giveExperienceLevels((int) -amount);
    return true;
  }

  @Override
  public boolean deposit(ServerPlayer player, long amount) {
    if (amount > Integer.MAX_VALUE) {
      return false;
    }
    player.giveExperienceLevels((int) amount);
    return true;
  }
}
