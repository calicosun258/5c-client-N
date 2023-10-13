package fifthcolumn.n.modules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class BuildPoop extends Module {
   private final SettingGroup sgGeneral;
   private final SettingGroup sgRender;
   private final SettingGroup sgStairCase;
   private final Setting<PlaceMode> placeMode;
   private final Setting<Double> range;
   private final Setting<Integer> delay;
   private final Setting<Integer> maxBlocksPerTick;
   private final Setting<Boolean> swingHand;
   private final Setting<Boolean> rotate;
   private final Setting<Boolean> render;
   private final Setting<SettingColor> lineColor;
   private final Setting<SettingColor> sideColor;
   private final Setting<SettingColor> overlapColor;
   private final Setting<ShapeMode> shapeMode;
   private final Setting<Integer> height;
   private final Setting<Boolean> reverse;
   private final Setting<Boolean> autoFly;
   private final Setting<Boolean> stopOnIntersect;
   private final Setting<Boolean> snapOnIntersect;
   private final Setting<Integer> snapDistance;
   private final Pool<BlockPos.Mutable> blockPosPool;
   private final List<BlockPos.Mutable> blocksList;
   private final Pool<RenderBlock> renderBlockPool;
   private final List<RenderBlock> renderBlocks;
   private final List<RenderBlock> renderOverlapBlocks;
   private final List<BlockPos> stairsList;
   private BlockPos snapPos;
   private Direction snapDir;
   private BlockPos flyHereBlock;
   private HitResult hitResult;
   private boolean started;
   private int delay1;

   public BuildPoop() {
      super(NAddOn.FIFTH_COLUMN_CATEGORY, "Build Poop", "Shit on the noobs");
      this.sgGeneral = this.settings.getDefaultGroup();
      this.sgRender = this.settings.createGroup("Render");
      this.sgStairCase = this.settings.createGroup("StairCase");
      this.placeMode = this.sgGeneral.add(new EnumSetting.Builder<PlaceMode>()
              .name("place-mode")
              .description("How to place")
              .defaultValue(PlaceMode.Floor)
              .build());
      this.range = this.sgGeneral.add((new DoubleSetting.Builder())
              .name("range")
              .description("Custom range to place at.")
              .defaultValue(4.5).min(1.0).max(4.5)
              .build());
      this.delay = this.sgGeneral.add((new IntSetting.Builder())
              .name("delay")
              .description("tick delay for placement. recommended 0 unless you are lagging")
              .defaultValue(0).min(0).max(2)
              .build());
      this.maxBlocksPerTick = this.sgGeneral.add((new IntSetting.Builder())
              .name("max-blocks-per-tick")
              .description("blocks to place per tick")
              .defaultValue(1).min(1).max(2)
              .build());
      this.swingHand = this.sgGeneral.add(new BoolSetting.Builder()
              .name("swing-hand")
              .description("Swing hand client side.")
              .defaultValue(true)
              .build());
      this.rotate = this.sgGeneral.add(new BoolSetting.Builder()
              .name("rotate")
              .description("Spinny spinny")
              .defaultValue(true)
              .build());
      this.render = this.sgRender.add(new BoolSetting.Builder()
              .name("render")
              .description("Renders a block overlay where the blocks will be placed.")
              .defaultValue(true)
              .build());
      this.lineColor = this.sgRender.add(new ColorSetting.Builder()
              .name("line-color")
              .description("The color of the lines of the blocks being rendered.")
              .visible(this.render::get)
              .defaultValue(new SettingColor(204, 0, 0, 255))
              .build());
      this.sideColor = this.sgRender.add(new ColorSetting.Builder().name("side-color")
              .description("The color of the sides of the blocks being rendered.")
              .visible(this.render::get)
              .defaultValue(new SettingColor(204, 0, 0, 10))
              .build());

      this.overlapColor = this.sgRender.add(new ColorSetting.Builder()
              .name("overlap-color")
              .description("The color of the lines & sides of the blocks being rendered when overlapping a block")
              .visible(this.render::get)
              .defaultValue(new SettingColor(0, 204, 0, 50))
              .build());
      this.shapeMode = this.sgRender.add(new EnumSetting.Builder<ShapeMode>()
              .name("shape-mode")
              .description("How the shapes are rendered.")
              .visible(this.render::get)
              .defaultValue(ShapeMode.Both)
              .build());
      this.height = this.sgStairCase.add(new IntSetting.Builder()
              .name("height")
              .description("Max height for each staircase placement")
              .defaultValue(16).min(2)
              .sliderMax(200)
              .build());
      this.reverse = this.sgStairCase.add(new BoolSetting.Builder()
              .name("reverse")
              .description("start from top when staircasing")
              .defaultValue(false)
              .build());
      this.autoFly = this.sgStairCase.add(new BoolSetting.Builder()
              .name("Auto Fly")
              .description("fly while staircasing")
              .defaultValue(Boolean.FALSE)
              .build());
      this.stopOnIntersect = this.sgStairCase.add(new BoolSetting.Builder()
              .name("Stop On Intersect")
              .description("Stairs will only go up to the first intersecting block and not past it")
              .defaultValue(Boolean.FALSE)
              .build());
      this.snapOnIntersect = this.sgStairCase.add(new BoolSetting.Builder()
              .name("Snap On Intersect")
              .description("Staircase will snap to other staircases")
              .defaultValue(false)
              .build());
      this.snapDistance = this.sgStairCase.add(new IntSetting.Builder().name("Snap Distance")
              .description("The distance that snapping breaks at")
              .defaultValue(3)
              .build());
      this.blockPosPool = new Pool<>(net.minecraft.util.math.BlockPos.Mutable::new);
      this.blocksList = new ArrayList<>();
      this.renderBlockPool = new Pool<>(RenderBlock::new);
      this.renderBlocks = new ArrayList<>();
      this.renderOverlapBlocks = new ArrayList<>();
      this.stairsList = new ArrayList<>();
      this.snapPos = null;
      this.snapDir = null;
      this.flyHereBlock = null;
      this.started = false;
      this.delay1 = 0;
   }

   public void onActivate() {
      super.onActivate();

      for (RenderBlock renderBlock : this.renderBlocks) {
         this.renderBlockPool.free(renderBlock);
      }

      this.renderBlocks.clear();
      this.started = false;
      this.stairsList.clear();
   }

   public void onDeactivate() {
      super.onDeactivate();

      for (RenderBlock renderBlock : this.renderBlocks) {
         this.renderBlockPool.free(renderBlock);
      }

      this.renderBlocks.clear();
      this.started = false;
      this.stairsList.clear();
   }

   @EventHandler
   private void onTickPre(TickEvent.Pre event) {
      this.renderBlocks.forEach(RenderBlock::tick);
      this.renderOverlapBlocks.forEach(RenderBlock::tick);
      this.renderBlocks.removeIf(renderBlock -> (renderBlock.ticks <= 0));
      this.renderOverlapBlocks.removeIf(renderBlock -> (renderBlock.ticks <= 0));
      if (this.placeMode.get() == PlaceMode.StairCase) {
         for (BlockPos pos : this.stairsList) {
            BlockState state = this.mc.world.getBlockState(pos);
            if (state.isAir() || state.isLiquid()) {
               this.renderBlocks.add(this.renderBlockPool.get().set(pos, 1));
               continue;
            }
            this.renderOverlapBlocks.add(this.renderBlockPool.get().set(pos, 1));
         }
         if (!this.started) {
            if (this.snapPos == null || this.stairsList.isEmpty() || !this.snapOnIntersect.get() || this.snapDir != this.mc.player.getHorizontalFacing() ||
                    Math.sqrt(this.mc.getCameraEntity().getBlockPos().getSquaredDistance(this.snapPos)) > this.snapDistance.get().intValue()) {
               this.snapPos = null;
               for (CardinalDirection dir : CardinalDirection.values()) {
                  if (dir.toDirection() == this.mc.player.getHorizontalFacing()) {
                     this.stairsList.clear();
                     this.hitResult = this.mc.getCameraEntity().raycast(this.mc.interactionManager.getReachDistance(), 0.0F, false);
                     if (!(this.hitResult instanceof BlockHitResult) ||
                             !(this.mc.player.getMainHandStack().getItem() instanceof BlockItem))
                        break;
                     BlockPos startBlock = ((BlockHitResult)this.hitResult).getBlockPos();
                     int x = 0, z = 0;
                     switch (dir) {
                        case South -> z = 1;
                        case East -> x = 1;
                        case West -> x = -1;
                        default -> z = -1;
                     }
                     for (int i = 1; i <= this.height.get().intValue(); i++) {
                        BlockPos nextStair = new BlockPos(startBlock.getX() + x * i, this.reverse.get().booleanValue() ? (startBlock.getY() - i) : (startBlock.getY() + i), startBlock.getZ() + z * i);
                        if (startBlock.getY() + i < 318)
                           this.stairsList.add(nextStair);
                        if (this.stopOnIntersect.get().booleanValue()) {
                           BlockState state = this.mc.world.getBlockState(nextStair);
                           if (!state.isAir() && !state.isLiquid()) {
                              this.snapPos = this.mc.getCameraEntity().getBlockPos();
                              this.snapDir = this.mc.player.getHorizontalFacing();
                              break;
                           }
                        }
                     }
                  }
               }
            }
         } else if (this.stairsList.isEmpty()) {
            this.started = false;
         }
      }
      double pX = this.mc.player.getX();
      double pY = this.mc.player.getY();
      double pZ = this.mc.player.getZ();
      double rangeSq = Math.pow(this.range.get().doubleValue(), 2.0D);
      BlockIterator.register((int)Math.ceil(this.range.get().doubleValue()), (int)Math.ceil(this.range.get().doubleValue()), (blockPos, blockState) -> {
         if (Utils.squaredDistance(pX, pY, pZ, blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D) > rangeSq)
            return;
         if (this.placeMode.get() == PlaceMode.Floor) {
            if (!this.mc.world.getBlockState(blockPos).isReplaceable())
               return;
            int offset = 1;
            if (this.mc.options.sneakKey.isPressed()) {
               offset = 2;
               this.mc.player.setVelocity(this.mc.player.getVelocity().add(0.0D, -0.5D, 0.0D));
            }
            if (!blockPos.equals(new BlockPos(blockPos.getX(), MathHelper.floor(pY - offset), blockPos.getZ())))
               return;
         } else if (this.placeMode.get() == PlaceMode.StairCase) {
            if (!this.mc.world.getBlockState(blockPos).isReplaceable()) {
               this.stairsList.remove(blockPos);
               return;
            }
            if (!this.stairsList.contains(blockPos) || !this.started)
               return;
         }
         this.blocksList.add(this.blockPosPool.get().set(blockPos));
      });
      BlockIterator.after(() -> {
         this.blocksList.sort(Comparator.comparingDouble((value) -> {
            return Utils.squaredDistance(pX, pY, pZ, (double)value.getX() + 0.5, (double)value.getY() + 0.5, (double)value.getZ() + 0.5);
         }));
         if (this.blocksList.isEmpty()) {
            this.flyHereBlock = null;
            return;
         }
         int count = 0;
         for (BlockPos.Mutable block : this.blocksList) {
            if (count >= this.maxBlocksPerTick.get().intValue())
               break;
            if (this.delay1 < this.delay.get().intValue()) {
               this.delay1++;
               break;
            }
            this.delay1 = 0;
            FindItemResult item = InvUtils.findInHotbar((itemStack) -> {
               return this.validItem(itemStack, block);
            });
            if (!item.found() || item.getHand() == null)
               return;
            if (BlockUtils.place(block, item, this.rotate.get().booleanValue(), 50, this.swingHand.get().booleanValue(), true)) {
               this.flyHereBlock = new BlockPos(block.getX(), block.getY() + 2, block.getZ());
               count++;
               this.renderBlocks.add(this.renderBlockPool.get().set(block, 20));
               if (this.autoFly.get().booleanValue() && this.placeMode.get() == PlaceMode.StairCase) {
                  this.mc.player.setVelocity(Vec3d.ZERO);
                  this.mc.player.setPosition(this.flyHereBlock.getX() + 0.5D, this.flyHereBlock.getY() + 0.5D, this.flyHereBlock.getZ() + 0.5D);
               }
            }
         }
         for (BlockPos.Mutable blockPos : this.blocksList)
            this.blockPosPool.free(blockPos);
         this.blocksList.clear();
      });
   }
   @EventHandler
   private void onSendPacket(PacketEvent.Send event) {
      if (this.autoFly.get() && this.placeMode.get() == PlaceMode.StairCase && !this.mc.player.getAbilities().creativeMode && event.packet instanceof PlayerMoveC2SPacket && !Modules.get().isActive(BetterFlight.class) && !this.mc.player.isFallFlying() && !(this.mc.player.getVelocity().y > -1.0E-4)) {
         ((PlayerMoveC2SPacketAccessor)event.packet).setOnGround(true);
      }
   }

   @EventHandler
   private void onMouseButton(MouseButtonEvent event) {
      if (!this.started && this.mc.player.getMainHandStack().getItem() instanceof BlockItem && this.mc.currentScreen == null && this.placeMode.get() == PlaceMode.StairCase && event.action == KeyAction.Press) {
         if (event.button == 1) {
            this.started = true;
            event.setCancelled(true);
         } else if (event.button == 2) {
            if (this.reverse.get()) {
               this.reverse.set(false);
            } else {
               this.reverse.set(true);
            }
         }

      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.render.get()) {
         this.renderBlocks.sort(Comparator.comparingInt((o) -> {
            return -o.ticks;
         }));
         this.renderBlocks.forEach((renderBlock) -> {
            renderBlock.render(event, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get());
         });
         this.renderOverlapBlocks.sort(Comparator.comparingInt((o) -> {
            return -o.ticks;
         }));
         this.renderOverlapBlocks.forEach((renderBlock) -> {
            renderBlock.render(event, this.overlapColor.get(), this.overlapColor.get(), this.shapeMode.get());
         });
      }
   }

   private boolean validItem(ItemStack itemStack, BlockPos pos) {
      if (!(itemStack.getItem() instanceof BlockItem)) {
         return false;
      } else {
         Block block = ((BlockItem)itemStack.getItem()).getBlock();
         if (!Block.isShapeFullCube(block.getDefaultState().getCollisionShape(this.mc.world, pos))) {
            return false;
         } else {
            return !(block instanceof FallingBlock) || !FallingBlock.canFallThrough(this.mc.world.getBlockState(pos));
         }
      }
   }

   public enum PlaceMode {
      Floor,
      StairCase;

      // $FF: synthetic method
      private static PlaceMode[] $values() {
         return new PlaceMode[]{Floor, StairCase};
      }
   }

   public static class RenderBlock {
      public BlockPos.Mutable pos = new BlockPos.Mutable();
      public int ticksMax;
      public int ticks;

      public RenderBlock set(BlockPos blockPos, int tick) {
         this.pos.set(blockPos);
         this.ticksMax = tick;
         this.ticks = tick;
         return this;
      }

      public void tick() {
         --this.ticks;
      }

      public void render(Render3DEvent event, Color sides, Color lines, ShapeMode shapeMode) {
         double x1 = (double)this.pos.getX() + (double)(this.ticksMax - this.ticks) / (double)(this.ticksMax ^ 2 / this.ticks);
         double y1 = (double)this.pos.getY() + (double)(this.ticksMax - this.ticks) / (double)(this.ticksMax ^ 2 / this.ticks);
         double z1 = (double)this.pos.getZ() + (double)(this.ticksMax - this.ticks) / (double)(this.ticksMax ^ 2 / this.ticks);
         double x2 = (double)(this.pos.getX() + 1) - (double)(this.ticksMax - this.ticks) / (double)(this.ticksMax ^ 2 / this.ticks);
         double y2 = (double)(this.pos.getY() + 1) - (double)(this.ticksMax - this.ticks) / (double)(this.ticksMax ^ 2 / this.ticks);
         double z2 = (double)(this.pos.getZ() + 1) - (double)(this.ticksMax - this.ticks) / (double)(this.ticksMax ^ 2 / this.ticks);
         int preSideA = sides.a;
         int preLineA = lines.a;
         sides.a = (int)((double)sides.a * ((double)this.ticks / (double)this.ticksMax));
         lines.a = (int)((double)lines.a * ((double)this.ticks / (double)this.ticksMax));
         event.renderer.box(x1, y1, z1, x2, y2, z2, sides, lines, shapeMode, 0);
         sides.a = preSideA;
         lines.a = preLineA;
      }
   }
}
