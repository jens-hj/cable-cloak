package dev.jensh.cablecloak.client;

import dev.jensh.cablecloak.CloakInteractions;
import dev.jensh.cablecloak.CloakSupport;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;

public class CableCloakClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register(ModelModifier.WRAP_PHASE,
				(model, ctx) -> CloakSupport.isCloakable(ctx.state()) ? new CloakedCableModel(model) : model));
		CloakInteractions.hasModel = CloakedCableModel::hasGeometry;
	}
}
