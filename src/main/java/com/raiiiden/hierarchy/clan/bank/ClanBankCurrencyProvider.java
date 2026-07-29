package com.raiiiden.hierarchy.clan.bank;

import com.raiiiden.hierarchy.clan.currency.ClanCurrencyProvider;
import com.raiiiden.hierarchy.clan.model.Clan;

public class ClanBankCurrencyProvider implements ClanCurrencyProvider {
  @Override
  public boolean hasFunds(Clan clan, long amount) {
    synchronized (clan) {
      return amount >= 0L && clan.getBankBalance() >= amount;
    }
  }

  @Override
  public boolean withdraw(Clan clan, long amount) {
    synchronized (clan) {
      if (!hasFunds(clan, amount)) {
        return false;
      }
      clan.setBankBalance(clan.getBankBalance() - amount);
      return true;
    }
  }

  @Override
  public boolean deposit(Clan clan, long amount) {
    synchronized (clan) {
      if (amount < 0L || Long.MAX_VALUE - clan.getBankBalance() < amount) {
        return false;
      }
      clan.setBankBalance(clan.getBankBalance() + amount);
      return true;
    }
  }
}
