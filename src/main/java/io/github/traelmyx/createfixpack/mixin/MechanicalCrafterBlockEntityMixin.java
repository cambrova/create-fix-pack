package io.github.traelmyx.createfixpack.mixin;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import io.github.traelmyx.createfixpack.CreateFixPack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Create #4510. A crafter whose target inventory is full keeps its result and retries. On load it retries right away, in
 * its first tick, before it has looked up the inventory in front of it. With no inventory found it assumes the target is
 * gone and throws the result on the ground. Belts and depots are checked directly, which is why they are not affected.
 * Now the crafter looks the inventory up first, and while the target's chunk isn't loaded it keeps waiting.
 */
@Mixin(value = MechanicalCrafterBlockEntity.class, remap = false)
public abstract class MechanicalCrafterBlockEntityMixin {
	@Shadow
	private InvManipulationBehaviour inserting;

	@Shadow
	protected abstract boolean isTargetingBelt();

	@Inject(method = "tryInsert", at = @At("HEAD"), cancellable = true, require = 0)
	private void create_fix_pack$findInventoryFirst(CallbackInfo ci) {
		if (!CreateFixPack.CRAFTER_KEEPS_OUTPUT.on() || inserting.hasInventory() || isTargetingBelt())
			return;
		inserting.findNewCapability();
		if (inserting.hasInventory())
			return;
		BlockEntity self = (BlockEntity) (Object) this;
		BlockPos target = inserting.getTarget().getConnectedPos();
		if (self.getLevel() != null && !self.getLevel().isLoaded(target))
			ci.cancel();
	}
}
