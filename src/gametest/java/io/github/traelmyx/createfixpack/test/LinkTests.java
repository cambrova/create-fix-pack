package io.github.traelmyx.createfixpack.test;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import io.github.traelmyx.createfixpack.CreateFixPack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Create #5009: a transmitter in the test area and a receiver in a chunk 8 chunks away. That chunk is force-loaded, and a
 * second forced chunk 3 chunks further keeps it in memory at the edge of loaded terrain when its own forcing is taken
 * away: not fully loaded, but the receiver's block entity stays. The transmitter switches on while the receiver's chunk
 * is in that state, then the chunk is fully loaded again and the transmitter switches off and on.
 */
@GameTestHolder(CrafterTests.MOD_ID)
@PrefixGameTestTemplate(false)
public class LinkTests {
	private static final BlockPos TRANSMITTER = new BlockPos(2, 1, 2), POWER = new BlockPos(1, 1, 2);

	private static void run(GameTestHelper helper, boolean fixed, int chunkOffset, Check check) {
		ServerLevel level = helper.getLevel();
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		helper.setBlock(TRANSMITTER, AllBlocks.REDSTONE_LINK.getDefaultState().setValue(BlockStateProperties.FACING, Direction.UP));

		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		ChunkPos far = new ChunkPos((origin.getX() >> 4) + chunkOffset, (origin.getZ() >> 4) - 8);
		ChunkPos keeper = new ChunkPos(far.x + 3, far.z);
		BlockPos receiver = new BlockPos(far.getMiddleBlockX(), origin.getY() + 1, far.getMiddleBlockZ());
		level.setChunkForced(far.x, far.z, true);
		level.setChunkForced(keeper.x, keeper.z, true);
		level.setBlockAndUpdate(receiver.below(), Blocks.STONE.defaultBlockState());
		level.setBlockAndUpdate(receiver, AllBlocks.REDSTONE_LINK.getDefaultState().setValue(BlockStateProperties.FACING, Direction.UP)
			.setValue(RedstoneLinkBlock.RECEIVER, true));
		RedstoneLinkBlockEntity[] be = {null};
		boolean[] wasHalfLoaded = {false};

		helper.runAtTickTime(10, () -> {
			be[0] = (RedstoneLinkBlockEntity) level.getBlockEntity(receiver);
			CreateFixPack.LINK_KEEPS_WAITING_LINKS.enabled = fixed;
			level.setChunkForced(far.x, far.z, false);
		});
		helper.runAtTickTime(14, () -> {
			wasHalfLoaded[0] = !level.isLoaded(receiver) && !be[0].isRemoved();
			// the transmitter switches on while the receiver's chunk is half loaded
			helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
		});
		helper.runAtTickTime(16, () -> level.setChunkForced(far.x, far.z, true));
		helper.runAtTickTime(24, () -> helper.setBlock(POWER, Blocks.AIR));
		helper.runAtTickTime(30, () -> helper.setBlock(POWER, Blocks.REDSTONE_BLOCK));
		helper.runAtTickTime(40, () -> {
			boolean same = level.getBlockEntity(receiver) == be[0];
			int signal = be[0].getReceivedSignal();
			CreateFixPack.LINK_KEEPS_WAITING_LINKS.enabled = true;
			// links without frequency items all share one frequency: clear both so the other test can't hear them
			level.setBlockAndUpdate(receiver, Blocks.AIR.defaultBlockState());
			helper.setBlock(POWER, Blocks.AIR);
			helper.setBlock(TRANSMITTER, Blocks.AIR);
			level.setChunkForced(far.x, far.z, false);
			level.setChunkForced(keeper.x, keeper.z, false);
			System.out.printf("[create_fix_pack] link, fix %s: chunk was half loaded %s, same receiver after %s, receiver signal %d (transmitter 15)%n",
				fixed ? "on" : "off", wasHalfLoaded[0], same, signal);
			helper.assertTrue(wasHalfLoaded[0], "the receiver's chunk never was half loaded, so the test proves nothing");
			helper.assertTrue(same, "the receiver was reloaded instead of kept");
			check.check(signal);
			helper.succeed();
		});
	}

	private interface Check {
		void check(int signal);
	}

	@GameTest(template = "empty", timeoutTicks = 80, batch = "link_fixed")
	public static void receiverFollowsAfterHalfLoad(GameTestHelper helper) {
		run(helper, true, 0, signal -> helper.assertTrue(signal == 15, "the receiver reads " + signal + ", the transmitter sends 15"));
	}

	@GameTest(template = "empty", timeoutTicks = 80, batch = "link_control")
	public static void linkControlReproducesBug(GameTestHelper helper) {
		// without the fix the receiver must have lost its frequency, or this test can't tell the fix works
		run(helper, false, 1, signal -> helper.assertTrue(signal == 0, "the receiver still follows without the fix (" + signal + "): the bug did not reproduce"));
	}
}
