package com.raiiiden.hierarchy.clan.currency;

import com.raiiiden.hierarchy.clan.model.Clan;

public interface ClanCurrencyProvider {
  boolean hasFunds(Clan clan, long amount);

  boolean withdraw(Clan clan, long amount);

  boolean deposit(Clan clan, long amount);
}
