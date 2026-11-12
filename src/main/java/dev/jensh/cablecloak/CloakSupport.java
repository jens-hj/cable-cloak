package dev.jensh.cablecloak;

import com.nightbeam.omnivault.network.block.CableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Which blocks count as cables. The one place to extend when more cable mods are supported. */
public final class CloakSupport {
	private CloakSupport() {
	}

	public static boolean isCloakable(Block block) {
		return block instanceof CableBlock;
	}

	public static boolean isCloakable(BlockState state) {
		return isCloakable(state.getBlock());
	}

	public static @Nullable CloakBlockEntity cloakAt(BlockGetter level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof CloakBlockEntity cloak ? cloak : null;
	}
}
