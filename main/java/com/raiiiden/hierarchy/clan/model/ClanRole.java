package com.raiiiden.hierarchy.clan.model;

public enum ClanRole {
  LEADER,
  CO_LEADER,
  LIEUTENANT,
  OFFICER,
  MEMBER;

  // The next rank up, one step at a time. Caps at CO_LEADER — becoming LEADER is
  // handled by {@code /clan transfer}, not promotion. Returns {@code this} when no
  // higher promotable rank exists.
  public ClanRole promoted() {
    return switch (this) {
      case MEMBER -> OFFICER;
      case OFFICER -> LIEUTENANT;
      case LIEUTENANT -> CO_LEADER;
      case CO_LEADER, LEADER -> this;
    };
  }

  // The next rank down, one step at a time. Floors at MEMBER. Returns {@code this}
  // when no lower rank exists (MEMBER) or the rank is not demotable (LEADER).
  public ClanRole demoted() {
    return switch (this) {
      case CO_LEADER -> LIEUTENANT;
      case LIEUTENANT -> OFFICER;
      case OFFICER -> MEMBER;
      case MEMBER, LEADER -> this;
    };
  }

  // True if this rank is strictly higher than {@code other}. Ranks are declared
  // highest-first, so a lower ordinal means a higher rank (LEADER outranks all).
  public boolean outranks(ClanRole other) {
    return ordinal() < other.ordinal();
  }

  // Lower-case, hyphenated label for chat messages, e.g. {@code co-leader}.
  public String label() {
    return name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
  }
}
