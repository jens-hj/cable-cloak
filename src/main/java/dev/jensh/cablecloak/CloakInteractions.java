package dev.jensh.cablecloak;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Prediction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Right-click handling on framed cables: apply a look, or sneak with an empty hand to take things off. */
public final class CloakInteractions {
	/**
	 * Client-side check that a block's model has geometry to copy. Chests, signs and the like have an empty model
	 * and are drawn by a renderer instead; the server cannot tell, but a refused click never reaches it.
	 */
	public static Predicate<BlockState> hasModel = state -> true;

	private CloakInteractions() {
	}

	public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (player.isSpectator()) {
			return InteractionResult.PASS;
		}
		BlockPos pos = hit.getBlockPos();
		if (!CloakSupport.isCloakable(level.getBlockState(pos))) {
			return InteractionResult.PASS;
		}
		CloakBlockEntity cloak = CloakSupport.cloakAt(level, pos);
		if (cloak == null || !cloak.hasFrame()) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (stack.isEmpty()) {
			return hand == InteractionHand.MAIN_HAND && player.isSecondaryUseActive()
					? takeOff(player, level, pos, cloak)
					: InteractionResult.PASS;
		}
		// Sneaking with a block places it against the facade as usual.
		if (player.isSecondaryUseActive() || !(stack.getItem() instanceof BlockItem blockItem)) {
			return InteractionResult.PASS;
		}
		BlockState look = lookFor(blockItem.getBlock(), player, hand, stack, hit);
		if (look != null && level.isClientSide() && !hasModel.test(look)) {
			look = null;
		}
		if (look == null) {
			if (level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("message.cable_cloak.unsupported", stack.getHoverName()));
			}
			return InteractionResult.FAIL;
		}
		if (look.equals(cloak.facade())) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			giveBack(player, cloak.facadeItem());
			boolean creative = player.getAbilities().instabuild;
			cloak.setFacade(look, creative ? ItemStack.EMPTY : stack);
			if (!creative) {
				stack.shrink(1);
			}
			SoundType sound = look.getSoundType();
			level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0f) / 2.0f, sound.getPitch() * 0.8f);
		}
		return InteractionResult.SUCCESS;
	}

	/** The state a facade of {@code block} shows, oriented as if placed; null if it cannot be shown as a facade. */
	static @Nullable BlockState lookFor(Block block, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
		if (CloakSupport.isCloakable(block)) {
			return null;
		}
		BlockState state;
		try {
			state = block.getStateForPlacement(new BlockPlaceContext(player, hand, stack, hit));
		} catch (RuntimeException e) {
			state = null;
		}
		if (state == null || !state.is(block)) {
			state = block.defaultBlockState();
		}
		if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
			state = state.setValue(BlockStateProperties.WATERLOGGED, false);
		}
		return isShowable(state) ? state : null;
	}

	/** Facades draw the block's model, so blocks without one (air, chests drawn by renderers, fluids) are out. */
	public static boolean isShowable(BlockState state) {
		return !state.isAir() && state.getRenderShape() == RenderShape.MODEL && !CloakSupport.isCloakable(state);
	}

	private static InteractionResult takeOff(Player player, Level level, BlockPos pos, CloakBlockEntity cloak) {
		if (!level.isClientSide()) {
			if (cloak.facade() != null) {
				giveBack(player, cloak.facadeItem());
				cloak.setFacade(null, ItemStack.EMPTY);
			} else {
				cloak.setFrame(false);
				if (!player.getAbilities().instabuild) {
					giveBack(player, new ItemStack(CableCloak.CLOAK_FRAME));
				}
			}
			level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
		}
		return InteractionResult.SUCCESS;
	}

	private static void giveBack(Player player, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}
		// Fills the inventory and drops whatever does not fit; the inventory menu syncs the slots.
		player.getInventory().placeItemBackInInventory(stack.copy(), false, Prediction.SERVER_ONLY);
	}
}
