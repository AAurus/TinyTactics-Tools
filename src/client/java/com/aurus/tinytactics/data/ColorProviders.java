package com.aurus.tinytactics.data;

import org.jetbrains.annotations.Nullable;

import com.aurus.tinytactics.TinyTactics;
import com.aurus.tinytactics.registry.DataRegistrar;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public class ColorProviders {

    public static int getItemColor(ItemStack stack, int tintIndex) {
        if (tintIndex == 0) {
            return ((DyeColor) stack.get(DataRegistrar.DYE_COLOR)).getEntityColor();
        }
        return -1;
    }

    public static int getBlockStateColor(BlockState state, @Nullable BlockRenderView world, @Nullable BlockPos pos,
            int tintIndex) {
        if (tintIndex == 0) {
            if (state.getBlock().getStateManager().getProperty("dye_color") instanceof DyeColorProperty prop) {
                DyeColor color = ((DyeColor) state.get(prop));
                return color.getFireworkColor();
            }
        }
        return -1;
    }

    public static int getBlockEntityColor(BlockState state, @Nullable BlockRenderView world, @Nullable BlockPos pos,
            int tintIndex) {
        if (tintIndex == 0) {
            if (world != null && pos != null) {
                BlockEntity entity = world.getBlockEntity(pos);
                if (entity != null) {
                    DyeColor color = entity.getComponents().get(DataRegistrar.DYE_COLOR);
                    if (color != null) {
                        TinyTactics.LOGGER.info(color.asString());
                        return color.getEntityColor();
                    }
                }
            } else {
                if (tintIndex == 0) {
                    if (state.getBlock().getStateManager().getProperty("dye_color") instanceof DyeColorProperty prop) {
                        DyeColor color = ((DyeColor) state.get(prop));
                        return color.getEntityColor();
                    }
                }
            }
        }
        return -1;
    }
}
