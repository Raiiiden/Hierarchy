package com.raiiiden.hierarchy.clan.currency;

import com.raiiiden.hierarchy.clan.bank.ClanBankCurrencyProvider;
import com.raiiiden.hierarchy.clan.config.ClanCombatConfig;
import net.minecraft.server.level.ServerPlayer;

public final class CurrencyManager {
  private static CurrencyProvider activeProvider;
  private static final CurrencyProvider XP_PROVIDER = new XpCurrencyProvider();
  private static final CurrencyProvider ITEM_PROVIDER = new ItemCurrencyProvider();
  private static final ClanCurrencyProvider CLAN_BANK_PROVIDER = new ClanBankCurrencyProvider();

  private CurrencyManager() {
  }

  public static void register(CurrencyProvider provider) {
    activeProvider = provider;
  }

  public static CurrencyProvider playerProvider() {
    if (activeProvider != null) {
      return activeProvider;
    }
    String type = ClanCombatConfig.PLAYER_CURRENCY_TYPE.get().trim().toLowerCase();
    if ("item".equals(type)) {
      return ITEM_PROVIDER;
    }
    return ClanCombatConfig.ALLOW_XP_CURRENCY_FALLBACK.get() ? XP_PROVIDER : null;
  }

  public static ClanCurrencyProvider clanProvider() {
    return CLAN_BANK_PROVIDER;
  }

  public static boolean hasPlayerFunds(ServerPlayer player, long amount) {
    CurrencyProvider provider = playerProvider();
    return provider != null && provider.hasFunds(player, amount);
  }

  public static boolean withdrawPlayer(ServerPlayer player, long amount) {
    CurrencyProvider provider = playerProvider();
    return provider != null && provider.withdraw(player, amount);
  }

  public static boolean depositPlayer(ServerPlayer player, long amount) {
    CurrencyProvider provider = playerProvider();
    return provider != null && provider.deposit(player, amount);
  }
}
