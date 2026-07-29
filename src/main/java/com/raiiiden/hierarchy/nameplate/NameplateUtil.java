package com.raiiiden.hierarchy.nameplate;

import com.raiiiden.hierarchy.Hierarchy;
import com.raiiiden.hierarchy.bounty.config.BountyConfig;
import com.raiiiden.hierarchy.bounty.data.BountyData;
import com.raiiiden.hierarchy.clan.config.ClanCombatConfig;
import com.raiiiden.hierarchy.clan.data.ClanData;
import com.raiiiden.hierarchy.clan.model.Clan;
import com.raiiiden.hierarchy.humanity.HumanityRank;
import com.raiiiden.hierarchy.humanity.config.HumanityConfig;
import com.raiiiden.hierarchy.humanity.data.HumanityData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class NameplateUtil {
  // Entity DATA_CUSTOM_NAME is slot 2, DATA_CUSTOM_NAME_VISIBLE is slot 3.
  // These are stable for all vanilla 1.20.x entities.
  private static final int SLOT_CUSTOM_NAME         = 2;
  private static final int SLOT_CUSTOM_NAME_VISIBLE = 3;

  private NameplateUtil() {}

  // Sets the canonical custom name on the player entity (seen by anyone without a per-viewer override)
  public static void refresh(ServerPlayer target) {
    if (!NameplateConfig.ENABLE_CUSTOM_NAMEPLATES.get()) {
      target.setCustomName(null);
      target.setCustomNameVisible(false);
      return;
    }
    // Set the canonical custom name ON the entity so it lives in the entity's
    // synched data and travels with vanilla entity tracking. This is what makes
    // the nameplate survive death/respawn and dimension changes: the per-viewer
    // override packets (below) are keyed to the entity's network id, so when the
    // player entity is destroyed and recreated they can race the new entity's
    // spawn packet and get dropped — leaving the client to fall back to the plain
    // name. The canonical name is resent automatically as part of the entity's
    // metadata, so the plate is never lost.
    target.setCustomName(build(target.server, target.getUUID(),
            target.getGameProfile().getName(), null));
    target.setCustomNameVisible(true);
    // Push a per-viewer colored name to every currently online player
    for (ServerPlayer viewer : target.server.getPlayerList().getPlayers()) {
      if (viewer == target) continue;
      refreshForViewer(viewer, target);
    }
  }

  // Pushes a per-viewer nameplate packet directly to {@code viewer} for {@code target}.
  // Builds the SynchedEntityData payload manually — never mutates the entity.
  public static void refreshForViewer(ServerPlayer viewer, ServerPlayer target) {
    if (!NameplateConfig.ENABLE_CUSTOM_NAMEPLATES.get()) return;

    Component name = build(target.server, target.getUUID(), target.getGameProfile().getName(), viewer);

    List<SynchedEntityData.DataValue<?>> values = List.of(
            new SynchedEntityData.DataValue<>(
                    SLOT_CUSTOM_NAME,
                    EntityDataSerializers.OPTIONAL_COMPONENT,
                    Optional.of(name)
            ),
            new SynchedEntityData.DataValue<>(
                    SLOT_CUSTOM_NAME_VISIBLE,
                    EntityDataSerializers.BOOLEAN,
                    true
            )
    );

    viewer.connection.send(new ClientboundSetEntityDataPacket(target.getId(), values));
  }

  // Full refresh: sets the canonical name on every player, then pushes
  // per-viewer overrides for every viewer/target pair.
  public static void refreshAll(MinecraftServer server) {
    List<ServerPlayer> players = server.getPlayerList().getPlayers();
    for (ServerPlayer target : players) {
      if (!NameplateConfig.ENABLE_CUSTOM_NAMEPLATES.get()) {
        target.setCustomName(null);
        target.setCustomNameVisible(false);
        continue;
      }
      // Canonical name on the entity — survives respawn/re-tracking (see refresh()).
      target.setCustomName(build(target.server, target.getUUID(),
              target.getGameProfile().getName(), null));
      target.setCustomNameVisible(true);
      for (ServerPlayer viewer : players) {
        if (viewer == target) continue;
        refreshForViewer(viewer, target);
      }
    }
  }

  // On join: refresh canonical name for the new player, then push
  // per-viewer overrides in both directions.
  public static void refreshOnJoin(ServerPlayer joined) {
    refresh(joined);
    for (ServerPlayer other : joined.server.getPlayerList().getPlayers()) {
      if (other == joined) continue;
      refreshForViewer(joined, other);
      refreshForViewer(other, joined);
    }
  }

  public static Component clientNameplate(Player player, boolean isTeammate) {
    if (!player.hasCustomName()) return player.getName();
    return player.getCustomName();
  }

  public static Component clientNameplate(Player player) {
    return clientNameplate(player, false);
  }

  // -------------------------------------------------------------------------

  private static Component build(MinecraftServer server, UUID id, String username, ServerPlayer viewer) {
    MutableComponent component = Component.empty();

    if (NameplateConfig.SHOW_CLAN_TAG.get() && ClanCombatConfig.ENABLE_CLANS.get()) {
      ClanData data = ClanData.get(server);
      Clan targetClan = data.clanOf(id).orElse(null);
      if (targetClan != null && !targetClan.getTag().isBlank()) {
        ChatFormatting color = resolveTagColor(viewer, targetClan, data);
        component.append(
                Component.literal("[" + targetClan.getTag() + "] ").withStyle(color)
        );
      }
    }

    component.append(Component.literal(username).withStyle(ChatFormatting.GRAY));

    if (NameplateConfig.SHOW_HUMANITY.get() && HumanityConfig.ENABLE_HUMANITY.get()) {
      component.append(Component.literal(" "))
              .append(humanityLabel(HumanityData.get(server).humanityDisplay(id)));
    }

    if (NameplateConfig.SHOW_BOUNTY_ICON.get() && BountyConfig.ENABLE_BOUNTIES.get()) {
      if (BountyData.get(server).hasActiveBounty(id, System.currentTimeMillis())) {
        component.append(Component.literal(" ✛").withStyle(ChatFormatting.RED));
      }
    }

    return component;
  }

  private static ChatFormatting resolveTagColor(
          ServerPlayer viewer,
          Clan targetClan,
          ClanData data
  ) {
    Clan viewerClan =
            (viewer != null)
                    ? data.clanOf(viewer.getUUID()).orElse(null)
                    : null;

    ChatFormatting result =
            TabListManager.tagColor(viewerClan, targetClan, data);

    return result;
  }

  private static Component humanityLabel(int humanity) {
    return HumanityRank.styled(humanity);
  }
}