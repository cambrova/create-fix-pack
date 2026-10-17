package io.github.traelmyx.createfixpack.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import io.github.traelmyx.createfixpack.CreateFixPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Create #5009 and its duplicates. When a link's frequency changes signal, every link on it that isn't "alive" is
 * dropped from the frequency for good. A link in a chunk that is briefly not fully loaded (the chunk is on its way out,
 * or held at the edge of loaded terrain) counts as not alive, although its block entity stays. When the chunk comes
 * back it is the same block entity, so it never joins again: the receiver ignores its transmitter until you replace it
 * or switch it to transmitter and back. Such links now stay on the frequency; only removed ones are dropped.
 */
@Mixin(value = RedstoneLinkNetworkHandler.class, remap = false)
public abstract class RedstoneLinkNetworkHandlerMixin {
	@WrapOperation(method = "updateNetworkOf", require = 0,
		at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;isAlive()Z"))
	private boolean create_fix_pack$keepWaitingLinks(IRedstoneLinkable link, Operation<Boolean> original) {
		if (original.call(link))
			return true;
		return CreateFixPack.LINK_KEEPS_WAITING_LINKS.on() && link instanceof LinkBehaviour behaviour
			&& !behaviour.blockEntity.isRemoved() && !behaviour.blockEntity.isChunkUnloaded();
	}
}
