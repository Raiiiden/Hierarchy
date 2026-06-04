package com.raiiiden.hierarchy.bounty.gui;

import com.raiiiden.hierarchy.bounty.BountyLogic;
import com.raiiiden.hierarchy.bounty.config.BountyConfig;
import com.raiiiden.hierarchy.bounty.data.BountyData;
import com.raiiiden.hierarchy.bounty.events.BountyEvents;
import com.raiiiden.hierarchy.bounty.model.Bounty;
import com.raiiiden.hierarchy.clan.config.ClanCombatConfig;
import com.raiiiden.hierarchy.clan.currency.CurrencyManager;
import com.raiiiden.hierarchy.humanity.config.HumanityConfig;
import com.raiiiden.hierarchy.humanity.data.HumanityData;
import com.raiiiden.hierarchy.nameplate.NameplateUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class BountyMenus {
  private static final int PLAYER_INV_START_3_ROWS = 27;
  private static final int PLAYER_INV_START_6_ROWS = 54;

  private BountyMenus() {
  }

  public static void openMain(ServerPlayer player) {
    SimpleContainer container = new SimpleContainer(27);
    container.setItem(10, button(Items.PLAYER_HEAD.getDefaultInstance(), "Place Bounty", "Select a target."));
    container.setItem(12, button(Items.CROSSBOW.getDefaultInstance(), "Active Bounties", "Browse the bounty board."));
    container.setItem(14, button(Items.CHEST.getDefaultInstance(), "My Contributions", "Remove rewards you added."));
    container.setItem(16, button(Items.TRIPWIRE_HOOK.getDefaultInstance(), "Redeem Contract", "Claim a bounty contract."));
    open(player, "Bounty Board", (id, inv, viewer) -> new ActionMenu(MenuType.GENERIC_9x3, id, inv, container, 3, slot -> {
      if (slot == 10) openPlayerSelection(player, false, 0);
      if (slot == 12) openActiveBoard(player, 0);
      if (slot == 14) openMyContributions(player, 0);
      if (slot == 16) openRedemption(player);
    }));
  }

  public static void openPlayerSelection(ServerPlayer player, boolean activeOnly, int page) {
    BountyData data = BountyData.get(player.server);
    long now = System.currentTimeMillis();
    data.expireAll(now, player.server);
    List<ServerPlayer> players = player.server.getPlayerList().getPlayers().stream()
        .filter(target -> !target.getUUID().equals(player.getUUID()))
        .filter(target -> !activeOnly || data.bounty(target.getUUID()).isPresent())
        .sorted(Comparator.comparing(target -> target.getGameProfile().getName(), String.CASE_INSENSITIVE_ORDER))
        .toList();
    SimpleContainer container = new SimpleContainer(54);
    int start = Math.max(0, page * 45);
    for (int i = 0; i < 45 && start + i < players.size(); i++) {
      ServerPlayer target = players.get(start + i);
      boolean active = data.bounty(target.getUUID()).isPresent();
      ItemStack head = playerHead(target);
      List<String> lore = new ArrayList<>();
      lore.add("Humanity: " + humanityRank(player.server, target.getUUID()));
      if (active) {
        lore.add(ChatFormatting.RED + "Active Bounty");
        lore.add("Click to Contribute");
      }
      setLore(head, lore);
      container.setItem(i, head);
    }
    addPager(container, page, players.size(), "Back");
    open(player, activeOnly ? "Contribute" : "Place Bounty", (id, inv, viewer) -> new ActionMenu(MenuType.GENERIC_9x6, id, inv, container, 6, slot -> {
      if (slot == 45) openMain(player);
      if (slot == 48 && page > 0) openPlayerSelection(player, activeOnly, page - 1);
      if (slot == 50 && (page + 1) * 45 < players.size()) openPlayerSelection(player, activeOnly, page + 1);
      if (slot >= 0 && slot < 45 && start + slot < players.size()) {
        ServerPlayer target = players.get(start + slot);
        Bounty bounty = data.bounty(target.getUUID()).orElse(null);
        if (bounty != null) {
          openContribute(player, target.getUUID());
        } else if (!activeOnly) {
          openCreate(player, target);
        }
      }
    }));
  }

  public static void openCreate(ServerPlayer player, ServerPlayer target) {
    SimpleContainer container = new SimpleContainer(27);
    container.setItem(11, button(Items.LIME_WOOL.getDefaultInstance(), "Confirm", "Place this bounty."));
    container.setItem(13, button(Items.BARRIER.getDefaultInstance(), "Cancel", "Return rewards."));
    container.setItem(15, button(Items.ARROW.getDefaultInstance(), "Back", "Return to target selection."));
    open(player, "Create: " + target.getGameProfile().getName(), (id, inv, viewer) -> new RewardEditMenu(id, inv, container, 3, 0, target.getUUID(), true, target.getGameProfile().getName()));
  }

  public static void openContribute(ServerPlayer player, UUID targetId) {
    BountyData data = BountyData.get(player.server);
    Bounty bounty = data.bounty(targetId).orElse(null);
    if (bounty == null) {
      player.sendSystemMessage(Component.literal("That target does not have an active bounty."));
      openPlayerSelection(player, true, 0);
      return;
    }
    SimpleContainer container = new SimpleContainer(27);
    for (Bounty.RewardItem reward : bounty.rewards()) {
      ItemStack stack = reward.stack().copy();
      appendLore(stack, List.of("Locked reward", "Contributor: " + reward.contributorId()));
      container.setItem(reward.slot(), stack);
    }
    container.setItem(11, button(Items.LIME_WOOL.getDefaultInstance(), "Confirm", "Add your rewards."));
    container.setItem(13, button(Items.BARRIER.getDefaultInstance(), "Cancel", "Return unconfirmed rewards."));
    container.setItem(15, button(Items.ARROW.getDefaultInstance(), "Back", "Return to active targets."));
    open(player, "Contribute: " + bounty.targetName(), (id, inv, viewer) -> new RewardEditMenu(id, inv, container, 3, bounty.rewards().size(), targetId, false, bounty.targetName()));
  }

  public static void openActiveBoard(ServerPlayer player, int page) {
    BountyData data = BountyData.get(player.server);
    long now = System.currentTimeMillis();
    data.expireAll(now, player.server);
    List<Bounty> bounties = data.activeBounties().stream()
        .sorted(Comparator.comparing(Bounty::targetName, String.CASE_INSENSITIVE_ORDER))
        .toList();
    SimpleContainer container = new SimpleContainer(54);
    int start = Math.max(0, page * 45);
    for (int i = 0; i < 45 && start + i < bounties.size(); i++) {
      Bounty bounty = bounties.get(start + i);
      ItemStack stack = button(Items.PLAYER_HEAD.getDefaultInstance(), bounty.targetName(), bountyLore(player, bounty, now));
      stack.getOrCreateTag().putString("SkullOwner", bounty.targetName());
      container.setItem(i, stack);
    }
    addPager(container, page, bounties.size(), "Back");
    open(player, "Active Bounties", (id, inv, viewer) -> new ActionMenu(MenuType.GENERIC_9x6, id, inv, container, 6, slot -> {
      if (slot == 45) openMain(player);
      if (slot == 48 && page > 0) openActiveBoard(player, page - 1);
      if (slot == 50 && (page + 1) * 45 < bounties.size()) openActiveBoard(player, page + 1);
      if (slot >= 0 && slot < 45 && start + slot < bounties.size()) openContribute(player, bounties.get(start + slot).targetId());
    }));
  }

  public static void openMyContributions(ServerPlayer player, int page) {
    BountyData data = BountyData.get(player.server);
    List<Bounty> mine = data.activeBounties().stream()
        .filter(bounty -> bounty.contributions().containsKey(player.getUUID()))
        .sorted(Comparator.comparing(Bounty::targetName, String.CASE_INSENSITIVE_ORDER))
        .toList();
    SimpleContainer container = new SimpleContainer(54);
    int start = Math.max(0, page * 45);
    for (int i = 0; i < 45 && start + i < mine.size(); i++) {
      Bounty bounty = mine.get(start + i);
      container.setItem(i, button(Items.CHEST.getDefaultInstance(), bounty.targetName(), "Click to remove only your rewards."));
    }
    addPager(container, page, mine.size(), "Back");
    open(player, "My Contributions", (id, inv, viewer) -> new ActionMenu(MenuType.GENERIC_9x6, id, inv, container, 6, slot -> {
      if (slot == 45) openMain(player);
      if (slot == 48 && page > 0) openMyContributions(player, page - 1);
      if (slot == 50 && (page + 1) * 45 < mine.size()) openMyContributions(player, page + 1);
      if (slot >= 0 && slot < 45 && start + slot < mine.size()) {
        List<ItemStack> removed = data.removeContribution(mine.get(start + slot).targetId(), player.getUUID(), player.server);
        for (ItemStack stack : removed) giveOrDrop(player, stack);
        player.sendSystemMessage(Component.literal(removed.isEmpty() ? "No rewards removed." : "Removed your bounty rewards."));
        openMyContributions(player, page);
      }
    }));
  }

  public static void openRedemption(ServerPlayer player) {
    ItemStack contract = BountyEvents.findDogTag(player);
    if (contract.isEmpty()) {
      player.sendSystemMessage(Component.literal("You do not have a bounty contract to redeem."));
      return;
    }
    CompoundTag tag = contract.getOrCreateTag();
    if (!tag.hasUUID("BountyId") || !tag.hasUUID("BountyTarget") || !tag.getUUID("BountyKiller").equals(player.getUUID())) {
      player.sendSystemMessage(Component.literal("That contract is invalid."));
      return;
    }
    if (!payClaimCost(player)) {
      player.sendSystemMessage(Component.literal("You do not have enough currency."));
      return;
    }
    long now = System.currentTimeMillis();
    List<ItemStack> rewards = BountyData.get(player.server).startClaim(tag.getUUID("BountyTarget"), tag.getUUID("BountyId"), now, player.server);
    if (rewards.isEmpty()) {
      player.sendSystemMessage(Component.literal("That bounty is no longer active."));
      return;
    }
    contract.shrink(1);
    if (HumanityConfig.ENABLE_HUMANITY.get()) {
      HumanityData.get(player.server).add(player.getUUID(), HumanityConfig.BOUNTY_CLAIM_HUMANITY_GAIN.get());
      NameplateUtil.refresh(player);
    }
    SimpleContainer container = new SimpleContainer(27);
    for (int i = 0; i < Math.min(27, rewards.size()); i++) {
      container.setItem(i, rewards.get(i));
    }
    open(player, "Bounty Contract", (id, inv, viewer) -> new RedemptionMenu(id, inv, container));
  }

  private static void open(ServerPlayer player, String title, MenuFactory factory) {
    player.openMenu(new SimpleMenuProvider(factory::create, Component.literal(title)));
  }

  private static boolean payPlacementCost(ServerPlayer player) {
    long cost = placementCostForConfiguredCurrency();
    return cost <= 0L || CurrencyManager.withdrawPlayer(player, cost);
  }

  private static long placementCostForConfiguredCurrency() {
    String type = ClanCombatConfig.PLAYER_CURRENCY_TYPE.get().trim().toLowerCase();
    if ("item".equals(type)) {
      return BountyConfig.PLACE_CURRENCY_COST.get();
    }
    return BountyConfig.PLACE_XP_COST.get();
  }

  private static boolean payContributionCost(ServerPlayer player) {
    long currency = BountyConfig.CONTRIBUTE_CURRENCY_COST.get();
    int xp = BountyConfig.CONTRIBUTE_XP_COST.get();
    if (currency > 0L && !CurrencyManager.hasPlayerFunds(player, currency)) return false;
    if (xp > 0 && player.experienceLevel < xp) return false;
    if (currency > 0L && !CurrencyManager.withdrawPlayer(player, currency)) return false;
    if (xp > 0) player.giveExperienceLevels(-xp);
    return true;
  }

  private static boolean payClaimCost(ServerPlayer player) {
    long currency = BountyConfig.CLAIM_CURRENCY_COST.get();
    int xp = BountyConfig.CLAIM_XP_COST.get();
    if (currency > 0L && !CurrencyManager.hasPlayerFunds(player, currency)) return false;
    if (xp > 0 && player.experienceLevel < xp) return false;
    if (currency > 0L && !CurrencyManager.withdrawPlayer(player, currency)) return false;
    if (xp > 0) player.giveExperienceLevels(-xp);
    return true;
  }

  private static ItemStack playerHead(ServerPlayer target) {
    ItemStack head = Items.PLAYER_HEAD.getDefaultInstance();
    head.setHoverName(Component.literal(target.getGameProfile().getName()).withStyle(ChatFormatting.GOLD));
    head.getOrCreateTag().putString("SkullOwner", target.getGameProfile().getName());
    return head;
  }

  private static ItemStack button(ItemStack stack, String name, String... lore) {
    return button(stack, name, List.of(lore));
  }

  private static ItemStack button(ItemStack stack, String name, List<String> lore) {
    stack.setHoverName(Component.literal(name).withStyle(ChatFormatting.GOLD));
    setLore(stack, lore);
    return stack;
  }

  private static void addPager(SimpleContainer container, int page, int total, String backLabel) {
    container.setItem(45, button(Items.ARROW.getDefaultInstance(), backLabel));
    if (page > 0) container.setItem(48, button(Items.ARROW.getDefaultInstance(), "Previous Page"));
    container.setItem(49, button(Items.PAPER.getDefaultInstance(), "Page " + (page + 1), total + " bounties"));
    if ((page + 1) * 45 < total) container.setItem(50, button(Items.ARROW.getDefaultInstance(), "Next Page"));
  }

  private static List<String> bountyLore(ServerPlayer viewer, Bounty bounty, long now) {
    List<String> lore = new ArrayList<>();
    lore.add("Target: " + bounty.targetName());
    lore.add("Humanity: " + humanityRank(viewer.server, bounty.targetId()));
    lore.add("Rewards:");
    for (Bounty.RewardItem reward : bounty.rewards()) {
      lore.add(reward.stack().getCount() + "x " + reward.stack().getHoverName().getString());
    }
    lore.add("Time Remaining: " + formatRemaining(Math.max(0L, bounty.expiresAt() - now)));
    return lore;
  }

  private static String humanityRank(net.minecraft.server.MinecraftServer server, UUID playerId) {
    if (!HumanityConfig.ENABLE_HUMANITY.get()) {
      return "Neutral";
    }
    double min = HumanityConfig.MIN_HUMANITY.get();
    double max = HumanityConfig.MAX_HUMANITY.get();
    double value = HumanityData.get(server).humanity(playerId);
    if (Math.abs(value) < 0.0001D) {
      return "Neutral";
    }
    if (value <= 0.0D) {
      int tier = Math.max(1, Math.min(5, (int)Math.ceil((0.0D - value) / Math.max(1.0D, (0.0D - min) / 5.0D))));
      return "Bandit " + roman(tier);
    }
    int tier = Math.max(1, Math.min(5, (int)Math.ceil(value / Math.max(1.0D, max / 5.0D))));
    return "Hero " + roman(tier);
  }

  private static String roman(int tier) {
    return switch (tier) {
      case 1 -> "I";
      case 2 -> "II";
      case 3 -> "III";
      case 4 -> "IV";
      default -> "V";
    };
  }

  private static String formatRemaining(long millis) {
    long minutes = millis / 60000L;
    long hours = minutes / 60L;
    minutes %= 60L;
    return hours + "h " + minutes + "m";
  }

  private static void appendLore(ItemStack stack, List<String> extraLore) {
    List<String> lore = new ArrayList<>(extraLore);
    setLore(stack, lore);
  }

  private static void setLore(ItemStack stack, List<String> lore) {
    ListTag loreTag = new ListTag();
    for (String line : lore) {
      loreTag.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line).withStyle(ChatFormatting.GRAY))));
    }
    stack.getOrCreateTagElement("display").put("Lore", loreTag);
  }

  private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
    if (!player.getInventory().add(stack)) {
      player.drop(stack, false);
    }
  }

  private interface MenuFactory {
    AbstractContainerMenu create(int id, Inventory inventory, Player player);
  }

  private static class ActionMenu extends BaseMenu {
    private final Consumer<Integer> action;

    ActionMenu(MenuType<?> type, int id, Inventory inventory, Container container, int rows, Consumer<Integer> action) {
      super(type, id, inventory, container, rows, LockedSlot::new);
      this.action = action;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
      if (slotId >= 0 && slotId < topSlots && clickType == ClickType.PICKUP) {
        action.accept(slotId);
      }
    }
  }

  private static class RewardEditMenu extends BaseMenu {
    private final int lockedRewards;
    private final UUID targetId;
    private final boolean creating;
    private final String targetName;
    private boolean complete;

    RewardEditMenu(int id, Inventory inventory, Container container, int rows, int lockedRewards, UUID targetId, boolean creating, String targetName) {
      super(MenuType.GENERIC_9x3, id, inventory, container, rows, (slotContainer, slot, x, y) -> {
        if (slot < 9 && slot >= lockedRewards) {
          return new InputSlot(slotContainer, slot, x, y);
        }
        return new LockedSlot(slotContainer, slot, x, y);
      });
      this.lockedRewards = lockedRewards;
      this.targetId = targetId;
      this.creating = creating;
      this.targetName = targetName;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
      if (clickType == ClickType.QUICK_CRAFT || clickType == ClickType.PICKUP_ALL || clickType == ClickType.SWAP || clickType == ClickType.CLONE) {
        return;
      }
      if (slotId == 11 && clickType == ClickType.PICKUP) {
        confirm((ServerPlayer) player);
        return;
      }
      if ((slotId == 13 || slotId == 15) && clickType == ClickType.PICKUP) {
        complete = true;
        returnStaged((ServerPlayer) player);
        if (slotId == 15) {
          if (creating) openPlayerSelection((ServerPlayer) player, false, 0);
          else openPlayerSelection((ServerPlayer) player, true, 0);
        } else {
          player.closeContainer();
        }
        return;
      }
      if (slotId >= 9 && slotId < topSlots) {
        return;
      }
      super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
      if (index < topSlots) {
        return super.quickMoveStack(player, index);
      }
      Slot slot = slots.get(index);
      if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
      ItemStack original = slot.getItem().copy();
      ItemStack moving = slot.getItem();
      if (!moveItemStackTo(moving, lockedRewards, 9, false)) {
        return ItemStack.EMPTY;
      }
      if (moving.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
      else slot.setChanged();
      return original;
    }

    @Override
    public void removed(Player player) {
      super.removed(player);
      if (!complete && player instanceof ServerPlayer serverPlayer) {
        returnStaged(serverPlayer);
      }
    }

    private void confirm(ServerPlayer player) {
      List<ItemStack> staged = stagedRewards();
      if (staged.isEmpty()) {
        player.sendSystemMessage(Component.literal("Add at least one reward item."));
        return;
      }
      long now = System.currentTimeMillis();
      BountyData data = BountyData.get(player.server);
      data.expireIfNeeded(targetId, now, player.server);
      if (creating) {
        ServerPlayer target = player.server.getPlayerList().getPlayer(targetId);
        if (target == null || player.getUUID().equals(targetId)) {
          player.sendSystemMessage(Component.literal("That target is not eligible for a bounty."));
          return;
        }
        if (data.hasActiveBounty(targetId, now)) {
          player.sendSystemMessage(Component.literal("That target already has an active bounty."));
          return;
        }
        if (data.isCoolingDown(targetId, now)) {
          player.sendSystemMessage(Component.literal("That target cannot be bountied yet."));
          return;
        }
        if (!BountyLogic.relationshipAllowsBounty(player.server, player.getUUID(), targetId) || !BountyLogic.isEligibleTarget(player.server, targetId, now)) {
          player.sendSystemMessage(Component.literal("That target is not eligible for a bounty."));
          return;
        }
        if (!payPlacementCost(player)) {
          player.sendSystemMessage(Component.literal("You do not have enough currency."));
          return;
        }
        clearStagedSlots();
        complete = true;
        data.place(player.server, targetId, targetName, player.getUUID(), staged, now);
        target.sendSystemMessage(Component.literal("A bounty has been placed on you."));
        player.closeContainer();
      } else {
        Bounty bounty = data.bounty(targetId).orElse(null);
        if (bounty == null) {
          player.sendSystemMessage(Component.literal("That bounty is no longer active."));
          return;
        }
        if (!bounty.hasRewardSpace(staged.size())) {
          player.sendSystemMessage(Component.literal("That bounty board is full."));
          return;
        }
        if (!BountyLogic.relationshipAllowsBounty(player.server, player.getUUID(), targetId)) {
          player.sendSystemMessage(Component.literal("You cannot contribute to a bounty on a clan member or ally."));
          return;
        }
        if (!payContributionCost(player)) {
          player.sendSystemMessage(Component.literal("You do not have enough currency."));
          return;
        }
        clearStagedSlots();
        complete = true;
        data.contribute(bounty, player.getUUID(), staged);
        player.closeContainer();
      }
    }

    private List<ItemStack> stagedRewards() {
      List<ItemStack> rewards = new ArrayList<>();
      for (int i = lockedRewards; i < 9; i++) {
        ItemStack stack = container.getItem(i);
        if (!stack.isEmpty()) rewards.add(stack.copy());
      }
      return rewards;
    }

    private void clearStagedSlots() {
      for (int i = lockedRewards; i < 9; i++) {
        container.setItem(i, ItemStack.EMPTY);
      }
    }

    private void returnStaged(ServerPlayer player) {
      for (ItemStack stack : stagedRewards()) {
        giveOrDrop(player, stack);
      }
      clearStagedSlots();
    }
  }

  private static class RedemptionMenu extends BaseMenu {
    RedemptionMenu(int id, Inventory inventory, Container container) {
      super(MenuType.GENERIC_9x3, id, inventory, container, 3, TakeOnlySlot::new);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
      if (clickType == ClickType.QUICK_CRAFT || clickType == ClickType.SWAP || clickType == ClickType.PICKUP_ALL || clickType == ClickType.CLONE) {
        return;
      }
      super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
      if (index >= topSlots) return ItemStack.EMPTY;
      return super.quickMoveStack(player, index);
    }

    @Override
    public void removed(Player player) {
      super.removed(player);
      if (player instanceof ServerPlayer serverPlayer) {
        for (int i = 0; i < topSlots; i++) {
          ItemStack stack = container.removeItemNoUpdate(i);
          if (!stack.isEmpty()) serverPlayer.drop(stack, false);
        }
      }
    }
  }

  private abstract static class BaseMenu extends AbstractContainerMenu {
    protected final Container container;
    protected final int topSlots;

    BaseMenu(MenuType<?> type, int id, Inventory inventory, Container container, int rows, TopSlotFactory slotFactory) {
      super(type, id);
      this.container = container;
      this.topSlots = rows * 9;
      container.startOpen(inventory.player);
      int offset = (rows - 4) * 18;
      for (int row = 0; row < rows; row++) {
        for (int column = 0; column < 9; column++) {
          addSlot(slotFactory.create(container, column + row * 9, 8 + column * 18, 18 + row * 18));
        }
      }
      for (int row = 0; row < 3; row++) {
        for (int column = 0; column < 9; column++) {
          addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 103 + row * 18 + offset));
        }
      }
      for (int column = 0; column < 9; column++) {
        addSlot(new Slot(inventory, column, 8 + column * 18, 161 + offset));
      }
    }

    @Override
    public boolean stillValid(Player player) {
      return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
      if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
      Slot slot = slots.get(index);
      if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
      ItemStack original = slot.getItem().copy();
      ItemStack moving = slot.getItem();
      if (index < topSlots) {
        if (!moveItemStackTo(moving, topSlots, slots.size(), true)) return ItemStack.EMPTY;
      } else {
        return ItemStack.EMPTY;
      }
      if (moving.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
      else slot.setChanged();
      return original;
    }

    @Override
    public void removed(Player player) {
      super.removed(player);
      container.stopOpen(player);
    }
  }

  private interface TopSlotFactory {
    Slot create(Container container, int slot, int x, int y);
  }

  private static class LockedSlot extends Slot {
    LockedSlot(Container container, int slot, int x, int y) {
      super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
      return false;
    }

    @Override
    public boolean mayPickup(Player player) {
      return false;
    }
  }

  private static class InputSlot extends Slot {
    InputSlot(Container container, int slot, int x, int y) {
      super(container, slot, x, y);
    }
  }

  private static class TakeOnlySlot extends Slot {
    TakeOnlySlot(Container container, int slot, int x, int y) {
      super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
      return false;
    }
  }
}
