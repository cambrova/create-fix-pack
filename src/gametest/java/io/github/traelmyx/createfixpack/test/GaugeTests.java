package io.github.traelmyx.createfixpack.test;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.RequestPromise;
import com.simibubi.create.content.logistics.packagerLink.RequestPromiseQueue;
import io.github.traelmyx.createfixpack.CreateFixPack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Create #9987: a packager with a barrel behind it and a stock link on top. The link's network has promised 64 iron
 * ingots, which a factory gauge waits for. The barrel holds 10 ingots; then they are taken out of their slot and 16
 * different ingots arrive in another slot. Only 6 ingots are new, so 58 must stay promised.
 */
@GameTestHolder(CrafterTests.MOD_ID)
@PrefixGameTestTemplate(false)
public class GaugeTests {
	private static final BlockPos PACKAGER = new BlockPos(2, 1, 2), BARREL = new BlockPos(2, 1, 3), LINK = new BlockPos(2, 2, 2);

	private static void build(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		helper.setBlock(BARREL, Blocks.BARREL);
		helper.setBlock(PACKAGER, AllBlocks.PACKAGER.getDefaultState().setValue(BlockStateProperties.FACING, Direction.NORTH));
		helper.setBlock(LINK, AllBlocks.STOCK_LINK.getDefaultState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
	}

	private static void run(GameTestHelper helper, boolean fixed, Check check) {
		build(helper);
		RequestPromiseQueue[] queue = {null};
		helper.runAtTickTime(5, () -> {
			PackagerLinkBlockEntity link = helper.getBlockEntity(LINK);
			queue[0] = Create.LOGISTICS.getQueuedPromises(link.behaviour.freqId);
			helper.assertTrue(queue[0] != null, "the stock link has no network");
			CreateFixPack.GAUGE_NO_OVER_REQUEST.enabled = fixed;
			PackagerBlockEntity packager = helper.getBlockEntity(PACKAGER);
			// change the barrel through the packager's own view of it, as other blocks and players do
			var inventory = packager.targetInventory.getInventory();
			helper.assertTrue(inventory != null, "the packager does not see the barrel");
			inventory.insertItem(0, new ItemStack(Items.IRON_INGOT, 10), false);
			packager.triggerStockCheck();
			System.out.println("[create_fix_pack] gauge debug: summary after 10 in: " + packager.getAvailableItems().getCountOf(new ItemStack(Items.IRON_INGOT)) + " inv " + inventory.getClass().getName());
			queue[0].add(new RequestPromise(new BigItemStack(new ItemStack(Items.IRON_INGOT), 64)));
			// the 10 ingots are taken out of their slot, and 16 others arrive in the next one
			inventory.extractItem(0, 10, false);
			inventory.insertItem(1, new ItemStack(Items.IRON_INGOT, 16), false);
			packager.triggerStockCheck();
			System.out.println("[create_fix_pack] gauge debug: summary after: " + packager.getAvailableItems().getCountOf(new ItemStack(Items.IRON_INGOT)));
			int promised = queue[0].getTotalPromisedAndRemoveExpired(new ItemStack(Items.IRON_INGOT), -1);
			CreateFixPack.GAUGE_NO_OVER_REQUEST.enabled = true;
			queue[0].forceClear(new ItemStack(Items.IRON_INGOT));
			System.out.printf("[create_fix_pack] gauge, fix %s: 6 ingots arrived, %d of 64 still promised (58 is right)%n", fixed ? "on" : "off", promised);
			check.check(promised);
			helper.succeed();
		});
	}

	private interface Check {
		void check(int promised);
	}

	@GameTest(template = "empty", timeoutTicks = 100, batch = "gauge_fixed")
	public static void gaugeCountsOnlyNewArrivals(GameTestHelper helper) {
		run(helper, true, promised -> helper.assertTrue(promised == 58, promised + " ingots still promised, expected 58"));
	}

	@GameTest(template = "empty", timeoutTicks = 100, batch = "gauge_control")
	public static void gaugeControlReproducesBug(GameTestHelper helper) {
		// without the fix too many promises must be cleared, or this test can't tell the fix works
		run(helper, false, promised -> helper.assertTrue(promised < 58, "promises were counted right without the fix (" + promised + "): the bug did not reproduce"));
	}
}
