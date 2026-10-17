package io.github.traelmyx.createfixpack.test;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import io.github.traelmyx.createfixpack.CreateFixPack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Create #4510: a creative motor turns a mechanical crafter that turns an oak log into planks and puts them into a barrel
 * above it. The barrel is full, so the crafter holds the planks and waits. Then the crafter is unloaded and loaded again
 * from its saved data, the way a chunk reload does.
 */
@GameTestHolder(CrafterTests.MOD_ID)
@PrefixGameTestTemplate(false)
public class CrafterTests {
	public static final String MOD_ID = "create_fix_pack_gametest";

	private static final BlockPos MOTOR = new BlockPos(1, 1, 3), COG = new BlockPos(1, 1, 2), CRAFTER = new BlockPos(2, 1, 2), BARREL = new BlockPos(2, 2, 2);

	private static void build(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		// crafters turn like cogwheels: the motor drives a cogwheel beside the crafter
		helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(BlockStateProperties.FACING, Direction.NORTH));
		helper.setBlock(COG, AllBlocks.COGWHEEL.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Z));
		helper.setBlock(CRAFTER, AllBlocks.MECHANICAL_CRAFTER.getDefaultState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
		helper.setBlock(BARREL, Blocks.BARREL);
		Container barrel = (Container) helper.getBlockEntity(BARREL);
		for (int i = 0; i < barrel.getContainerSize(); i++) barrel.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
	}

	private static MechanicalCrafterBlockEntity crafter(GameTestHelper helper) {
		return (MechanicalCrafterBlockEntity) helper.getBlockEntity(CRAFTER);
	}

	private static int droppedPlanks(GameTestHelper helper) {
		BlockPos o = helper.absolutePos(BlockPos.ZERO);
		int n = 0;
		for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(o.getX(), o.getY(), o.getZ(), o.getX() + 8, o.getY() + 8, o.getZ() + 8)))
			if (e.getItem().is(Items.OAK_PLANKS)) n += e.getItem().getCount();
		return n;
	}

	/** Planks held by the crafter, read from what it would save. */
	private static int heldPlanks(GameTestHelper helper) {
		CompoundTag tag = crafter(helper).saveWithoutMetadata(helper.getLevel().registryAccess());
		return tag.toString().contains("oak_planks") ? 1 : 0;
	}

	private static void reloadCrafter(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos abs = helper.absolutePos(CRAFTER);
		BlockEntity old = level.getBlockEntity(abs);
		CompoundTag saved = old.saveWithFullMetadata(level.registryAccess());
		old.onChunkUnloaded();
		level.removeBlockEntity(abs);
		level.setBlockEntity(BlockEntity.loadStatic(abs, level.getBlockState(abs), saved, level.registryAccess()));
	}

	private static void run(GameTestHelper helper, boolean fixed, boolean finish, Check check) {
		CreateFixPack.CRAFTER_KEEPS_OUTPUT.enabled = fixed;
		build(helper);
		helper.runAtTickTime(5, () -> {
			var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(CRAFTER), Direction.SOUTH);
			helper.assertTrue(handler != null, "the crafter has no item input");
			helper.assertTrue(handler.insertItem(0, new ItemStack(Items.OAK_LOG), false).isEmpty(), "the crafter did not take the log");
		});
		int[] waiting = {0};
		helper.runAtTickTime(300, () -> {
			waiting[0] = heldPlanks(helper);
			helper.assertTrue(waiting[0] == 1 && droppedPlanks(helper) == 0, "the crafter is not holding its planks before the reload (held "
				+ waiting[0] + ", dropped " + droppedPlanks(helper) + ")");
			reloadCrafter(helper);
		});
		helper.runAtTickTime(340, () -> {
			int dropped = droppedPlanks(helper), held = heldPlanks(helper);
			CreateFixPack.CRAFTER_KEEPS_OUTPUT.enabled = true;
			System.out.printf("[create_fix_pack] crafter, fix %s: after the reload %d planks on the ground, still holding planks: %s%n",
				fixed ? "on" : "off", dropped, held == 1);
			check.check(dropped, held);
			if (finish) helper.succeed();
		});
	}

	private interface Check {
		void check(int dropped, int held);
	}

	@GameTest(template = "empty", timeoutTicks = 400, batch = "crafter_fixed")
	public static void crafterKeepsOutputOnLoad(GameTestHelper helper) {
		run(helper, true, true, (dropped, held) -> helper.assertTrue(dropped == 0 && held == 1,
			"the crafter dropped " + dropped + " planks after the reload"));
	}

	@GameTest(template = "empty", timeoutTicks = 400, batch = "crafter_control")
	public static void crafterControlReproducesBug(GameTestHelper helper) {
		// without the fix the planks must land on the ground, or this test can't tell the fix works
		run(helper, false, true, (dropped, held) -> helper.assertTrue(dropped > 0, "the crafter kept its planks without the fix: the bug did not reproduce"));
	}

	/** The crafter still delivers once the barrel has room. */
	@GameTest(template = "empty", timeoutTicks = 480, batch = "crafter_delivers")
	public static void crafterDeliversWhenRoomFrees(GameTestHelper helper) {
		run(helper, true, false, (dropped, held) -> {
			Container barrel = (Container) helper.getBlockEntity(BARREL);
			barrel.setItem(0, ItemStack.EMPTY);
		});
		helper.runAtTickTime(420, () -> {
			Container barrel = (Container) helper.getBlockEntity(BARREL);
			helper.assertTrue(barrel.getItem(0).is(Items.OAK_PLANKS), "the crafter did not deliver its planks when the barrel had room: " + barrel.getItem(0));
			helper.succeed();
		});
	}
}
