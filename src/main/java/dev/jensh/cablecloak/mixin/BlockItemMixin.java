package dev.jensh.cablecloak.mixin;

import dev.jensh.cablecloak.CloakInteractions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets a block placed into a framed cable's spot (by clicking a neighbouring face) dress the cable instead. */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
	private void cableCloak$dressCable(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		InteractionResult result = CloakInteractions.onPlaceAgainst(context, (BlockItem) (Object) this);
		if (result != null) {
			cir.setReturnValue(result);
		}
	}
}
