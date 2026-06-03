package com.raiiiden.hierarchy.nameplate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.raiiiden.hierarchy.Hierarchy;
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
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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

    boolean isTeammate = areTeammates(minecraft, viewer, player);

    double maxDistance = isTeammate
            ? NameplateConfig.TEAMMATE_RENDER_DISTANCE_BLOCKS.get()
            : NameplateConfig.MAX_RENDER_DISTANCE_BLOCKS.get();

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
    if (!NameplateConfig.SHOW_TEAMMATE_ARROW.get()) return;

    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.level == null || mc.gameRenderer == null) return;

    var camera = mc.gameRenderer.getMainCamera();
    Vec3 camPos = camera.getPosition();
    PoseStack poseStack = event.getPoseStack();
    MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

    double maxDist = NameplateConfig.ARROW_RENDER_DISTANCE_BLOCKS.get();
    float scale = NameplateConfig.ARROW_SCALE.get().floatValue();
    double yOffset = NameplateConfig.ARROW_Y_OFFSET.get();

    Component arrow = Component.literal("▼").withStyle(ChatFormatting.GREEN);

    for (Player target : mc.level.players()) {
      if (target == mc.player) continue;
      if (!areTeammates(mc, mc.player, target)) continue;
      if (maxDist > 0 && mc.player.distanceToSqr(target) > maxDist * maxDist) continue;
      if (NameplateConfig.REQUIRE_LINE_OF_SIGHT.get() && !mc.player.hasLineOfSight(target)) continue;

      float partial = event.getPartialTick();
      double dx = (target.xo + (target.getX() - target.xo) * partial) - camPos.x;
      double dy = (target.yo + (target.getY() - target.yo) * partial) - camPos.y;
      double dz = (target.zo + (target.getZ() - target.zo) * partial) - camPos.z;

      // Place it above the nameplate: bbHeight + nameplate offset + extra offset
      double arrowY = dy + target.getBbHeight() + 0.5 + yOffset;

      poseStack.pushPose();
      poseStack.translate(dx, arrowY, dz);
      poseStack.mulPose(camera.rotation());
      poseStack.scale(-scale, -scale, scale);

      int halfWidth = mc.font.width(arrow) / 2;
      mc.font.drawInBatch(
              arrow,
              -halfWidth,
              0,
              0xFF55FF55, // bright green, explicit ARGB
              false,
              poseStack.last().pose(),
              bufferSource,
              Font.DisplayMode.NORMAL,
              0,
              LightTexture.FULL_BRIGHT
      );

      poseStack.popPose();
    }

    bufferSource.endBatch();
  }

  private static boolean areTeammates(Minecraft minecraft, Entity viewer, Player target) {
    if (!(viewer instanceof Player localPlayer) || minecraft.level == null) return false;
    Scoreboard scoreboard = minecraft.level.getScoreboard();
    PlayerTeam viewerTeam = scoreboard.getPlayersTeam(localPlayer.getGameProfile().getName());
    PlayerTeam targetTeam = scoreboard.getPlayersTeam(target.getGameProfile().getName());
    return viewerTeam != null && viewerTeam == targetTeam;
  }
}