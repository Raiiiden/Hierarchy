package com.raiiiden.hierarchy.nameplate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.raiiiden.hierarchy.Hierarchy;
import com.raiiiden.hierarchy.party.config.PartyConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Hierarchy.MODID, value = Dist.CLIENT)
public final class ClientNameplateEvents {
  private ClientNameplateEvents() {}

  @SubscribeEvent
  public static void onRenderNameTag(RenderNameTagEvent event) {
    if (!NameplateConfig.ENABLE_CUSTOM_NAMEPLATES.get()
            || !(event.getEntity() instanceof Player player)) {
      return;
    }

    Minecraft minecraft = Minecraft.getInstance();
    Entity viewer = minecraft.getCameraEntity();
    if (viewer == null || viewer == player) {
      event.setResult(Event.Result.DENY);
      return;
    }

    boolean isTeammate = ClientClanCache.isClanmate(player.getUUID());

    double maxDistance = isTeammate
            ? ClientClanCache.teammateRenderDistance()
            : ClientClanCache.maxRenderDistance();

    if (maxDistance > 0.0D && viewer.distanceToSqr(player) > maxDistance * maxDistance) {
      event.setResult(Event.Result.DENY);
      return;
    }

    if (NameplateConfig.REQUIRE_LINE_OF_SIGHT.get()
            && !(viewer instanceof LivingEntity lv && lv.hasLineOfSight(player))) {
      event.setResult(Event.Result.DENY);
      return;
    }

    event.setContent(NameplateUtil.clientNameplate(player, isTeammate));
  }

  @SubscribeEvent
  public static void onRenderLevel(RenderLevelStageEvent event) {
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
    if (!NameplateConfig.ENABLE_CUSTOM_NAMEPLATES.get()) return;

    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.level == null || mc.gameRenderer == null) return;

    var camera = mc.gameRenderer.getMainCamera();
    Vec3 camPos = camera.getPosition();
    PoseStack poseStack = event.getPoseStack();
    MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

    renderClanRoles(mc, poseStack, bufferSource, camPos, event.getPartialTick());

    boolean renderArrow   = NameplateConfig.SHOW_TEAMMATE_ARROW.get();
    boolean renderParty   = PartyConfig.ENABLE_PARTIES.get()
            && PartyConfig.SHOW_PARTY_INDICATOR.get();

    if (!renderArrow && !renderParty) {
      bufferSource.endBatch();
      return;
    }

    float   scale   = NameplateConfig.ARROW_SCALE.get().floatValue();
    double  yOffset = NameplateConfig.ARROW_Y_OFFSET.get();
    float   partial = event.getPartialTick();

    // ------------------------------------------------------------------
    // Clan teammate arrow  ▼  (green)
    // ------------------------------------------------------------------
    if (renderArrow) {
      double maxDist = ClientClanCache.arrowRenderDistance();
      Component arrow = Component.literal("▼").withStyle(ChatFormatting.GREEN);

      for (Player target : mc.level.players()) {
        if (target == mc.player) continue;
        if (!ClientClanCache.isClanmate(target.getUUID())) continue;
        if (maxDist > 0 && mc.player.distanceToSqr(target) > maxDist * maxDist) continue;
        if (NameplateConfig.REQUIRE_LINE_OF_SIGHT.get()
                && !mc.player.hasLineOfSight(target)) continue;

        double dx = lerp(target.xo, target.getX(), partial) - camPos.x;
        double dy = lerp(target.yo, target.getY(), partial) - camPos.y;
        double dz = lerp(target.zo, target.getZ(), partial) - camPos.z;

        poseStack.pushPose();
        poseStack.translate(dx, dy + target.getBbHeight() + 0.5 + yOffset, dz);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-scale, -scale, scale);

        int halfWidth = mc.font.width(arrow) / 2;
        mc.font.drawInBatch(arrow, -halfWidth, 0, 0xFF55FF55, false,
                poseStack.last().pose(), bufferSource,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);

        poseStack.popPose();
      }
    }

    // ------------------------------------------------------------------
    // Party indicator  ◆  (configurable color) /  ★  (gold, leader)
    // Clan indicator takes priority — skipped for clan teammates.
    // ------------------------------------------------------------------
    if (renderParty) {
      double partyMaxDist = PartyConfig.PARTY_RENDER_DISTANCE_BLOCKS.get();

      ChatFormatting memberColor = ChatFormatting.getByName(
              PartyConfig.PARTY_INDICATOR_COLOR.get());
      if (memberColor == null || !memberColor.isColor()) {
        memberColor = ChatFormatting.LIGHT_PURPLE;
      }

      UUID leaderUUID = ClientClanCache.partyLeaderId();

      for (Player target : mc.level.players()) {
        if (target == mc.player) continue;
        if (!ClientClanCache.isPartymate(target.getUUID())) continue;
        // Clan indicator has priority — never stack indicators on the same player
        if (ClientClanCache.isClanmate(target.getUUID())) continue;
        if (partyMaxDist > 0
                && mc.player.distanceToSqr(target) > partyMaxDist * partyMaxDist) continue;
        if (NameplateConfig.REQUIRE_LINE_OF_SIGHT.get()
                && !mc.player.hasLineOfSight(target)) continue;

        boolean isLeader = leaderUUID != null
                && leaderUUID.equals(target.getUUID());
        Component indicator = Component.literal("◆")
                .withStyle(isLeader ? ChatFormatting.GOLD : memberColor);

        double dx = lerp(target.xo, target.getX(), partial) - camPos.x;
        double dy = lerp(target.yo, target.getY(), partial) - camPos.y;
        double dz = lerp(target.zo, target.getZ(), partial) - camPos.z;

        poseStack.pushPose();
        poseStack.translate(dx, dy + target.getBbHeight() + 0.5 + yOffset, dz);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-scale, -scale, scale);

        int halfWidth = mc.font.width(indicator) / 2;
        mc.font.drawInBatch(indicator, -halfWidth, 0, 0xFFFFFFFF, false,
                poseStack.last().pose(), bufferSource,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);

        poseStack.popPose();
      }
    }

    bufferSource.endBatch();
  }

  // -------------------------------------------------------------------------
  // Clan role labels — rendered above every visible player's nameplate
  // -------------------------------------------------------------------------

  private static void renderClanRoles(
          Minecraft mc,
          PoseStack poseStack,
          MultiBufferSource.BufferSource bufferSource,
          Vec3 camPos,
          float partial
  ) {
    var camera = mc.gameRenderer.getMainCamera();
    double maxDist = ClientClanCache.maxRenderDistance();
    float scale = 0.025F;

    for (Player target : mc.level.players()) {
      if (target == mc.player) continue;
      if (maxDist > 0 && mc.player.distanceToSqr(target) > maxDist * maxDist) continue;
      if (NameplateConfig.REQUIRE_LINE_OF_SIGHT.get()
              && !mc.player.hasLineOfSight(target)) continue;

      String roleName = roleDisplay(ClientClanCache.playerRole(target.getUUID()));
      if (roleName.isBlank()) continue;

      double dx = lerp(target.xo, target.getX(), partial) - camPos.x;
      double dy = lerp(target.yo, target.getY(), partial) - camPos.y;
      double dz = lerp(target.zo, target.getZ(), partial) - camPos.z;

      Component role = Component.literal(roleName);

      poseStack.pushPose();
      poseStack.translate(dx, dy + target.getBbHeight() + 0.72D, dz);
      poseStack.mulPose(camera.rotation());
      poseStack.scale(-scale, -scale, scale);

      int halfWidth = mc.font.width(role) / 2;
      mc.font.drawInBatch(role, -halfWidth, 0, 0xFF0E5C2F, false,
              poseStack.last().pose(), bufferSource,
              Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);

      poseStack.popPose();
    }
  }

  private static String roleDisplay(String role) {
    return switch (role) {
      case "LEADER"     -> "Leader";
      case "CO_LEADER"  -> "Co-Leader";
      case "LIEUTENANT" -> "Lieutenant";
      case "OFFICER"    -> "Officer";
      case "MEMBER"     -> "Member";
      default           -> "";
    };
  }

  // Linear interpolation helper — replaces the verbose inline form used previously
  private static double lerp(double prev, double curr, float partial) {
    return prev + (curr - prev) * partial;
  }

  @SubscribeEvent
  public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
    ClientClanCache.clear();
  }
}