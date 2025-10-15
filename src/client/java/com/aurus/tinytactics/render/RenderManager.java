package com.aurus.tinytactics.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.joml.Quaternionf;

import com.aurus.tinytactics.data.TacticsDrawToolMap;
import com.aurus.tinytactics.data.TacticsRulerMap;
import com.aurus.tinytactics.data.TacticsShape;
import com.aurus.tinytactics.data.TacticsShapeMap;
import com.aurus.tinytactics.util.Collection;
import com.aurus.tinytactics.util.ListCollection;
import com.aurus.tinytactics.util.MapCollection;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.DyeColor;

import java.awt.Color;

public class RenderManager {

    private TacticsRulerMap tacticsRulerMap;
    private TacticsShapeMap tacticsShapeMap;

    private static RenderManager manager;

    private static final double MAIN_RULER_LINE_OPACITY = 1.0;
    private static final float MAIN_RULER_LINE_WIDTH = 0.05F;

    private static final double CORNER_RULER_LINE_OPACITY = 0.5;
    private static final float CORNER_RULER_LINE_WIDTH = 0.02F;

    private static final double SHAPE_OPACITY = 0.5;

    private static final RenderPipeline TACTICS_SHAPES_PIPELINE = RenderPipelines.TRANSLUCENT;

    private static LineDrawer lineDrawer = LineDrawer.getInstance();
    private static ShapeDrawer shapeDrawer = ShapeDrawer.getInstance();

    private RenderManager() {
        tacticsRulerMap = TacticsRulerMap.DEFAULT;
        tacticsShapeMap = TacticsShapeMap.DEFAULT;
    }

    public static RenderManager getManager() {
        if (manager == null) {
            manager = new RenderManager();
        }
        return manager;
    }

    public void init() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            renderAllRulerLines(context);
            renderAllShapes(context);
        });
    }

    public void updateRulerMap(TacticsRulerMap map) {
        this.tacticsRulerMap = map;
    }

    public void updateShapeMap(TacticsShapeMap map) {
        this.tacticsShapeMap = map;
    }

    private <E extends TacticsDrawToolMap<T, C>, T, C extends Collection<T>> void renderAllDrawFeatures(
            WorldRenderContext context, E map, CollectionConsumer<T> consumer) {
        for (Map<DyeColor, C> userMap : map.getFullMap().values()) {
            for (DyeColor color : userMap.keySet()) {
                consumer.consume(userMap.get(color), color);
            }
        }
    }

    private void renderAllRulerLines(WorldRenderContext context) {
        renderAllDrawFeatures(context, tacticsRulerMap, (collection, color) -> {
            List<BlockPos> list = ((ListCollection<BlockPos>) collection).getEntries();
            List<Vec3d> vecs = blockPosToVec3ds(list);

            if (vecs.size() >= 2) {
                int mainColor = setColorAlpha(color.getEntityColor(), MAIN_RULER_LINE_OPACITY);
                lineDrawer.extractAndDrawLineStrip(context, vecs, mainColor, MAIN_RULER_LINE_WIDTH);

                Vec3d from = vecs.get(Math.max(vecs.size() - 2, 0));
                Vec3d to = vecs.get(Math.max(vecs.size() - 1, 0));
                int conerColor = setColorAlpha(mainColor, CORNER_RULER_LINE_OPACITY);
                lineDrawer.extractAndDrawLinesToCorners(context, from, to, conerColor,
                        CORNER_RULER_LINE_WIDTH);
            }
        });
    }

    private void renderAllShapes(WorldRenderContext context) {
        renderAllDrawFeatures(context, tacticsShapeMap, (collection, color) -> {
            Map<TacticsShape.Type, TacticsShape> map = ((MapCollection<TacticsShape.Type, TacticsShape>) collection)
                    .getEntries();
            renderShape(TacticsShape.Type.CONE, map, (cone) -> {
                shapeDrawer.extractAndDrawCone(context, blockPosToVec3d(cone.getOrigin()), cone.getLength(),
                        cone.getDiameter(),
                        cone.getDirection(), setColorAlpha(color.getEntityColor(), SHAPE_OPACITY));
            });
            renderShape(TacticsShape.Type.SPHERE, map, (sphere) -> {
                shapeDrawer.extractAndDrawSphere(context, blockPosToVec3d(sphere.getOrigin()), sphere.getDiameter(),
                        setColorAlpha(color.getEntityColor(), SHAPE_OPACITY));
            });
            renderShape(TacticsShape.Type.LINE, map, (line) -> {
                shapeDrawer.extractAndDrawCylinder(context, blockPosToVec3d(line.getOrigin()), line.getLength(),
                        line.getDiameter(), line.getDirection(), setColorAlpha(color.getEntityColor(), SHAPE_OPACITY));
            });
        });
    }

    private void renderShape(TacticsShape.Type type, Map<TacticsShape.Type, TacticsShape> map,
            TacticsShapeConsumer consumer) {
        TacticsShape shape = map.get(type);
        if (shape != null) {
            consumer.consume(shape);
        }
    }

    public interface TacticsShapeConsumer {
        void consume(TacticsShape shape);
    }

    public interface CollectionConsumer<T> {
        void consume(Collection<T> collection, DyeColor color);
    }

    public static List<Vec3d> blockPosToVec3ds(List<BlockPos> blockPos) {
        List<Vec3d> result = new ArrayList<>();
        for (BlockPos pos : blockPos) {
            result.add(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        }
        return result;
    }

    public static Vec3d blockPosToVec3d(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public static int setColorAlpha(int color, double alpha) {
        Color baseColor = new Color(color, true);
        Color newColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(),
                ((int) (alpha * 255)));
        return newColor.getRGB();

    }

    public static Quaternionf getQuaternionTo(Vec3d from, Vec3d to) {
        Vec3d xyz = from.crossProduct(to);
        float w = (float) (Math.sqrt((from.lengthSquared()) * (to.lengthSquared())) + from.dotProduct(to));
        return new Quaternionf((float) xyz.getX(), (float) xyz.getY(), (float) xyz.getZ(), w).normalize();
    }

}
