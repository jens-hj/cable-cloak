package dev.jensh.cablecloak.test;

import com.nightbeam.omnivault.network.NetworkContent;
import com.nightbeam.omnivault.network.block.CableBlock;
import dev.jensh.cablecloak.CableCloak;
import dev.jensh.cablecloak.CloakBlockEntity;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public class CloakGameTests {
	private static final BlockPos CABLE = new BlockPos(1, 1, 1);

	private static ServerPlayer player(GameTestHelper helper) {
		return (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
	}

	/** Right-click the cable's top face through the real interaction path (fires UseBlockCallback). */
	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, ItemStack stack) {
		InteractionHand hand = InteractionHand.MAIN_HAND;
		player.setItemInHand(hand, stack);
		BlockPos abs = helper.absolutePos(CABLE);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false);
		// Same order as ServerPlayerGameMode.useItemOn, minus the packets a mock player cannot receive.
		InteractionResult result = UseBlockCallback.EVENT.invoker().interact(player, helper.getLevel(), hand, hit);
		if (result != InteractionResult.PASS) {
			return result;
		}
		BlockState state = helper.getLevel().getBlockState(abs);
		if (!player.isSecondaryUseActive() || stack.isEmpty()) {
			result = state.useItemOn(stack, helper.getLevel(), player, hand, hit);
			if (result.consumesAction()) {
				return result;
			}
			if (result instanceof InteractionResult.TryEmptyHandInteraction) {
				result = state.useWithoutItem(helper.getLevel(), player, hit);
				if (result.consumesAction()) {
					return result;
				}
			}
		}
		return stack.isEmpty() ? InteractionResult.PASS : stack.useOn(new UseOnContext(player, hand, hit));
	}

	private static CloakBlockEntity cloak(GameTestHelper helper) {
		return helper.getBlockEntity(CABLE, CloakBlockEntity.class);
	}

	private static void placeCable(GameTestHelper helper) {
		helper.setBlock(CABLE, NetworkContent.NEXUS_CABLE.get());
	}

	@GameTest
	public void frameThenFacade(GameTestHelper helper) {
		placeCable(helper);
		ServerPlayer player = player(helper);

		ItemStack frames = new ItemStack(CableCloak.CLOAK_FRAME, 2);
		helper.assertTrue(use(helper, player, frames).consumesAction(), "frame should go on the cable");
		helper.assertTrue(cloak(helper).hasFrame(), "cable should have a frame");
		helper.assertValueEqual(frames.getCount(), 1, "frames left");
		helper.assertTrue(helper.getBlockState(CABLE).getShape(helper.getLevel(), helper.absolutePos(CABLE)) == Shapes.block()
				|| helper.getBlockState(CABLE).getCollisionShape(helper.getLevel(), helper.absolutePos(CABLE)).bounds().getSize() == 1.0,
				"a framed cable should be a full block");

		ItemStack stone = new ItemStack(Items.STONE, 2);
		use(helper, player, stone);
		helper.assertValueEqual(cloak(helper).facade(), Blocks.STONE.defaultBlockState(), "facade");
		helper.assertValueEqual(stone.getCount(), 1, "stone left");

		// A new look swaps out the old one and hands the old block back.
		use(helper, player, new ItemStack(Items.OAK_LOG));
		helper.assertTrue(cloak(helper).facade() != null && cloak(helper).facade().is(Blocks.OAK_LOG), "log facade");
		helper.assertValueEqual(cloak(helper).facade().getValue(RotatedPillarBlock.AXIS), Direction.Axis.Y, "log axis from the clicked face");
		helper.assertTrue(player.getInventory().countItem(Items.STONE) == 1, "stone should be handed back");

		// Sneak with an empty hand: first the look comes off, then the frame.
		player.setShiftKeyDown(true);
		use(helper, player, ItemStack.EMPTY);
		helper.assertTrue(cloak(helper).facade() == null && cloak(helper).hasFrame(), "look removed, frame kept");
		helper.assertTrue(player.getInventory().countItem(Items.OAK_LOG) == 1, "log should be handed back");
		use(helper, player, ItemStack.EMPTY);
		helper.assertFalse(cloak(helper).hasFrame(), "frame removed");
		helper.assertTrue(player.getInventory().countItem(CableCloak.CLOAK_FRAME) == 1, "frame should be handed back");
		helper.succeed();
	}

	@GameTest
	public void breakingDropsFrameAndBlock(GameTestHelper helper) {
		placeCable(helper);
		ServerPlayer player = player(helper);
		use(helper, player, new ItemStack(CableCloak.CLOAK_FRAME));
		use(helper, player, new ItemStack(Items.BRICKS));
		helper.getLevel().destroyBlock(helper.absolutePos(CABLE), true);
		helper.assertItemEntityPresent(CableCloak.CLOAK_FRAME);
		helper.assertItemEntityPresent(Items.BRICKS);
		helper.succeed();
	}

	@GameTest
	public void dyeingKeepsCloak(GameTestHelper helper) {
		placeCable(helper);
		ServerPlayer player = player(helper);
		use(helper, player, new ItemStack(CableCloak.CLOAK_FRAME));
		use(helper, player, new ItemStack(Items.GLASS));
		use(helper, player, new ItemStack(Items.DYE.red()));
		helper.assertValueEqual(helper.getBlockState(CABLE).getValue(CableBlock.COLOR), CableBlock.CableColor.of(DyeColor.RED), "dyed");
		helper.assertValueEqual(cloak(helper).facade(), Blocks.GLASS.defaultBlockState(), "facade survives dyeing");
		helper.succeed();
	}

	@GameTest
	public void blocksWithoutModelAreRefused(GameTestHelper helper) {
		placeCable(helper);
		ServerPlayer player = player(helper);
		use(helper, player, new ItemStack(CableCloak.CLOAK_FRAME));
		ItemStack barrier = new ItemStack(Items.BARRIER);
		helper.assertValueEqual(use(helper, player, barrier), InteractionResult.FAIL, "barrier refused");
		helper.assertTrue(cloak(helper).facade() == null, "barriers are invisible, nothing to copy");
		helper.assertValueEqual(barrier.getCount(), 1, "barrier kept");
		ItemStack cable = new ItemStack(NetworkContent.NEXUS_CABLE.get());
		use(helper, player, cable);
		helper.assertTrue(cloak(helper).facade() == null, "cables cannot cloak cables");
		helper.succeed();
	}

	@GameTest
	public void frameOnlyGoesOnCables(GameTestHelper helper) {
		helper.setBlock(CABLE, Blocks.STONE);
		ServerPlayer player = player(helper);
		ItemStack frame = new ItemStack(CableCloak.CLOAK_FRAME);
		use(helper, player, frame);
		helper.assertValueEqual(frame.getCount(), 1, "frame not used on stone");
		helper.succeed();
	}
}
