package com.aurus.tinytactics.render;

import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import com.aurus.tinytactics.TinyTactics;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;

public class TacticsDrawRenderPipelines {

    private static MappableRingBuffer vertexBuffer;

    public static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);

    public static final RenderPipeline TACTICS_LINES = RenderPipelines
            .register(setPreferencesAndBuild(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET),
                    VertexFormat.DrawMode.QUADS));

    public static final RenderPipeline TACTICS_CONES = RenderPipelines
            .register(setPreferencesAndBuild(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET),
                    VertexFormat.DrawMode.TRIANGLE_FAN));

    public static final RenderPipeline TACTICS_BANDS = RenderPipelines
            .register(setPreferencesAndBuild(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET),
                    VertexFormat.DrawMode.TRIANGLE_STRIP));

    private static RenderPipeline setPreferencesAndBuild(RenderPipeline.Builder builder,
            VertexFormat.DrawMode drawMode) {
        return builder.withLocation(Identifier.of(TinyTactics.MOD_ID, "pipeline/tactics_drawing"))
                .withVertexFormat(VertexFormats.POSITION_COLOR, drawMode)
                .withDepthTestFunction(DepthTestFunction.LESS_DEPTH_TEST)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false)
                .build();
    }

    public static final BufferAllocator ALLOCATOR = new BufferAllocator(RenderLayer.CUTOUT_BUFFER_SIZE);

    private TacticsDrawRenderPipelines() {
    }

    public static GpuBuffer upload(BuiltBuffer.DrawParameters drawParameters, VertexFormat format,
            BuiltBuffer builtBuffer) {
        // Calculate the size needed for the vertex buffer
        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

        // Initialize or resize the vertex buffer as needed
        if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
            vertexBuffer = new MappableRingBuffer(() -> TinyTactics.MOD_ID + " Shape Drawing Pipeline",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
        }

        // Copy vertex data into the vertex buffer
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        try (GpuBuffer.MappedView mappedView = commandEncoder
                .mapBuffer(vertexBuffer.getBlocking().slice(0, builtBuffer.getBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(builtBuffer.getBuffer(), mappedView.data());
        }

        return vertexBuffer.getBlocking();
    }

    public static void rotateVertexBuffer() {
        vertexBuffer.rotate();
    }

    public static void draw(MinecraftClient client, RenderPipeline pipeline, BuiltBuffer builtBuffer,
            BuiltBuffer.DrawParameters drawParameters, GpuBuffer vertices, VertexFormat format) {
        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        if (pipeline.getVertexFormatMode() == VertexFormat.DrawMode.QUADS) {
            // Sort the quads if there is translucency
            builtBuffer.sortQuads(TacticsDrawRenderPipelines.ALLOCATOR,
                    RenderSystem.getProjectionType().getVertexSorter());
            // Upload the index buffer
            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.getSortedBuffer());
            indexType = builtBuffer.getDrawParameters().indexType();
        } else {
            // Use the general shape index buffer for non-quad draw modes
            RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem
                    .getSequentialBuffer(pipeline.getVertexFormatMode());
            indices = shapeIndexBuffer.getIndexBuffer(drawParameters.indexCount());
            indexType = shapeIndexBuffer.getIndexType();
        }

        // Actually execute the draw
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .write(RenderSystem.getModelViewMatrix(), TacticsDrawRenderPipelines.COLOR_MODULATOR,
                        RenderSystem.getModelOffset(), RenderSystem.getTextureMatrix(), 1f);
        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> TinyTactics.MOD_ID + " Shape Drawing Pipeline Rendering",
                        client.getFramebuffer().getColorAttachmentView(), OptionalInt.empty(),
                        client.getFramebuffer().getDepthAttachmentView(), OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);

            // Bind texture if applicable:
            // Sampler0 is used for texture inputs in vertices
            // renderPass.bindSampler("Sampler0", textureView);

            renderPass.setVertexBuffer(0, vertices);
            renderPass.setIndexBuffer(indices, indexType);

            // The base vertex is the starting index when we copied the data into the vertex
            // buffer divided by vertex size
            // noinspection ConstantValue
            renderPass.drawIndexed(0 / format.getVertexSize(), 0, drawParameters.indexCount(), 1);
        }

        builtBuffer.close();
    }
}