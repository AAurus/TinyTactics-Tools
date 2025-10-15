package com.aurus.tinytactics.blocks.actor_marker;

import com.aurus.tinytactics.data.ActorMarkerInventory;
import com.aurus.tinytactics.registry.BlockRegistrar;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;

public class ActorMarkerBlockEntity extends BlockEntity {

    public ActorMarkerInventory items = ActorMarkerInventory.DEFAULT;

    public ActorMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockRegistrar.ACTOR_MARKER_BLOCK_ENTITY, pos, state);
    }

    public boolean setItem(String key, ItemStack item) {
        ActorMarkerInventory newItems = items.setItem(key, item);
        if (newItems != null) {
            items = newItems;
            markDirty();
            return true;
        }
        return false;
    }

    public ItemStack getItem(String key) {
        return items.getItem(key);
    }

    public boolean hasItem(String key) {
        return items.hasItem(key);
    }

    @Override
    protected void writeData(WriteView writeView) {
        writeView.put("actor_marker_inventory", ActorMarkerInventory.CODEC, items);
        super.writeData(writeView);
    }

    @Override
    protected void readData(ReadView readView) {
        super.readData(readView);

        items = readView.read("actor_marker_inventory", ActorMarkerInventory.CODEC)
                .orElse(ActorMarkerInventory.DEFAULT);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup lookup) {
        return createNbt(lookup);
    }

}
