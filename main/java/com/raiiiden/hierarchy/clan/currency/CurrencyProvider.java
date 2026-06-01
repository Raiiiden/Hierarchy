package com.raiiiden.hierarchy.clan.currency;

import net.minecraft.server.level.ServerPlayer;

public interface CurrencyProvider {
  boolean hasFunds(ServerPlayer player, long amount);

  boolean withdraw(ServerPlayer player, long amount);

  boolean deposit(ServerPlayer player, long amount);
}
