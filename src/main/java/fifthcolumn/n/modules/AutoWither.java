package fifthcolumn.n.modules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class AutoWither extends Module {
   private final SettingGroup sgGeneral;
   private final SettingGroup sgRender;
   private final Setting<Double> range;
   private final Setting<Integer> delay;
   private final Setting<Boolean> nametag;
   private final Setting<Boolean> swingHand;
   private final Setting<Boolean> rotate;
   private final Setting<Boolean> render;
   private final Setting<SettingColor> lineColor;
   private final Setting<SettingColor> sideColor;
   private final Setting<ShapeMode> shapeMode;
   private final Pool<BlockPos.Mutable> blockPosPool;
   private final List<BlockPos.Mutable> blocksList;
   private final Pool<RenderBlock> renderBlockPool;
   private final List<RenderBlock> renderBlocks;
   private final List<BlockPos> sandList;
   private final List<BlockPos> headList;
   private HitResult hitResult;
   private boolean started;
   private boolean head;
   private int d;

   public AutoWither() {
      super(NAddOn.FIFTH_COLUMN_CATEGORY, "AutoWither", "WITHER DEEZ!!!");
      this.sgGeneral = this.settings.getDefaultGroup();
      this.sgRender = this.settings.createGroup("Render");
      this.range = this.sgGeneral.add(new DoubleSetting.Builder()
              .name("range")
              .description("Custom range to place at.")
              .defaultValue(5.0).min(1.0).sliderMax(6.0).build());
      this.delay = this.sgGeneral.add(new IntSetting.Builder()
              .name("delay")
              .description("Delay")
              .defaultValue(1).min(0).sliderMax(5)
              .build());
      this.nametag = this.sgGeneral.add((new BoolSetting.Builder())
              .name("nametag")
              .description("will nametag withers if present in hotbar")
              .defaultValue(true)
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
      this.sideColor = this.sgRender.add(new ColorSetting.Builder()
              .name("side-color")
              .description("The color of the sides of the blocks being rendered.")
              .visible(this.render::get)
              .defaultValue(new SettingColor(204, 0, 0, 10)).build());
      this.shapeMode = this.sgRender.add(new EnumSetting.Builder<ShapeMode>()
              .name("shape-mode")
              .description("How the shapes are rendered.")
              .visible(this.render::get)
              .defaultValue(ShapeMode.Both)
              .build());

      this.blockPosPool = new Pool<>(BlockPos.Mutable::new);
      this.blocksList = new ArrayList<>();
      this.renderBlockPool = new Pool<>(RenderBlock::new);
      this.renderBlocks = new ArrayList<>();
      this.sandList = new ArrayList<>();
      this.headList = new ArrayList<>();
      this.started = false;
      this.head = false;
      this.d = 0;
   }

   public void onActivate() {
      super.onActivate();

      for (RenderBlock renderBlock : this.renderBlocks) {
         this.renderBlockPool.free(renderBlock);
      }

      this.renderBlocks.clear();
      this.started = false;
      this.head = false;
      this.sandList.clear();
      this.headList.clear();
   }

   public void onDeactivate() {
      super.onDeactivate();

      for (RenderBlock renderBlock : this.renderBlocks) {
         this.renderBlockPool.free(renderBlock);
      }

      this.renderBlocks.clear();
      this.started = false;
      this.head = false;
      this.sandList.clear();
      this.headList.clear();
   }

   @EventHandler
   private void onTickPre(TickEvent.Pre event) {
      double pX = this.mc.player.getX();
      double pY = this.mc.player.getY();
      double pZ = this.mc.player.getZ();
      this.renderBlocks.forEach(RenderBlock::tick);
      this.renderBlocks.removeIf((renderBlock) -> renderBlock.ticks <= 0);

      for (BlockPos pos : this.sandList) {
         this.renderBlocks.add(this.renderBlockPool.get().set(pos));
      }

      for (BlockPos element : this.headList) {
         BlockPos pos = element;
         this.renderBlocks.add(this.renderBlockPool.get().set(pos));
      }

      if (this.nametag.get()) {

         for (Entity entity : this.mc.world.getEntities()) {
            if (entity.getType() == EntityType.WITHER && !(Utils.distance(pX, pY, pZ, entity.getX(), entity.getY(), entity.getZ()) > this.range.get()) && !entity.hasCustomName()) {
               FindItemResult item = InvUtils.findInHotbar(this::isNameTag);
               if (!item.found()) {
                  break;
               }

               if (item.getHand() == null) {
                  this.mc.player.getInventory().selectedSlot = item.slot();
                  return;
               }

               String text = this.mc.player.getMainHandStack().getName().getString();
               if (!EntityUtils.getName(entity).equals(text)) {
                  this.interact(entity);
                  return;
               }
            }
         }
      }

      if (!this.started) {
         CardinalDirection[] var18 = CardinalDirection.values();
         int var22 = var18.length;

         for (CardinalDirection dir : var18) {
            if (dir.toDirection() == this.mc.player.getHorizontalFacing()) {
               this.sandList.clear();
               this.headList.clear();
               this.hitResult = this.mc.getCameraEntity().raycast(this.mc.interactionManager.getReachDistance(), 0.0F, false);
               if (!(this.mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
                  break;
               }

               Block block = ((BlockItem) this.mc.player.getMainHandStack().getItem()).getBlock();
               if (!(this.hitResult instanceof BlockHitResult) || block != Blocks.SOUL_SAND) {
                  break;
               }

               BlockPos startBlock = ((BlockHitResult) this.hitResult).getBlockPos();
               int ns = 0;
               int ew = 0;
               switch (dir) {
                  case East:
                  case West:
                     ns = 1;
                     break;
                  default:
                     ew = 1;
               }

               this.sandList.add(new BlockPos(startBlock.getX(), startBlock.getY() + 1, startBlock.getZ()));
               this.sandList.add(new BlockPos(startBlock.getX(), startBlock.getY() + 2, startBlock.getZ()));
               this.sandList.add(new BlockPos(startBlock.getX() + ew, startBlock.getY() + 2, startBlock.getZ() + ns));
               this.sandList.add(new BlockPos(startBlock.getX() - ew, startBlock.getY() + 2, startBlock.getZ() - ns));
               this.headList.add(new BlockPos(startBlock.getX() + ew, startBlock.getY() + 3, startBlock.getZ() + ns));
               this.headList.add(new BlockPos(startBlock.getX() - ew, startBlock.getY() + 3, startBlock.getZ() - ns));
               this.headList.add(new BlockPos(startBlock.getX(), startBlock.getY() + 3, startBlock.getZ()));


               for (BlockPos pos : this.sandList) {
                  if (!this.mc.world.getBlockState(pos).isReplaceable()) {
                     this.sandList.clear();
                     this.headList.clear();
                     return;
                  }
               }

               for (BlockPos pos : this.headList) {
                  if (!this.mc.world.getBlockState(pos).isReplaceable()) {
                     this.sandList.clear();
                     this.headList.clear();
                     return;
                  }
               }
            }
         }
      } else {
         if (this.sandList.isEmpty() && !this.head) {
            this.head = true;
         }

         if (this.headList.isEmpty()) {
            this.head = false;
            this.started = false;
            FindItemResult item1 = InvUtils.findInHotbar((itemStack) -> this.validItem(itemStack, new BlockPos(0, 0, 0), false));
            if (!item1.found()) {
               return;
            }

            if (item1.getHand() == null) {
               this.mc.player.getInventory().selectedSlot = item1.slot();
               return;
            }
         }
      }

      double rangeSq = Math.pow(this.range.get(), 2.0);
      BlockIterator.register((int)Math.ceil(this.range.get()), (int)Math.ceil(this.range.get()), (blockPos, blockState) -> {
         if (this.started && !(Utils.squaredDistance(pX, pY, pZ, (double)blockPos.getX() + 0.5, (double)blockPos.getY() + 0.5, (double)blockPos.getZ() + 0.5) > rangeSq) && (this.sandList.contains(blockPos) || this.headList.contains(blockPos))) {
            if (this.mc.world.getBlockState(blockPos).isReplaceable()) {
               this.blocksList.add(this.blockPosPool.get().set(blockPos));
            } else {
               if (this.head && this.headList.contains(blockPos)) {
                  this.headList.remove(blockPos);
               } else this.sandList.remove(blockPos);

            }
         }
      });

      BlockIterator.after(() -> {
         this.blocksList.sort(Comparator.comparingDouble((value) -> {
            return Utils.squaredDistance(pX, pY, pZ, (double)value.getX() + 0.5, (double)value.getY() + 0.5, (double)value.getZ() + 0.5);
         }));
         if (this.blocksList.isEmpty())
            return;
         for (BlockPos.Mutable block : this.blocksList) {
            boolean isHead = this.headList.contains(block);
            FindItemResult item = InvUtils.findInHotbar((itemStack) -> this.validItem(itemStack, block, isHead));
            if (!item.found())
               return;
            if (item.getHand() == null) {
               (this.mc.player.getInventory()).selectedSlot = item.slot();
               return;
            }
            if (this.d > 0) {
               this.d--;
               return;
            }
            this.d = this.delay.get().intValue();
            if (BlockUtils.place(block, item, this.rotate.get().booleanValue(), 50, this.swingHand.get().booleanValue(), true)) {
               this.renderBlocks.add(this.renderBlockPool.get().set(block));
               if (this.headList.size() == 1)
                  this.headList.clear();
               break;
            }
         }
         for (BlockPos.Mutable blockPos : this.blocksList)
            this.blockPosPool.free(blockPos);
         this.blocksList.clear();
      });
   }

   @EventHandler
   private void onMouseButton(MouseButtonEvent event) {
      if (this.mc.player.getMainHandStack().getItem() instanceof BlockItem && this.mc.currentScreen == null && !this.started && event.action == KeyAction.Press && event.button == 1 && ((BlockItem)this.mc.player.getMainHandStack().getItem()).getBlock() == Blocks.SOUL_SAND) {
         this.started = true;
         event.setCancelled(true);
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
      }
   }

   private boolean validItem(ItemStack itemStack, BlockPos pos, boolean isHead) {
      if (!(itemStack.getItem() instanceof BlockItem)) {
         return false;
      } else {
         Block block = ((BlockItem)itemStack.getItem()).getBlock();
         if (isHead) {
            if (block != Blocks.WITHER_SKELETON_SKULL) {
               return false;
            }
         } else if (block != Blocks.SOUL_SAND) {
            return false;
         }

         return !(block instanceof FallingBlock) || !FallingBlock.canFallThrough(this.mc.world.getBlockState(pos));
      }
   }

   private boolean isNameTag(ItemStack itemStack) {
      return itemStack.getItem().equals(Items.NAME_TAG);
   }

   private void interact(Entity entity) {
      if (this.rotate.get()) {
         Rotations.rotate(Rotations.getYaw(entity), Rotations.getPitch(entity), -100, () -> {
            this.mc.interactionManager.interactEntity(this.mc.player, entity, Hand.MAIN_HAND);
         });
      } else {
         this.mc.interactionManager.interactEntity(this.mc.player, entity, Hand.MAIN_HAND);
      }

   }

   public static class RenderBlock {
      public BlockPos.Mutable pos = new BlockPos.Mutable();
      public int ticks;

      public RenderBlock set(BlockPos blockPos) {
         this.pos.set(blockPos);
         this.ticks = 8;
         return this;
      }

      public void tick() {
         --this.ticks;
      }

      public void render(Render3DEvent event, Color sides, Color lines, ShapeMode shapeMode) {
         int preSideA = sides.a;
         int preLineA = lines.a;
         sides.a = (int)((double)sides.a * ((double)this.ticks / 8.0));
         lines.a = (int)((double)lines.a * ((double)this.ticks / 8.0));
         event.renderer.box(this.pos, sides, lines, shapeMode, 0);
         sides.a = preSideA;
         lines.a = preLineA;
      }
   }
}
