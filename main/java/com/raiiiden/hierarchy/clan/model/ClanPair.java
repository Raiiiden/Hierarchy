package com.raiiiden.hierarchy.clan.model;

import java.util.UUID;

public final class ClanPair {
  private ClanPair() {
  }

  public static String key(UUID first, UUID second) {
    String a = first.toString();
    String b = second.toString();
    return a.compareTo(b) <= 0 ? a + ":" + b : b + ":" + a;
  }
}
