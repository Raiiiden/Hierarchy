package com.raiiiden.hierarchy.nameplate;

import com.raiiiden.hierarchy.Hierarchy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Hierarchy.MODID, value = Dist.CLIENT)
public final class ClientNameplateEvents {
  private ClientNameplateEvents() {
  }

  @SubscribeEvent
  public static void onRenderNameTag(RenderNameTagEvent event) {
    if (!NameplateConfig.ENABLE_CUSTOM_NAMEPLATES.get() || !(event.getEntity() instanceof Player player)) {
      return;
    }
    if (!shouldRenderNameplate(player)) {
      event.setResult(Event.Result.DENY);
      return;
    }
    event.setContent(NameplateUtil.clientNameplate(player));
  }

  private static boolean shouldRenderNameplate(Player player) {
    Minecraft minecraft = Minecraft.getInstance();
    Entity viewer = minecraft.getCameraEntity();
    if (viewer == null || viewer == player) {
      return false;
    }
    double maxDistance = NameplateConfig.MAX_RENDER_DISTANCE_BLOCKS.get();
    if (maxDistance <= 0.0D || viewer.distanceToSqr(player) > maxDistance * maxDistance) {
      return false;
    }
    return !NameplateConfig.REQUIRE_LINE_OF_SIGHT.get() || viewer instanceof LivingEntity livingViewer && livingViewer.hasLineOfSight(player);
  }
}
