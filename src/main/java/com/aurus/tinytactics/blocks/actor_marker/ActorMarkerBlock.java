package com.aurus.tinytactics.blocks.actor_marker;

import java.util.Map;

import com.aurus.tinytactics.data.ActorMarkerInventory;
import com.aurus.tinytactics.data.DyeColorProperty;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class ActorMarkerBlock extends BlockWithEntity {

    public static final int MIN_ROTATION_INDEX = 0;
    public static final int MAX_ROTATION_INDEX = 15;

    private static final Map<Integer, String> LOCAL_ROTATION_ITEMS = Map.ofEntries(
            Map.entry(ActorMarkerRotationHelper.LEFT, ActorMarkerInventory.LEFT_HAND_KEY),
            Map.entry(ActorMarkerRotationHelper.RIGHT, ActorMarkerInventory.RIGHT_HAND_KEY),
            Map.entry(ActorMarkerRotationHelper.FRONT, ActorMarkerInventory.HEAD_KEY),
            Map.entry(ActorMarkerRotationHelper.BACK, ActorMarkerInventory.ATTACHMENT_KEY));

    public static final IntProperty ROTATION = IntProperty.of("rotation", MIN_ROTATION_INDEX, MAX_ROTATION_INDEX);
    public static final Property<DyeColor> COLOR = DyeColorProperty.of("dye_color");

    private static final SoundEvent EQUIP_SOUND = SoundEvents.ITEM_ARMOR_EQUIP_GENERIC.value();
    private static final SoundEvent ROTATE_SOUND = SoundEvents.BLOCK_COMPARATOR_CLICK;

    private static final VoxelShape OUTLINE_SHAPE = Block.createCuboidShape(1.0, 1.0, 1.0, 15.0, 15.0, 15.0);

    public ActorMarkerBlock(Settings settings) {
        super(settings.noCollision().luminance(state -> {
            return state.get(ROTATION);
        }));
        setDefaultState(getDefaultState().with(ROTATION, 0).with(COLOR, DyeColor.WHITE));
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ActorMarkerBlockEntity(pos, state);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
        builder.add(COLOR);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE_SHAPE;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    protected VoxelShape getCullingShape(BlockState state, BlockView world,
            BlockPos pos) {
        return VoxelShapes.empty();
    }

    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(ROTATION, ActorMarkerRotationHelper.fromYaw(context.getPlayerYaw()));
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!player.getAbilities().allowModifyWorld) {
            return ActionResult.PASS;
        }
        int localDirection = ActorMarkerRotationHelper.toLocalDirection(player.getYaw(), state.get(ROTATION));
        boolean itemSuccess = false;
        if (player.getMainHandStack().equals(ItemStack.EMPTY)) {
            itemSuccess = dequipActorMarker(state, world, pos, player, localDirection);
        } else {
            itemSuccess = equipActorMarker(state, world, pos, player, localDirection);
        }
        if (itemSuccess) {
            world.playSound(player, pos, EQUIP_SOUND, SoundCategory.BLOCKS);
            return ActionResult.SUCCESS;
        } else if ((localDirection == ActorMarkerRotationHelper.LEFT
                || localDirection == ActorMarkerRotationHelper.RIGHT)) {
            int newRotation = state.get(ROTATION) + (-1 * (localDirection + 4)) + (MAX_ROTATION_INDEX
                    + 1);
            world.setBlockState(pos, state.with(ROTATION, (newRotation % (MAX_ROTATION_INDEX + 1))));
            world.playSound(player, pos, ROTATE_SOUND, SoundCategory.BLOCKS);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        ItemStack itemStack = super.getPickStack(world, pos, state, true);
        return itemStack;
    }

    protected boolean equipActorMarker(BlockState state, World world, BlockPos pos, PlayerEntity player,
            int localDirection) {
        if (world.getBlockEntity(pos) instanceof ActorMarkerBlockEntity actorMarkerBlockEntity) {
            String key = LOCAL_ROTATION_ITEMS.get(localDirection);
            if (!actorMarkerBlockEntity.hasItem(key)) {
                actorMarkerBlockEntity.setItem(key, player.getMainHandStack().copy());
                return true;
            }
        }
        return false;
    }

    protected boolean dequipActorMarker(BlockState state, World world, BlockPos pos, PlayerEntity player,
            int localDirection) {
        if (world.getBlockEntity(pos) instanceof ActorMarkerBlockEntity actorMarkerBlockEntity) {
            String key = LOCAL_ROTATION_ITEMS.get(localDirection);
            if (actorMarkerBlockEntity.hasItem(key)) {
                actorMarkerBlockEntity.setItem(key, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    protected boolean rotateActorMarker(boolean counterClockwise, BlockState state, World world, BlockPos pos,
            PlayerEntity player, int localDirection) {
        int newRotation = state.get(ROTATION) + (-1 * (localDirection + 4)) + (MAX_ROTATION_INDEX
                + 1);
        world.setBlockState(pos, state.with(ROTATION, (newRotation % (MAX_ROTATION_INDEX + 1))));
        world.playSound(player, pos, ROTATE_SOUND, SoundCategory.BLOCKS);
        return true;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(ActorMarkerBlock::new);
    }

}
