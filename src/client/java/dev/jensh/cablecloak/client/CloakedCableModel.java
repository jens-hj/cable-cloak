package dev.jensh.cablecloak.client;

import dev.jensh.cablecloak.CableCloak;
import dev.jensh.cablecloak.CloakBlockEntity.CloakRenderData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Wraps every cable model. A cable wearing a cloak draws the copied block's model in its place (plus the cable
 * itself when the copy does not hide it); a bare frame draws the frame around the cable.
 */
public class CloakedCableModel extends WrapperBlockStateModel {
	public CloakedCableModel(BlockStateModel wrapped) {
		super(wrapped);
	}

	private static @Nullable CloakRenderData cloak(BlockAndTintGetter level, BlockPos pos) {
		return level.getBlockEntityRenderData(pos) instanceof CloakRenderData data ? data : null;
	}

	/**
	 * False for block entity blocks whose model has no quads (chests, signs, beds...): a renderer draws those, so
	 * a facade of them would be invisible. Other models count as drawable, including custom ones that only emit
	 * quads in world.
	 */
	public static boolean hasGeometry(BlockState state) {
		if (!(state.getBlock() instanceof EntityBlock)) {
			return true;
		}
		List<BlockStateModelPart> parts = new ArrayList<>();
		models().get(state).collectParts(RandomSource.create(42L), parts);
		for (BlockStateModelPart part : parts) {
			if (!part.getQuads(null).isEmpty()) {
				return true;
			}
			for (Direction direction : Direction.values()) {
				if (!part.getQuads(direction).isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	private static BlockStateModelSet models() {
		return Minecraft.getInstance().getModelManager().getBlockStateModelSet();
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest) {
		CloakRenderData cloak = cloak(level, pos);
		if (cloak == null) {
			super.emitQuads(emitter, level, pos, state, random, cullTest);
			return;
		}
		if (!cloak.hidesCable()) {
			super.emitQuads(emitter, level, pos, state, random, cullTest);
		}
		BlockState look = cloak.facade() != null ? cloak.facade() : CableCloak.FRAME_MODEL_BLOCK.defaultBlockState();
		// Tints are looked up against the block being drawn (the cable), so bake the copied block's own in.
		// Cables give off light, which makes the renderer fall back to flat lighting for the whole block. Light the
		// facade as the block it copies would be lit: smooth, unless that block glows or its model opts out.
		boolean smooth = look.getLightEmission() == 0;
		emitter.pushTransform(quad -> {
			// The facade is culled as the block it copies by the test below. Renderers such as Sodium cull every quad
			// once more against the block being drawn, the cable, and culling mods (MoreCulling) can then hide the
			// facade wherever a cable joins it. Without a cull face the quad is left to our test alone.
			quad.cullFace(null);
			if (quad.ambientOcclusion() == TriState.DEFAULT) {
				quad.ambientOcclusion(TriState.of(smooth));
			}
			int tintIndex = quad.tintIndex();
			if (tintIndex != -1) {
				BlockTintSource source = Minecraft.getInstance().getBlockColors().getTintSource(look, tintIndex);
				if (source != null) {
					quad.multiplyColor(source.colorInWorld(look, level, pos));
				}
				quad.tintIndex(-1);
			}
			return true;
		});
		// The given cull test compares the cable's shape with its neighbours, which hides the facade's face wherever
		// the cable connects (arm ends match). Cull the facade as the block it copies instead.
		Predicate<@Nullable Direction> facadeCull = direction -> direction != null
				&& !Block.shouldRenderFace(look, level.getBlockState(pos.relative(direction)), direction);
		models().get(look).emitQuads(emitter, level, pos, look, random, facadeCull);
		emitter.popTransform();
	}

	@Override
	public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		return cloak(level, pos) != null ? null : super.createGeometryKey(level, pos, state, random);
	}

	@Override
	public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		CloakRenderData cloak = cloak(level, pos);
		if (cloak != null && cloak.facade() != null) {
			return models().get(cloak.facade()).particleMaterial(level, pos, cloak.facade());
		}
		return super.particleMaterial(level, pos, state);
	}
}
