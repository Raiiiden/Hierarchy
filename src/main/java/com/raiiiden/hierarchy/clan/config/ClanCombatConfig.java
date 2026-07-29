package com.raiiiden.hierarchy.clan.config;

import com.raiiiden.hierarchy.clan.bank.BankPermission;
import com.raiiiden.hierarchy.clan.currency.ActionCost;
import com.raiiiden.hierarchy.clan.currency.ClanAction;
import com.raiiiden.hierarchy.clan.model.ClanRole;
import net.minecraftforge.common.ForgeConfigSpec;

public final class ClanCombatConfig {
  public static final ForgeConfigSpec SPEC;
  public static final ForgeConfigSpec.BooleanValue ENABLE_CLANS;
  public static final ForgeConfigSpec.BooleanValue USE_VANILLA_TEAMS;
  public static final ForgeConfigSpec.BooleanValue ENABLE_FRIENDLY_FIRE_SYSTEM;
  public static final ForgeConfigSpec.LongValue PVP_DELAY_SECONDS;
  public static final ForgeConfigSpec.LongValue INVITE_EXPIRY_HOURS;
  public static final ForgeConfigSpec.BooleanValue ENABLE_RELATIONSHIP_SNAPSHOTS;
  public static final ForgeConfigSpec.BooleanValue ENABLE_NOTIFICATIONS;
  public static final ForgeConfigSpec.BooleanValue HANDLE_EXPLOSIONS;
  public static final ForgeConfigSpec.BooleanValue HANDLE_FIRE;
  public static final ForgeConfigSpec.BooleanValue HANDLE_ENVIRONMENTAL_ATTRIBUTION;
  public static final ForgeConfigSpec.BooleanValue ENABLE_CLAN_BANK;
  public static final ForgeConfigSpec.LongValue MIN_DEPOSIT_AMOUNT;
  public static final ForgeConfigSpec.LongValue MIN_WITHDRAW_AMOUNT;
  public static final ForgeConfigSpec.BooleanValue RESET_BANK_ON_WIPE;
  public static final ForgeConfigSpec.IntValue MAX_TRANSACTION_ENTRIES;
  public static final ForgeConfigSpec.BooleanValue ENABLE_BANK_AUDIT_LOGGING;
  public static final ForgeConfigSpec.BooleanValue ALLOW_XP_CURRENCY_FALLBACK;
  public static final ForgeConfigSpec.ConfigValue<String> PLAYER_CURRENCY_TYPE;
  public static final ForgeConfigSpec.ConfigValue<String> PLAYER_CURRENCY_ITEM;
  public static final ForgeConfigSpec.BooleanValue ENABLE_XP_COSTS;
  public static final ForgeConfigSpec.BooleanValue USE_CLAN_BANK_FOR_ROE_COSTS;
  public static final ForgeConfigSpec.BooleanValue BANK_LEADER_VIEW;
  public static final ForgeConfigSpec.BooleanValue BANK_LEADER_DEPOSIT;
  public static final ForgeConfigSpec.BooleanValue BANK_LEADER_WITHDRAW;
  public static final ForgeConfigSpec.BooleanValue BANK_LEADER_TRANSACTIONS;
  public static final ForgeConfigSpec.BooleanValue BANK_CO_LEADER_VIEW;
  public static final ForgeConfigSpec.BooleanValue BANK_CO_LEADER_DEPOSIT;
  public static final ForgeConfigSpec.BooleanValue BANK_CO_LEADER_WITHDRAW;
  public static final ForgeConfigSpec.BooleanValue BANK_CO_LEADER_TRANSACTIONS;
  public static final ForgeConfigSpec.BooleanValue BANK_LIEUTENANT_VIEW;
  public static final ForgeConfigSpec.BooleanValue BANK_LIEUTENANT_DEPOSIT;
  public static final ForgeConfigSpec.BooleanValue BANK_LIEUTENANT_WITHDRAW;
  public static final ForgeConfigSpec.BooleanValue BANK_LIEUTENANT_TRANSACTIONS;
  public static final ForgeConfigSpec.BooleanValue BANK_OFFICER_VIEW;
  public static final ForgeConfigSpec.BooleanValue BANK_OFFICER_DEPOSIT;
  public static final ForgeConfigSpec.BooleanValue BANK_OFFICER_WITHDRAW;
  public static final ForgeConfigSpec.BooleanValue BANK_OFFICER_TRANSACTIONS;
  public static final ForgeConfigSpec.BooleanValue BANK_MEMBER_VIEW;
  public static final ForgeConfigSpec.BooleanValue BANK_MEMBER_DEPOSIT;
  public static final ForgeConfigSpec.BooleanValue BANK_MEMBER_WITHDRAW;
  public static final ForgeConfigSpec.BooleanValue BANK_MEMBER_TRANSACTIONS;
  public static final ForgeConfigSpec.BooleanValue PERM_LEADER_INVITE;
  public static final ForgeConfigSpec.BooleanValue PERM_LEADER_KICK;
  public static final ForgeConfigSpec.BooleanValue PERM_LEADER_PROMOTE;
  public static final ForgeConfigSpec.BooleanValue PERM_LEADER_TAG;
  public static final ForgeConfigSpec.BooleanValue PERM_LEADER_DIPLOMACY;
  public static final ForgeConfigSpec.BooleanValue PERM_LEADER_FRIENDLY_FIRE;
  public static final ForgeConfigSpec.BooleanValue PERM_CO_LEADER_INVITE;
  public static final ForgeConfigSpec.BooleanValue PERM_CO_LEADER_KICK;
  public static final ForgeConfigSpec.BooleanValue PERM_CO_LEADER_PROMOTE;
  public static final ForgeConfigSpec.BooleanValue PERM_CO_LEADER_TAG;
  public static final ForgeConfigSpec.BooleanValue PERM_CO_LEADER_DIPLOMACY;
  public static final ForgeConfigSpec.BooleanValue PERM_CO_LEADER_FRIENDLY_FIRE;
  public static final ForgeConfigSpec.BooleanValue PERM_LIEUTENANT_INVITE;
  public static final ForgeConfigSpec.BooleanValue PERM_LIEUTENANT_KICK;
  public static final ForgeConfigSpec.BooleanValue PERM_LIEUTENANT_PROMOTE;
  public static final ForgeConfigSpec.BooleanValue PERM_LIEUTENANT_TAG;
  public static final ForgeConfigSpec.BooleanValue PERM_LIEUTENANT_DIPLOMACY;
  public static final ForgeConfigSpec.BooleanValue PERM_LIEUTENANT_FRIENDLY_FIRE;
  public static final ForgeConfigSpec.BooleanValue PERM_OFFICER_INVITE;
  public static final ForgeConfigSpec.BooleanValue PERM_OFFICER_KICK;
  public static final ForgeConfigSpec.BooleanValue PERM_OFFICER_PROMOTE;
  public static final ForgeConfigSpec.BooleanValue PERM_OFFICER_TAG;
  public static final ForgeConfigSpec.BooleanValue PERM_OFFICER_DIPLOMACY;
  public static final ForgeConfigSpec.BooleanValue PERM_OFFICER_FRIENDLY_FIRE;
  public static final ForgeConfigSpec.BooleanValue PERM_MEMBER_INVITE;
  public static final ForgeConfigSpec.BooleanValue PERM_MEMBER_KICK;
  public static final ForgeConfigSpec.BooleanValue PERM_MEMBER_PROMOTE;
  public static final ForgeConfigSpec.BooleanValue PERM_MEMBER_TAG;
  public static final ForgeConfigSpec.BooleanValue PERM_MEMBER_DIPLOMACY;
  public static final ForgeConfigSpec.BooleanValue PERM_MEMBER_FRIENDLY_FIRE;
  public static final ForgeConfigSpec.LongValue COST_CURRENCY_CLAN_CREATE;
  public static final ForgeConfigSpec.LongValue COST_CURRENCY_CLAN_DISBAND;
  public static final ForgeConfigSpec.LongValue COST_CURRENCY_CLAN_ALLY;
  public static final ForgeConfigSpec.LongValue COST_CURRENCY_CLAN_UNALLY;
  public static final ForgeConfigSpec.LongValue COST_CURRENCY_FRIENDLY_FIRE_ACCEPT;
  public static final ForgeConfigSpec.IntValue COST_XP_CLAN_CREATE;
  public static final ForgeConfigSpec.IntValue COST_XP_CLAN_DISBAND;
  public static final ForgeConfigSpec.IntValue COST_XP_CLAN_ALLY;
  public static final ForgeConfigSpec.IntValue COST_XP_CLAN_UNALLY;
  public static final ForgeConfigSpec.IntValue COST_XP_FRIENDLY_FIRE_ACCEPT;

  static {
    ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    builder.push("clanCombat");
    ENABLE_CLANS = builder
        .comment("Enable clan commands and clan PvP enforcement.")
        .define("enableClans", true);
    USE_VANILLA_TEAMS = builder
            .comment("Use vanilla scoreboard teams for clan membership (collision rules, /team commands, etc). " +
                    "Default false: membership is synced via packets only, avoiding scoreboard pollution from " +
                    "other mods or game events corrupting the teammate check.")
            .define("useVanillaTeams", false);
    ENABLE_FRIENDLY_FIRE_SYSTEM = builder
        .comment("Enable friendly fire request, acceptance, and revoke commands between allied clans.")
        .define("enableFriendlyFireSystem", true);
    PVP_DELAY_SECONDS = builder
        .comment("Seconds before PvP counts after leaving a clan, being kicked, unallying, or revoking friendly fire.")
        .defineInRange("pvpDelaySeconds", 300L, 0L, 604800L);
    INVITE_EXPIRY_HOURS = builder
        .comment("Hours before a pending clan invite expires (real time).")
        .defineInRange("inviteExpiryHours", 24L, 1L, 8760L);
    ENABLE_RELATIONSHIP_SNAPSHOTS = builder
        .comment("Snapshot clan relationships on the first damage event in a combat exchange.")
        .define("enableRelationshipSnapshots", true);
    ENABLE_NOTIFICATIONS = builder
        .comment("Store offline clan notifications and deliver them on next login.")
        .define("enableNotifications", true);
    HANDLE_EXPLOSIONS = builder
        .comment("Apply clan PvP relationship checks to player-caused explosion damage.")
        .define("handleExplosions", true);
    HANDLE_FIRE = builder
        .comment("Apply clan PvP relationship checks to player-attributed fire damage.")
        .define("handleFire", true);
    HANDLE_ENVIRONMENTAL_ATTRIBUTION = builder
        .comment("Apply clan PvP relationship checks when environmental damage has a player source.")
        .define("handleEnvironmentalAttribution", true);
    builder.pop();
    builder.push("clanBank");
    ENABLE_CLAN_BANK = builder
        .comment("Enable clan bank commands and clan bank backed costs.")
        .define("enableClanBank", true);
    MIN_DEPOSIT_AMOUNT = builder
        .comment("Minimum amount accepted by /clan bank deposit.")
        .defineInRange("minimumDepositAmount", 1L, 1L, Long.MAX_VALUE);
    MIN_WITHDRAW_AMOUNT = builder
        .comment("Minimum amount accepted by /clan bank withdraw.")
        .defineInRange("minimumWithdrawAmount", 1L, 1L, Long.MAX_VALUE);
    RESET_BANK_ON_WIPE = builder
        .comment("Intended wipe behavior flag. Clan bank is stored in world SavedData, so deleting world data resets it.")
        .define("resetBankOnWipe", true);
    MAX_TRANSACTION_ENTRIES = builder
        .comment("Maximum successful bank transactions stored per clan.")
        .defineInRange("maxTransactionEntries", 50, 1, 500);
    ENABLE_BANK_AUDIT_LOGGING = builder
        .comment("Log successful clan bank deposits and withdrawals to the server log.")
        .define("enableAuditLogging", false);
    ENABLE_XP_COSTS = builder
        .comment("Enable XP level costs for configured ROE actions.")
        .define("enableXpCosts", true);
    USE_CLAN_BANK_FOR_ROE_COSTS = builder
        .comment("Use clan bank for ROE action currency costs when a clan already exists; otherwise use player currency.")
        .define("useClanBankForRoeCosts", false);
    builder.pop();
    builder.push("currency");
    ALLOW_XP_CURRENCY_FALLBACK = builder
        .comment("Use XP levels as currency if no external CurrencyProvider is registered and playerCurrencyType is xp.")
        .define("allowXpCurrencyFallback", true);
    PLAYER_CURRENCY_TYPE = builder
        .comment("Built-in player currency used when no external CurrencyProvider is registered. Valid values: xp, item.")
        .define("playerCurrencyType", "xp");
    PLAYER_CURRENCY_ITEM = builder
        .comment("Item used when playerCurrencyType is item. Example: minecraft:iron_ingot.")
        .define("playerCurrencyItem", "minecraft:iron_ingot");
    builder.pop();
    builder.push("clanRolePermissions");
    PERM_LEADER_INVITE = builder.define("leaderInvite", true);
    PERM_LEADER_KICK = builder.define("leaderKick", true);
    PERM_LEADER_PROMOTE = builder.define("leaderPromote", true);
    PERM_LEADER_TAG = builder.define("leaderTag", true);
    PERM_LEADER_DIPLOMACY = builder.define("leaderDiplomacy", true);
    PERM_LEADER_FRIENDLY_FIRE = builder.define("leaderFriendlyFire", true);
    PERM_CO_LEADER_INVITE = builder.define("coLeaderInvite", true);
    PERM_CO_LEADER_KICK = builder.define("coLeaderKick", true);
    PERM_CO_LEADER_PROMOTE = builder.define("coLeaderPromote", true);
    PERM_CO_LEADER_TAG = builder.define("coLeaderTag", true);
    PERM_CO_LEADER_DIPLOMACY = builder.define("coLeaderDiplomacy", true);
    PERM_CO_LEADER_FRIENDLY_FIRE = builder.define("coLeaderFriendlyFire", true);
    PERM_LIEUTENANT_INVITE = builder.define("lieutenantInvite", true);
    PERM_LIEUTENANT_KICK = builder.define("lieutenantKick", true);
    PERM_LIEUTENANT_PROMOTE = builder.define("lieutenantPromote", false);
    PERM_LIEUTENANT_TAG = builder.define("lieutenantTag", false);
    PERM_LIEUTENANT_DIPLOMACY = builder.define("lieutenantDiplomacy", false);
    PERM_LIEUTENANT_FRIENDLY_FIRE = builder.define("lieutenantFriendlyFire", false);
    PERM_OFFICER_INVITE = builder.define("officerInvite", true);
    PERM_OFFICER_KICK = builder.define("officerKick", false);
    PERM_OFFICER_PROMOTE = builder.define("officerPromote", false);
    PERM_OFFICER_TAG = builder.define("officerTag", false);
    PERM_OFFICER_DIPLOMACY = builder.define("officerDiplomacy", false);
    PERM_OFFICER_FRIENDLY_FIRE = builder.define("officerFriendlyFire", false);
    PERM_MEMBER_INVITE = builder.define("memberInvite", false);
    PERM_MEMBER_KICK = builder.define("memberKick", false);
    PERM_MEMBER_PROMOTE = builder.define("memberPromote", false);
    PERM_MEMBER_TAG = builder.define("memberTag", false);
    PERM_MEMBER_DIPLOMACY = builder.define("memberDiplomacy", false);
    PERM_MEMBER_FRIENDLY_FIRE = builder.define("memberFriendlyFire", false);
    builder.pop();
    builder.push("clanBankRolePermissions");
    BANK_LEADER_VIEW = builder.define("leaderView", true);
    BANK_LEADER_DEPOSIT = builder.define("leaderDeposit", true);
    BANK_LEADER_WITHDRAW = builder.define("leaderWithdraw", true);
    BANK_LEADER_TRANSACTIONS = builder.define("leaderTransactions", true);
    BANK_CO_LEADER_VIEW = builder.define("coLeaderView", true);
    BANK_CO_LEADER_DEPOSIT = builder.define("coLeaderDeposit", true);
    BANK_CO_LEADER_WITHDRAW = builder.define("coLeaderWithdraw", true);
    BANK_CO_LEADER_TRANSACTIONS = builder.define("coLeaderTransactions", true);
    BANK_LIEUTENANT_VIEW = builder.define("lieutenantView", true);
    BANK_LIEUTENANT_DEPOSIT = builder.define("lieutenantDeposit", true);
    BANK_LIEUTENANT_WITHDRAW = builder.define("lieutenantWithdraw", false);
    BANK_LIEUTENANT_TRANSACTIONS = builder.define("lieutenantTransactions", true);
    BANK_OFFICER_VIEW = builder.define("officerView", true);
    BANK_OFFICER_DEPOSIT = builder.define("officerDeposit", true);
    BANK_OFFICER_WITHDRAW = builder.define("officerWithdraw", false);
    BANK_OFFICER_TRANSACTIONS = builder.define("officerTransactions", false);
    BANK_MEMBER_VIEW = builder.define("memberView", false);
    BANK_MEMBER_DEPOSIT = builder.define("memberDeposit", false);
    BANK_MEMBER_WITHDRAW = builder.define("memberWithdraw", false);
    BANK_MEMBER_TRANSACTIONS = builder.define("memberTransactions", false);
    builder.pop();
    builder.push("actionCurrencyCosts");
    COST_CURRENCY_CLAN_CREATE = builder.defineInRange("clanCreate", 0L, 0L, Long.MAX_VALUE);
    COST_CURRENCY_CLAN_DISBAND = builder.defineInRange("clanDisband", 0L, 0L, Long.MAX_VALUE);
    COST_CURRENCY_CLAN_ALLY = builder.defineInRange("clanAlly", 0L, 0L, Long.MAX_VALUE);
    COST_CURRENCY_CLAN_UNALLY = builder.defineInRange("clanUnally", 0L, 0L, Long.MAX_VALUE);
    COST_CURRENCY_FRIENDLY_FIRE_ACCEPT = builder.defineInRange("friendlyFireAccept", 0L, 0L, Long.MAX_VALUE);
    builder.pop();
    builder.push("actionXpCosts");
    COST_XP_CLAN_CREATE = builder.defineInRange("clanCreate", 0, 0, Integer.MAX_VALUE);
    COST_XP_CLAN_DISBAND = builder.defineInRange("clanDisband", 0, 0, Integer.MAX_VALUE);
    COST_XP_CLAN_ALLY = builder.defineInRange("clanAlly", 0, 0, Integer.MAX_VALUE);
    COST_XP_CLAN_UNALLY = builder.defineInRange("clanUnally", 0, 0, Integer.MAX_VALUE);
    COST_XP_FRIENDLY_FIRE_ACCEPT = builder.defineInRange("friendlyFireAccept", 0, 0, Integer.MAX_VALUE);
    builder.pop();
    SPEC = builder.build();
  }

  public static ActionCost cost(ClanAction action) {
    return new ActionCost(currencyCost(action), xpCost(action));
  }

  private static long currencyCost(ClanAction action) {
    return switch (action) {
      case CLAN_CREATE -> COST_CURRENCY_CLAN_CREATE.get();
      case CLAN_DISBAND -> COST_CURRENCY_CLAN_DISBAND.get();
      case CLAN_ALLY -> COST_CURRENCY_CLAN_ALLY.get();
      case CLAN_UNALLY -> COST_CURRENCY_CLAN_UNALLY.get();
      case FRIENDLY_FIRE_ACCEPT -> COST_CURRENCY_FRIENDLY_FIRE_ACCEPT.get();
    };
  }

  private static int xpCost(ClanAction action) {
    return switch (action) {
      case CLAN_CREATE -> COST_XP_CLAN_CREATE.get();
      case CLAN_DISBAND -> COST_XP_CLAN_DISBAND.get();
      case CLAN_ALLY -> COST_XP_CLAN_ALLY.get();
      case CLAN_UNALLY -> COST_XP_CLAN_UNALLY.get();
      case FRIENDLY_FIRE_ACCEPT -> COST_XP_FRIENDLY_FIRE_ACCEPT.get();
    };
  }

  public static boolean bankPermission(ClanRole role, BankPermission permission) {
    if (role == null) {
      return false;
    }
    return switch (role) {
      case LEADER -> switch (permission) {
        case BANK_VIEW -> BANK_LEADER_VIEW.get();
        case BANK_DEPOSIT -> BANK_LEADER_DEPOSIT.get();
        case BANK_WITHDRAW -> BANK_LEADER_WITHDRAW.get();
        case BANK_VIEW_TRANSACTIONS -> BANK_LEADER_TRANSACTIONS.get();
      };
      case CO_LEADER -> switch (permission) {
        case BANK_VIEW -> BANK_CO_LEADER_VIEW.get();
        case BANK_DEPOSIT -> BANK_CO_LEADER_DEPOSIT.get();
        case BANK_WITHDRAW -> BANK_CO_LEADER_WITHDRAW.get();
        case BANK_VIEW_TRANSACTIONS -> BANK_CO_LEADER_TRANSACTIONS.get();
      };
      case LIEUTENANT -> switch (permission) {
        case BANK_VIEW -> BANK_LIEUTENANT_VIEW.get();
        case BANK_DEPOSIT -> BANK_LIEUTENANT_DEPOSIT.get();
        case BANK_WITHDRAW -> BANK_LIEUTENANT_WITHDRAW.get();
        case BANK_VIEW_TRANSACTIONS -> BANK_LIEUTENANT_TRANSACTIONS.get();
      };
      case OFFICER -> switch (permission) {
        case BANK_VIEW -> BANK_OFFICER_VIEW.get();
        case BANK_DEPOSIT -> BANK_OFFICER_DEPOSIT.get();
        case BANK_WITHDRAW -> BANK_OFFICER_WITHDRAW.get();
        case BANK_VIEW_TRANSACTIONS -> BANK_OFFICER_TRANSACTIONS.get();
      };
      case MEMBER -> switch (permission) {
        case BANK_VIEW -> BANK_MEMBER_VIEW.get();
        case BANK_DEPOSIT -> BANK_MEMBER_DEPOSIT.get();
        case BANK_WITHDRAW -> BANK_MEMBER_WITHDRAW.get();
        case BANK_VIEW_TRANSACTIONS -> BANK_MEMBER_TRANSACTIONS.get();
      };
    };
  }

  private ClanCombatConfig() {
  }
}
