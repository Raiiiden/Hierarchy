package com.raiiiden.hierarchy.clan.currency;

import com.raiiiden.hierarchy.clan.config.ClanCombatConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemCurrencyProvider implements CurrencyProvider {
  @Override
  public boolean hasFunds(ServerPlayer player, long amount) {
    if (amount <= 0L) {
      return true;
    }
    Item item = currencyItem();
    if (item == Items.AIR) {
      return false;
    }
    return count(player, item) >= amount;
  }

  @Override
  public boolean withdraw(ServerPlayer player, long amount) {
    if (!hasFunds(player, amount)) {
      return false;
    }
    Item item = currencyItem();
    long remaining = amount;
    for (ItemStack stack : player.getInventory().items) {
      remaining = shrink(stack, item, remaining);
      if (remaining <= 0L) {
        return true;
      }
    }
    shrink(player.getOffhandItem(), item, remaining);
    return true;
  }

  @Override
  public boolean deposit(ServerPlayer player, long amount) {
    if (amount <= 0L) {
      return true;
    }
    Item item = currencyItem();
    if (item == Items.AIR || !hasSpace(player, item, amount)) {
      return false;
    }
    int maxStackSize = item.getDefaultInstance().getMaxStackSize();
    long remaining = amount;
    while (remaining > 0L) {
      int count = (int) Math.min(maxStackSize, remaining);
      ItemStack stack = new ItemStack(item, count);
      if (!player.getInventory().add(stack)) {
        return false;
      }
      remaining -= count;
    }
    return true;
  }

  private Item currencyItem() {
    ResourceLocation id = ResourceLocation.tryParse(ClanCombatConfig.PLAYER_CURRENCY_ITEM.get());
    if (id == null) {
      return Items.AIR;
    }
    Item item = ForgeRegistries.ITEMS.getValue(id);
    return item == null ? Items.AIR : item;
  }

  private long count(ServerPlayer player, Item item) {
    long total = 0L;
    for (ItemStack stack : player.getInventory().items) {
      if (stack.is(item)) {
        total += stack.getCount();
      }
    }
    ItemStack offhand = player.getOffhandItem();
    if (offhand.is(item)) {
      total += offhand.getCount();
    }
    return total;
  }

  private boolean hasSpace(ServerPlayer player, Item item, long amount) {
    long available = 0L;
    int maxStackSize = item.getDefaultInstance().getMaxStackSize();
    for (ItemStack stack : player.getInventory().items) {
      if (stack.isEmpty()) {
        available += maxStackSize;
      } else if (stack.is(item)) {
        available += maxStackSize - stack.getCount();
      }
      if (available >= amount) {
        return true;
      }
    }
    return false;
  }

  private long shrink(ItemStack stack, Item item, long remaining) {
    if (remaining <= 0L || !stack.is(item)) {
      return remaining;
    }
    int removed = (int) Math.min(stack.getCount(), remaining);
    stack.shrink(removed);
    return remaining - removed;
  }
}
