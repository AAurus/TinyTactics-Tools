package com.aurus.tinytactics.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.MinecraftClient;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public class LineDrawer {
    private BufferBuilder buffer;
    private static final LineDrawer INSTANCE = new LineDrawer();

    private LineDrawer() {
    }

    public static LineDrawer getInstance() {
        return INSTANCE;
    }

    public void extractAndDrawLineStrip(WorldRenderContext context, List<Vec3d> positions, int color, double width) {
        extractQuadCrossLineStrip(context, positions, color, width);
        drawLines(MinecraftClient.getInstance(), TacticsDrawRenderPipelines.TACTICS_LINES);
    }

    public void extractAndDrawLinesToCorners(WorldRenderContext context, Vec3d from, Vec3d to, int color,
            float width) {
        extractLinesToCorners(context, from, to, color, width);
        drawLines(MinecraftClient.getInstance(), TacticsDrawRenderPipelines.TACTICS_LINES);
    }

    private void drawLines(MinecraftClient client, RenderPipeline pipeline) {
        BuiltBuffer builtBuffer = buffer.end();
        BuiltBuffer.DrawParameters params = builtBuffer.getDrawParameters();
        VertexFormat format = params.format();

        GpuBuffer verts = TacticsDrawRenderPipelines.upload(params, format, builtBuffer);
        TacticsDrawRenderPipelines.draw(client, pipeline, builtBuffer, params, verts, format);

        TacticsDrawRenderPipelines.rotateVertexBuffer();
        buffer = null;
    }

    public void extractQuadCrossLine(WorldRenderContext context, Vec3d pos1, Vec3d pos2, int color,
            double width) {
        List<Vec3d> vecs = createQuadCross(pos1, pos2, width);
        placeQuadsIntoWorld(context, vecs, color);
    }

    public void extractQuadCrossLineStrip(WorldRenderContext context, List<Vec3d> positions, int color,
            double width) {
        List<Vec3d> vecs = new ArrayList<>();
        for (int i = 1; i < positions.size(); i++) {
            Vec3d pos1 = positions.get(i - 1);
            Vec3d pos2 = positions.get(i);
            vecs.addAll(createQuadCross(pos1, pos2, width));
        }
        placeQuadsIntoWorld(context, vecs, color);
    }

    public void extractLinesToCorners(WorldRenderContext context, Vec3d from, Vec3d to, int color,
            float width) {
        List<Vec3d> vecs = new ArrayList<>();

        vecs.add(to.add(0.5, 0.5, 0.5));
        vecs.add(to.add(0.5, 0.5, -0.5));
        vecs.add(to.add(0.5, -0.5, 0.5));
        vecs.add(to.add(0.5, -0.5, -0.5));
        vecs.add(to.add(-0.5, 0.5, 0.5));
        vecs.add(to.add(-0.5, 0.5, -0.5));
        vecs.add(to.add(-0.5, -0.5, 0.5));
        vecs.add(to.add(-0.5, -0.5, -0.5));

        for (Vec3d vec : vecs) {
            extractQuadCrossLine(context, from, vec, color, width);
        }
    }

    public static List<Vec3d> createQuadCross(Vec3d pos1, Vec3d pos2, double lineWidth) {
        List<Vec3d> result = new ArrayList<>();

        Vec3d diff = pos2.subtract(pos1);
        Vec3d norm = diff.normalize();

        Vec3d norm1 = RenderUtils.getQuickPerpendicularNormal(norm);
        Vec3d norm2 = norm.crossProduct(norm1);

        Vec3d scale1 = norm1.multiply(lineWidth);
        Vec3d scale2 = norm2.multiply(lineWidth);

        result.add(pos1.add(scale1));
        result.add(pos2.add(scale1));
        result.add(pos2.subtract(scale1));
        result.add(pos1.subtract(scale1));
        result.add(pos2.add(scale2));
        result.add(pos1.add(scale2));
        result.add(pos1.subtract(scale2));
        result.add(pos2.subtract(scale2));

        return result;
    }

    public void placeQuadsIntoWorld(WorldRenderContext context, List<Vec3d> finalVecs, int color) {
        if (!finalVecs.isEmpty()) {
            Camera camera = context.camera();
            MatrixStack matrices = Objects.requireNonNull(context.matrixStack());

            matrices.push();

            if (buffer == null) {
                buffer = new BufferBuilder(TacticsDrawRenderPipelines.ALLOCATOR,
                        TacticsDrawRenderPipelines.TACTICS_LINES.getVertexFormatMode(),
                        TacticsDrawRenderPipelines.TACTICS_LINES.getVertexFormat());
            }

            matrices.translate(-camera.getPos().x, -camera.getPos().y, -camera.getPos().z);

            for (Vec3d vec : finalVecs) {
                buffer.vertex(matrices.peek(), (float) vec.getX(), (float) vec.getY(), (float) vec.getZ()).color(color);
            }

            RenderUtils.setRenderPreferences();

            matrices.pop();
        }
    }

}
