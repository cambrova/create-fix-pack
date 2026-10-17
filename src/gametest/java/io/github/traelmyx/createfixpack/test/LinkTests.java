package io.github.traelmyx.createfixpack.test;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import io.github.traelmyx.createfixpack.CreateFixPack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The experimental #5009 switch (links in half-loaded chunks keep their frequency) must not change how links normally
 * behave: with it on, a receiver still follows its transmitter, and a receiver that is broken still leaves the frequency.
 * The bug itself is not reproduced by a test yet, which is why the switch is off by default.
 */
@GameTestHolder(CrafterTests.MOD_ID)
@PrefixGameTestTemplate(false)
public class LinkTests {
	private static final BlockPos TRANSMITTER = new BlockPos(2, 1, 2), POWER = new BlockPos(1, 1, 2), RECEIVER = new BlockPos(5, 1, 5);

	@GameTest(template = "empty", timeoutTicks = 100, batch = "link_regression")
	public static void linksWorkNormallyWithExperimentalSwitchOn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CreateFixPack.LINK_KEEPS_WAITING_LINKS.enabled = true;
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		helper.setBlock(TRANSMITTER, AllBlocks.REDSTONE_LINK.getDefaultState().setValue(BlockStateProperties.FACING, Direction.UP));
		helper.setBlock(RECEIVER, AllBlocks.REDSTONE_LINK.getDefaultState().setValue(BlockStateProperties.FACING, Direction.UP)
			.setValue(RedstoneLinkBlock.RECEIVER, true));
		BlockPos receiver = helper.absolutePos(RECEIVER);
		// a link placed without saved data creates its frequency part only when it reads data, so load it from its own save
		BlockEntity placed = level.getBlockEntity(receiver);
		var tag = placed.saveWithFullMetadata(level.registryAccess());
		level.removeBlockEntity(receiver);
		level.setBlockEntity(BlockEntity.loadStatic(receiver, level.getBlockState(receiver), tag, level.registryAccess()));

		int[] seen = new int[3];
		helper.runAtTickTime(10, () -> helper.setBlock(POWER, Blocks.REDSTONE_BLOCK));
		helper.runAtTickTime(16, () -> {
			seen[0] = ((RedstoneLinkBlockEntity) level.getBlockEntity(receiver)).getReceivedSignal();
			helper.setBlock(POWER, Blocks.AIR);
		});
		helper.runAtTickTime(22, () -> {
			seen[1] = ((RedstoneLinkBlockEntity) level.getBlockEntity(receiver)).getReceivedSignal();
			helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
		});
		LinkBehaviour[] gone = {null};
		helper.runAtTickTime(28, () -> {
			seen[2] = ((RedstoneLinkBlockEntity) level.getBlockEntity(receiver)).getReceivedSignal();
			gone[0] = BlockEntityBehaviour.get(level, receiver, LinkBehaviour.TYPE);
			helper.setBlock(RECEIVER, Blocks.AIR);
		});
		helper.runAtTickTime(30, () -> helper.setBlock(POWER, Blocks.AIR));
		helper.runAtTickTime(34, () -> {
			boolean stillListed = gone[0] != null && Create.REDSTONE_LINK_NETWORK_HANDLER.getNetworkOf(level, gone[0]).contains(gone[0]);
			helper.setBlock(TRANSMITTER, Blocks.AIR);
			CreateFixPack.LINK_KEEPS_WAITING_LINKS.enabled = false;
			System.out.printf("[create_fix_pack] link switch on: receiver read %d / %d / %d (expected 15 / 0 / 15), broken receiver still on the frequency: %s%n",
				seen[0], seen[1], seen[2], stillListed);
			helper.assertTrue(seen[0] == 15 && seen[1] == 0 && seen[2] == 15, "the receiver read " + seen[0] + " / " + seen[1] + " / " + seen[2] + ", expected 15 / 0 / 15");
			helper.assertTrue(!stillListed, "a broken receiver stayed on the frequency");
			helper.succeed();
		});
	}
}
