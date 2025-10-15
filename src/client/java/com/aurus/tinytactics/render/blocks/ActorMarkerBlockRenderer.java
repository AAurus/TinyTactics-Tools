package com.aurus.tinytactics.render.blocks;

import org.joml.Vector3f;

import com.aurus.tinytactics.blocks.actor_marker.ActorMarkerBlock;
import com.aurus.tinytactics.blocks.actor_marker.ActorMarkerBlockEntity;
import com.aurus.tinytactics.blocks.actor_marker.ActorMarkerRotationHelper;
import com.aurus.tinytactics.data.ActorMarkerInventory;
import com.aurus.tinytactics.data.ItemAttachmentPosition;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ActorMarkerBlockRenderer implements BlockEntityRenderer<ActorMarkerBlockEntity> {

    private static final ItemAttachmentPosition HEAD_POSITION = new ItemAttachmentPosition(
            new Vector3f(8.0F, 11.0F, 8.0F), new Vector3f(0.5F,
                    0.5F,
                    0.5F),
            new EulerAngle(0, 0, 0), ItemDisplayContext.HEAD,
            ActorMarkerInventory.HEAD_KEY);
    private static final ItemAttachmentPosition LEFT_HAND_POSITION = new ItemAttachmentPosition(
            new Vector3f(5.0F, 4.0F, 6.5F),
            new Vector3f(0.5F,
                    0.5F,
                    0.5F),
            new EulerAngle(40, 20, -90), ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
            ActorMarkerInventory.LEFT_HAND_KEY,
            true, true);
    private static final ItemAttachmentPosition RIGHT_HAND_POSITION = new ItemAttachmentPosition(
            new Vector3f(11.0F, 4.0F, 6.5F),
            new Vector3f(0.5F,
                    0.5F,
                    0.5F),
            new EulerAngle(40, -20, 90), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            ActorMarkerInventory.RIGHT_HAND_KEY,
            true);
    private static final ItemAttachmentPosition ATTACHMENT_POSITION = new ItemAttachmentPosition(
            new Vector3f(8.0F, 7.5F, 12F),
            new Vector3f(0.4F,
                    0.4F,
                    0.4F),
            new EulerAngle(0, 180, 0), ItemDisplayContext.FIXED,
            ActorMarkerInventory.ATTACHMENT_KEY);

    private BlockEntityRendererFactory.Context context;

    public ActorMarkerBlockRenderer(BlockEntityRendererFactory.Context context) {
        this.context = context;
    }

    @Override
    public void render(ActorMarkerBlockEntity entity, float tickProgress, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light, int overlay, Vec3d cameraPos) {
        World world = entity.getWorld();
        BlockPos pos = entity.getPos();
        BlockState state = entity.getCachedState();

        Text name = entity.getComponents().get(DataComponentTypes.CUSTOM_NAME);
        if (name != null) {
            renderName(name, entity, state, matrices, vertexConsumers, light, overlay);
        }

        matrices.push();

        rotateToLocal(matrices, state);

        BlockRenderManager renderManager = MinecraftClient.getInstance().getBlockRenderManager();

        renderManager.renderBlock(state, pos, world, matrices,
                vertexConsumers.getBuffer(RenderLayers.getEntityBlockLayer(state)), false,
                renderManager.getModel(state).getParts(null));

        matrices.pop();

        renderItemAttachments(entity, tickProgress, matrices, vertexConsumers, light, overlay);
    }

    private void renderName(Text name, ActorMarkerBlockEntity entity, BlockState state, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light, int overlay) {
        matrices.push();
        matrices.translate(0.5, 1.25, 0.5);

        TextRenderer textRenderer = context.getTextRenderer();

        float width = textRenderer.getWidth(name);

        matrices.scale(1 / width, 1 / width, 1 / width);

        matrices.multiply(MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation());

        matrices.scale(1, -1, -1);

        textRenderer.draw(name, -width / 2, 0, 0xFFFFFF, false,
                matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL,
                (int) (MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F)
                        * 255.0F) << 24,
                light);
        matrices.pop();
    }

    public void renderItemAttachments(ActorMarkerBlockEntity entity, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light, int overlay) {
        ItemRenderer renderer = MinecraftClient.getInstance().getItemRenderer();
        renderAttachmentItem(
                HEAD_POSITION, renderer, entity, tickDelta, matrices,
                vertexConsumers,
                light, overlay);
        renderAttachmentItem(
                LEFT_HAND_POSITION, renderer, entity, tickDelta, matrices,
                vertexConsumers, light, overlay);
        renderAttachmentItem(RIGHT_HAND_POSITION, renderer, entity, tickDelta,
                matrices,
                vertexConsumers, light, overlay);
        renderAttachmentItem(
                ATTACHMENT_POSITION, renderer, entity, tickDelta,
                matrices,
                vertexConsumers, light, overlay);
    }

    private static void rotateToLocal(MatrixStack matrices, BlockState state) {
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.NEGATIVE_Y
                .rotationDegrees(ActorMarkerRotationHelper
                        .toDegrees(state.get(ActorMarkerBlock.ROTATION))));
        matrices.translate(-0.5, -0.5, -0.5);
    }

    public void renderAttachmentItem(ItemAttachmentPosition attachmentPosition, ItemRenderer renderer,
            ActorMarkerBlockEntity entity,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light, int overlay) {
        ItemStack item = entity.getItem(attachmentPosition.itemKey);
        if (!item.isEmpty()) {
            renderAttachmentItem(attachmentPosition, item, renderer, entity, tickDelta, matrices, vertexConsumers,
                    light, overlay);
        }
    }

    public void renderAttachmentItem(ItemAttachmentPosition attachmentPosition,
            ItemStack item, ItemRenderer renderer, ActorMarkerBlockEntity entity,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light, int overlay) {
        World world = entity.getWorld();
        BlockState state = entity.getCachedState();
        matrices.push();

        rotateToLocal(matrices, state);
        attachmentPosition.transformToAttachment(matrices);
        if (attachmentPosition.isHand) {
            attachmentPosition.rotateToHand(matrices);
        }
        renderer.renderItem(null, item, attachmentPosition.mode, // attachmentPosition.leftHanded,
                matrices, vertexConsumers, world, light, overlay, 0);

        matrices.pop();
    }

}
