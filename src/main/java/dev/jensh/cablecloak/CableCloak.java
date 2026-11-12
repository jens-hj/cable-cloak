package dev.jensh.cablecloak;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class CableCloak implements ModInitializer {
	public static final String MOD_ID = "cable_cloak";

	/** Never placed in the world: it only holds the frame's model, drawn around cables that wear a frame. */
	public static final Block FRAME_MODEL_BLOCK = registerBlock("cloak_frame",
			BlockBehaviour.Properties.of().noOcclusion().noLootTable().sound(SoundType.COPPER));

	public static final Item CLOAK_FRAME = registerItem("cloak_frame", CloakFrameItem::new);

	public static final BlockEntityType<CloakBlockEntity> CLOAK_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			id("cloak"), FabricBlockEntityTypeBuilder.create(CloakBlockEntity::new).build());

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	private static Block registerBlock(String name, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, new Block(properties.setId(key)));
	}

	private static Item registerItem(String name, java.util.function.Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}

	@Override
	public void onInitialize() {
		// Cables may register before or after us, so pick up both.
		BuiltInRegistries.BLOCK.forEach(CableCloak::addIfCloakable);
		RegistryEntryAddedCallback.event(BuiltInRegistries.BLOCK).register((raw, id, block) -> addIfCloakable(block));

		UseBlockCallback.EVENT.register(CloakInteractions::onUseBlock);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(CLOAK_FRAME));
	}

	private static void addIfCloakable(Block block) {
		if (CloakSupport.isCloakable(block)) {
			CLOAK_BE.addValidBlock(block);
		}
	}
}
