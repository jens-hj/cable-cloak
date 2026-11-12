package dev.jensh.cablecloak.mixin;

import com.nightbeam.omnivault.network.block.CableBlock;
import dev.jensh.cablecloak.CloakBlockEntity;
import dev.jensh.cablecloak.CloakSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gives OmniVault cables a block entity to hold their cloak, and makes a cloaked cable as solid as its facade. */
@Mixin(CableBlock.class)
public abstract class CableBlockMixin implements EntityBlock {
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CloakBlockEntity(pos, state);
	}

	@Inject(method = "getShape", at = @At("RETURN"), cancellable = true)
	private void cableCloak$facadeShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
		CloakBlockEntity cloak = CloakSupport.cloakAt(level, pos);
		if (cloak != null && cloak.hasFrame()) {
			cir.setReturnValue(cloak.shape(state, cir.getReturnValue()));
		}
	}
}
