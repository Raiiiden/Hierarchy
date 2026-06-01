package com.raiiiden.hierarchy;

import com.raiiiden.hierarchy.bounty.commands.BountyCommands;
import com.raiiiden.hierarchy.bounty.config.BountyConfig;
import com.raiiiden.hierarchy.bounty.events.BountyEvents;
import com.raiiiden.hierarchy.clan.commands.ClanCommands;
import com.raiiiden.hierarchy.clan.config.ClanCombatConfig;
import com.raiiiden.hierarchy.clan.events.ClanEvents;
import com.raiiiden.hierarchy.humanity.config.HumanityConfig;
import com.raiiiden.hierarchy.nameplate.NameplateConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Hierarchy.MODID)
public class Hierarchy {
  public static final String MODID = "hierarchy";
  public static final Logger LOGGER = LogManager.getLogger();

  public Hierarchy() {
    ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ClanCombatConfig.SPEC);
    ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BountyConfig.SPEC, "hierarchy-bounty.toml");
    ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, HumanityConfig.SPEC, "hierarchy-humanity.toml");
    ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, NameplateConfig.SPEC, "hierarchy-nameplates.toml");
    MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    MinecraftForge.EVENT_BUS.register(new ClanEvents());
    MinecraftForge.EVENT_BUS.register(new BountyEvents());
    LOGGER.info("[{}] Hierarchy mod initializing...", MODID);
  }

  private void registerCommands(RegisterCommandsEvent event) {
    ClanCommands.register(event.getDispatcher());
    BountyCommands.register(event.getDispatcher());
  }
}
