package com.raiiiden.hierarchy.clan.bank;

import java.util.UUID;

public record BankTransaction(BankTransactionType type, UUID playerId, String playerName, long amount, long timestamp, String reason) {
}
