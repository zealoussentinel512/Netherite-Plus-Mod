/*
 * MIT License
 *
 * Copyright (c) 2021 OroArmor (Eli Orona)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.oroarmor.netherite_plus.block.entity;

import java.util.List;
import java.util.stream.IntStream;

import com.oroarmor.netherite_plus.block.NetheritePlusBlocks;
import com.oroarmor.netherite_plus.block.NetheriteShulkerBoxBlock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Tickable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class NetheriteShulkerBoxBlockEntity extends LootableContainerBlockEntity implements SidedInventory, Tickable {
    private static final int[] AVAILABLE_SLOTS = IntStream.range(0, 27).toArray();
    private DefaultedList<ItemStack> inventory;
    private int viewerCount;
    private AnimationStage animationStage;
    private float animationProgress;
    private float prevAnimationProgress;
    private DyeColor cachedColor;
    private boolean cachedColorUpdateNeeded;

    public NetheriteShulkerBoxBlockEntity() {
        this(null);
        cachedColorUpdateNeeded = true;
    }

    public NetheriteShulkerBoxBlockEntity(DyeColor color) {
        super(NetheritePlusBlocks.NETHERITE_SHULKER_BOX_ENTITY.get());
        inventory = DefaultedList.ofSize(27, ItemStack.EMPTY);
        animationStage = AnimationStage.CLOSED;
        cachedColor = color;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        Block block = Block.getBlockFromItem(stack.getItem());
        return !(block instanceof NetheriteShulkerBoxBlock) && !(block instanceof ShulkerBoxBlock);
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new ShulkerBoxScreenHandler(syncId, playerInventory, this);
    }

    public void deserializeInventory(CompoundTag tag) {
        inventory = DefaultedList.ofSize(size(), ItemStack.EMPTY);
        if (!deserializeLootTable(tag) && tag.contains("Items", 9)) {
            Inventories.fromTag(tag, inventory);
        }

    }

    @Override
    public void fromTag(BlockState state, CompoundTag tag) {
        super.fromTag(state, tag);
        deserializeInventory(tag);
    }

    public float getAnimationProgress(float f) {
        return MathHelper.lerp(f, prevAnimationProgress, animationProgress);
    }

    public AnimationStage getAnimationStage() {
        return animationStage;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return AVAILABLE_SLOTS;
    }

    public Box getBoundingBox(BlockState state) {
        return this.getBoundingBox(state.get(NetheriteShulkerBoxBlock.FACING));
    }

    public Box getBoundingBox(Direction openDirection) {
        float f = getAnimationProgress(1.0F);
        return VoxelShapes.fullCube().getBoundingBox().stretch(0.5F * f * openDirection.getOffsetX(), 0.5F * f * openDirection.getOffsetY(), 0.5F * f * openDirection.getOffsetZ());
    }

    private Box getCollisionBox(Direction facing) {
        Direction direction = facing.getOpposite();
        return this.getBoundingBox(facing).shrink(direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ());
    }

    @Environment(EnvType.CLIENT)
    public DyeColor getColor() {
        if (cachedColorUpdateNeeded) {
            cachedColor = NetheriteShulkerBoxBlock.getColor(getCachedState().getBlock());
            cachedColorUpdateNeeded = false;
        }

        return cachedColor;
    }

    @Override
    protected Text getContainerName() {
        return new TranslatableText("container.netheriteShulkerBox");
    }

    @Override
    protected DefaultedList<ItemStack> getInvStackList() {
        return inventory;
    }

    @Override
    protected void setInvStackList(DefaultedList<ItemStack> list) {
        inventory = list;
    }

    @Override
    public void onClose(PlayerEntity player) {
        if (!player.isSpectator()) {
            --viewerCount;
            world.addSyncedBlockEvent(pos, getCachedState().getBlock(), 1, viewerCount);
            if (viewerCount <= 0) {
                world.playSound(null, pos, SoundEvents.BLOCK_SHULKER_BOX_CLOSE, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
            }
        }

    }

    @Override
    public void onOpen(PlayerEntity player) {
        if (!player.isSpectator()) {
            if (viewerCount < 0) {
                viewerCount = 0;
            }

            ++viewerCount;
            world.addSyncedBlockEvent(pos, getCachedState().getBlock(), 1, viewerCount);
            if (viewerCount == 1) {
                world.playSound(null, pos, SoundEvents.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
            }
        }

    }

    @Override
    public boolean onSyncedBlockEvent(int type, int data) {
        if (type == 1) {
            viewerCount = data;
            if (data == 0) {
                animationStage = AnimationStage.CLOSING;
                updateNeighborStates();
            }

            if (data == 1) {
                animationStage = AnimationStage.OPENING;
                updateNeighborStates();
            }

            return true;
        }
        return super.onSyncedBlockEvent(type, data);
    }

    private void pushEntities() {
        BlockState blockState = world.getBlockState(getPos());
        if (blockState.getBlock() instanceof NetheriteShulkerBoxBlock) {
            Direction direction = blockState.get(NetheriteShulkerBoxBlock.FACING);
            Box box = getCollisionBox(direction).offset(pos);
            List<Entity> list = world.getOtherEntities(null, box);
            if (!list.isEmpty()) {
                for (int i = 0; i < list.size(); ++i) {
                    Entity entity = list.get(i);
                    if (entity.getPistonBehavior() != PistonBehavior.IGNORE) {
                        double d = 0.0D;
                        double e = 0.0D;
                        double f = 0.0D;
                        Box box2 = entity.getBoundingBox();
                        switch (direction.getAxis()) {
                            case X:
                                if (direction.getDirection() == Direction.AxisDirection.POSITIVE) {
                                    d = box.maxX - box2.minX;
                                } else {
                                    d = box2.maxX - box.minX;
                                }

                                d += 0.01D;
                                break;
                            case Y:
                                if (direction.getDirection() == Direction.AxisDirection.POSITIVE) {
                                    e = box.maxY - box2.minY;
                                } else {
                                    e = box2.maxY - box.minY;
                                }

                                e += 0.01D;
                                break;
                            case Z:
                                if (direction.getDirection() == Direction.AxisDirection.POSITIVE) {
                                    f = box.maxZ - box2.minZ;
                                } else {
                                    f = box2.maxZ - box.minZ;
                                }

                                f += 0.01D;
                        }

                        entity.move(MovementType.SHULKER_BOX, new Vec3d(d * direction.getOffsetX(), e * direction.getOffsetY(), f * direction.getOffsetZ()));
                    }
                }

            }
        }
    }

    public CompoundTag serializeInventory(CompoundTag tag) {
        if (!serializeLootTable(tag)) {
            Inventories.toTag(tag, inventory, false);
        }

        return tag;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        super.setStack(slot, stack);
    }

    @Override
    public int size() {
        return inventory.size();
    }

    public boolean suffocates() {
        return animationStage == AnimationStage.CLOSED;
    }

    @Override
    public void tick() {
        updateAnimation();
        if (animationStage == AnimationStage.OPENING || animationStage == AnimationStage.CLOSING) {
            pushEntities();
        }

    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);
        return serializeInventory(tag);
    }

    protected void updateAnimation() {
        prevAnimationProgress = animationProgress;
        switch (animationStage) {
            case CLOSED:
                animationProgress = 0.0F;
                break;
            case OPENING:
                animationProgress += 0.1F;
                if (animationProgress >= 1.0F) {
                    pushEntities();
                    animationStage = AnimationStage.OPENED;
                    animationProgress = 1.0F;
                    updateNeighborStates();
                }
                break;
            case CLOSING:
                animationProgress -= 0.1F;
                if (animationProgress <= 0.0F) {
                    animationStage = AnimationStage.CLOSED;
                    animationProgress = 0.0F;
                    updateNeighborStates();
                }
                break;
            case OPENED:
                animationProgress = 1.0F;
        }

    }

    private void updateNeighborStates() {
        getCachedState().updateNeighbors(getWorld(), getPos(), 3);
    }

    public enum AnimationStage {
        CLOSED, OPENING, OPENED, CLOSING
    }
}
