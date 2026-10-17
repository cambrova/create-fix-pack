package io.github.traelmyx.createfixpack.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import io.github.traelmyx.createfixpack.CreateFixPack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Create #9987, the two other ways a packager clears more promises than items arrived:
 * <ul>
 * <li>In singleplayer the client copy of the packager also reported arrivals into the shared promise list, so every
 * arrival counted twice. Only the server reports them now.</li>
 * <li>When the packager briefly can't see its inventory (the inventory block was just updated), it remembered an empty
 * stock, and the next look counted everything in the inventory as new. It now forgets the stock instead, so the next look
 * starts fresh.</li>
 * </ul>
 */
@Mixin(value = PackagerBlockEntity.class, remap = false)
public abstract class PackagerBlockEntityMixin {
	@Inject(method = "submitNewArrivals", at = @At("HEAD"), cancellable = true, require = 0)
	private void create_fix_pack$serverOnly(InventorySummary before, InventorySummary after, CallbackInfo ci) {
		BlockEntity self = (BlockEntity) (Object) this;
		if (CreateFixPack.GAUGE_NO_OVER_REQUEST.on() && self.getLevel() != null && self.getLevel().isClientSide())
			ci.cancel();
	}

	@WrapOperation(method = "getAvailableItems", require = 0,
		at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/logistics/packager/PackagerBlockEntity;availableItems:Lcom/simibubi/create/content/logistics/packager/InventorySummary;",
			opcode = Opcodes.PUTFIELD, ordinal = 0))
	private void create_fix_pack$forgetWhileBlind(PackagerBlockEntity self, InventorySummary empty, Operation<Void> original) {
		original.call(self, CreateFixPack.GAUGE_NO_OVER_REQUEST.on() ? null : empty);
	}
}
