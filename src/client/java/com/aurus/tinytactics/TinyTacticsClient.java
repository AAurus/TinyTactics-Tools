package com.aurus.tinytactics;

import com.aurus.tinytactics.data.ColorProviders;
import com.aurus.tinytactics.data.TacticsRulerMap;
import com.aurus.tinytactics.data.TacticsRulerMapPayload;
import com.aurus.tinytactics.data.TacticsShapeMap;
import com.aurus.tinytactics.data.TacticsShapeMapPayload;
import com.aurus.tinytactics.render.RenderManager;
import com.aurus.tinytactics.render.blocks.ActorMarkerBlockRenderer;
import com.aurus.tinytactics.registry.BlockRegistrar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.world.ClientWorld;

public class TinyTacticsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        RenderManager.getManager().init();

        ClientPlayNetworking.registerGlobalReceiver(TacticsRulerMapPayload.ID,
                TinyTacticsClient::receiveRulerMapPacket);
        ClientPlayNetworking.registerGlobalReceiver(TacticsShapeMapPayload.ID,
                TinyTacticsClient::receiveShapeMapPacket);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ServerHandler.broadcastRulerData();
            ServerHandler.broadcastShapeData();
        });

        // ColorProviderRegistry.ITEM.register(ColorProviders::getItemColor,
        // ItemRegistrar.SIMPLE_DYEABLE_ITEMS);
        ColorProviderRegistry.BLOCK.register(ColorProviders::getBlockEntityColor,
                BlockRegistrar.SIMPLE_DYEABLE_BLOCKS);

        BlockEntityRendererFactories.register(BlockRegistrar.ACTOR_MARKER_BLOCK_ENTITY, ActorMarkerBlockRenderer::new);
        BlockRenderLayerMap.putBlock(BlockRegistrar.ACTOR_MARKER, BlockRenderLayer.CUTOUT);
    }

    public static void receiveRulerMapPacket(TacticsRulerMapPayload payload, ClientPlayNetworking.Context context) {
        ClientWorld world = context.client().world;

        if (world == null) {
            return;
        }

        TacticsRulerMap map = payload.map();
        RenderManager.getManager().updateRulerMap(map);

        TinyTactics.LOGGER.info("Positions Received");
    }

    public static void receiveShapeMapPacket(TacticsShapeMapPayload payload, ClientPlayNetworking.Context context) {
        ClientWorld world = context.client().world;

        if (world == null) {
            return;
        }

        TacticsShapeMap map = payload.map();
        RenderManager.getManager().updateShapeMap(map);

        TinyTactics.LOGGER.info("Positions Received");
    }
}
