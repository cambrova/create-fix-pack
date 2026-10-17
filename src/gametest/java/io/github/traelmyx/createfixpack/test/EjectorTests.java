package io.github.traelmyx.createfixpack.test;

import com.simibubi.create.AllBlocks;
import io.github.traelmyx.createfixpack.CreateFixPack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Create #7681: a charged, empty weighted ejector is offered 16 iron ingots at once, the way a brass funnel set to 16
 * offers them. It has to take all 16.
 */
@GameTestHolder(CrafterTests.MOD_ID)
@PrefixGameTestTemplate(false)
public class EjectorTests {
	private static final BlockPos MOTOR = new BlockPos(2, 1, 3), EJECTOR = new BlockPos(3, 1, 3);

	private static void run(GameTestHelper helper, boolean fixed, Check check) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(BlockStateProperties.FACING, Direction.EAST));
		helper.setBlock(EJECTOR, AllBlocks.WEIGHTED_EJECTOR.getDefaultState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
		helper.runAtTickTime(60, () -> {
			var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(EJECTOR), Direction.UP);
			helper.assertTrue(handler != null, "the ejector has no item input");
			// charged? one item must fit either way
			helper.assertTrue(handler.insertItem(0, new ItemStack(Items.IRON_INGOT), true).isEmpty(), "the ejector is not ready to take items");
			CreateFixPack.EJECTOR_TAKES_STACKS.enabled = fixed;
			int refused = handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 16), true).getCount();
			CreateFixPack.EJECTOR_TAKES_STACKS.enabled = true;
			System.out.printf("[create_fix_pack] ejector, fix %s: offered 16 ingots, refused %d%n", fixed ? "on" : "off", refused);
			check.check(refused);
			helper.succeed();
		});
	}

	private interface Check {
		void check(int refused);
	}

	@GameTest(template = "empty", timeoutTicks = 100, batch = "ejector_fixed")
	public static void ejectorTakesAStack(GameTestHelper helper) {
		run(helper, true, refused -> helper.assertTrue(refused == 0, "the ejector refused " + refused + " of 16"));
	}

	@GameTest(template = "empty", timeoutTicks = 100, batch = "ejector_control")
	public static void ejectorControlReproducesBug(GameTestHelper helper) {
		run(helper, false, refused -> helper.assertTrue(refused > 0, "the ejector took all 16 without the fix: the bug did not reproduce"));
	}
}
