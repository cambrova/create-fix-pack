package io.github.traelmyx.createfixpack.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import io.github.traelmyx.createfixpack.CreateFixPack;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Create #7681. An empty weighted ejector works out how much it can take from the stack size of the item it holds. With
 * nothing held that is the empty stack, whose stack size is 1 since Minecraft 1.21, so it takes one item at a time and
 * refuses a brass funnel that hands over more. With nothing held, the limit is now a full stack.
 */
@Mixin(value = DepotBehaviour.class, remap = false)
public abstract class DepotBehaviourMixin {
	@WrapOperation(method = "getRemainingSpace", require = 0,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
	private int create_fix_pack$fullStackWhenEmpty(ItemStack held, Operation<Integer> original) {
		if (held.isEmpty() && CreateFixPack.EJECTOR_TAKES_STACKS.on())
			return 64;
		return original.call(held);
	}
}
