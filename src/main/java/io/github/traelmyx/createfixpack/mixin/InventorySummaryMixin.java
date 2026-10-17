package io.github.traelmyx.createfixpack.mixin;

import com.simibubi.create.content.logistics.packager.InventorySummary;
import io.github.traelmyx.createfixpack.CreateFixPack;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Create #9987. A packager's stock summary used the inventory's own item stacks as its entries. When that stack later
 * shrinks or empties in the inventory, the old summary changes with it, so the next comparison sees items "arrive" that
 * were already there, and clears too many promises. The gauge then orders the same items again. Entries are now copies.
 */
@Mixin(value = InventorySummary.class, remap = false)
public abstract class InventorySummaryMixin {
	@ModifyArg(method = "add(Lnet/minecraft/world/item/ItemStack;I)V", require = 0,
		at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/BigItemStack;<init>(Lnet/minecraft/world/item/ItemStack;I)V"), index = 0)
	private ItemStack create_fix_pack$copyKey(ItemStack stack) {
		return CreateFixPack.GAUGE_NO_OVER_REQUEST.on() ? stack.copy() : stack;
	}
}
