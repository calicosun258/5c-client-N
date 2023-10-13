package fifthcolumn.n.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class ChestStealerAura extends Module {
   private final SettingGroup sgGeneral;
   private final SettingGroup sgRender;
   private final Setting<Double> range;
   private final Setting<List<Item>> items;
   private final Setting<Mode> mode;
   private final Setting<Boolean> render;
   private final Setting<SettingColor> lineColor;
   private final Setting<SettingColor> sideColor;
   private final Setting<ShapeMode> shapeMode;
   private final Pool<BlockPos.Mutable> blockPosPool;
   private final List<BlockPos.Mutable> blocksList;
   private final List<BlockPos> openedChestList;
   private final Pool<RenderBlock> renderBlockPool;
   private final List<RenderBlock> renderBlocks;
   private BlockPos chestBlock;
   private int delay1;
   private int delay2;
   private int delay3;
   private boolean isChested;
   private String address;

   public ChestStealerAura() {
      super(NAddOn.FIFTH_COLUMN_CATEGORY, "ChestStealerAura", "steal2win");
      this.sgGeneral = this.settings.getDefaultGroup();
      this.sgRender = this.settings.createGroup("Render");
      this.range = this.sgGeneral.add(new DoubleSetting.Builder()
              .name("range")
              .description("range to steal")
              .defaultValue(4.5).min(1.0).max(6.0)
              .build());
      this.items = this.sgGeneral.add(new ItemListSetting.Builder()
              .name("items")
              .description("items to steal")
              .build());
      this.mode = this.sgGeneral.add(new EnumSetting.Builder<Mode>()
              .name("mode")
              .defaultValue(Mode.Aura)
              .build());
      this.render = this.sgRender.add(new BoolSetting.Builder()
              .name("render")
              .description("shows what chests have been stolen from")
              .defaultValue(true)
              .build());
      this.lineColor = this.sgRender.add(new ColorSetting.Builder().name("line-color").description("The color of the lines of the blocks being rendered.").visible(this.render::get).defaultValue(new SettingColor(204, 0, 0, 255)).build());
      Objects.requireNonNull(this.render);
      this.sideColor = this.sgRender.add(new ColorSetting.Builder().name("side-color").description("The color of the sides of the blocks being rendered.").visible(this.render::get).defaultValue(new SettingColor(204, 0, 0, 10)).build());
      Objects.requireNonNull(this.render);
      this.shapeMode = this.sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").visible(this.render::get).defaultValue(ShapeMode.Both).build());

      this.blockPosPool = new Pool<>(BlockPos.Mutable::new);
      this.blocksList = new ArrayList<>();
      this.openedChestList = new ArrayList<>();

      this.renderBlockPool = new Pool<>(RenderBlock::new);
      this.renderBlocks = new ArrayList<>();

      this.chestBlock = null;
      this.delay1 = 0;
      this.delay2 = 0;
      this.delay3 = 0;
      this.isChested = false;
      this.address = null;
   }

   public void onActivate() {
      super.onActivate();

      for (RenderBlock renderBlock : this.renderBlocks) {
         this.renderBlockPool.free(renderBlock);
      }

      this.renderBlocks.clear();
   }

   public void onDeactivate() {
      super.onDeactivate();

      for (RenderBlock renderBlock : this.renderBlocks) {
         this.renderBlockPool.free(renderBlock);
      }

      this.renderBlocks.clear();
   }

   @EventHandler
   private void onTickPre(TickEvent.Pre event) {
      if (this.mc.getCurrentServerEntry() != null && !this.mc.getCurrentServerEntry().address.equals(this.address)) {
         this.address = this.mc.getCurrentServerEntry().address;
         this.openedChestList.clear();
      }

      this.renderBlocks.forEach(RenderBlock::tick);
      this.renderBlocks.removeIf((renderBlock) -> renderBlock.ticks <= 0);

      for (BlockPos c : this.openedChestList) {
         this.renderBlocks.add(this.renderBlockPool.get().set(c, 1));
      }

      if (this.delay3 >= 1) {
         --this.delay3;
      } else if (this.isChested) {
         if (this.mc.currentScreen instanceof GenericContainerScreen) {
            GenericContainerScreenHandler container = ((GenericContainerScreen)this.mc.currentScreen).getScreenHandler();
            Inventory inv = container.getInventory();
            if (!inv.isEmpty() && inv.containsAny(new HashSet(this.items.get()))) {
               for(int i = 0; i < inv.size(); ++i) {
                  if (this.items.get().contains(inv.getStack(i).getItem())) {
                     InvUtils.quickSwap().slotId(i);
                     this.delay3 = 2;
                     return;
                  }
               }

            } else {
               this.isChested = false;
               this.openedChestList.add(this.chestBlock);
               Direction dir = this.getDirectionToOtherChestHalf(this.mc.world.getBlockState(this.chestBlock));
               if (dir != null) {
                  switch (dir) {
                     case NORTH:
                        this.openedChestList.add(new BlockPos(this.chestBlock.getX(), this.chestBlock.getY(), this.chestBlock.getZ() - 1));
                        break;
                     case SOUTH:
                        this.openedChestList.add(new BlockPos(this.chestBlock.getX(), this.chestBlock.getY(), this.chestBlock.getZ() + 1));
                        break;
                     case EAST:
                        this.openedChestList.add(new BlockPos(this.chestBlock.getX() + 1, this.chestBlock.getY(), this.chestBlock.getZ()));
                        break;
                     case WEST:
                        this.openedChestList.add(new BlockPos(this.chestBlock.getX() - 1, this.chestBlock.getY(), this.chestBlock.getZ()));
                  }
               }

               this.mc.player.closeScreen();
               this.delay2 = 5;
            }
         } else {
            this.isChested = false;
            this.mc.player.closeScreen();
            this.delay2 = 3;
         }
      } else if (this.delay2 >= 0) {
         --this.delay2;
      } else if (this.mode.get() == Mode.Aura) {
         double pX = this.mc.player.getX();
         double pY = this.mc.player.getY();
         double pZ = this.mc.player.getZ();
         double rangeSq = Math.pow(this.range.get(), 2.0);
         BlockIterator.register((int)Math.ceil(this.range.get()), (int)Math.ceil(this.range.get()), (blockPos, blockState) -> {
            if (!(Utils.squaredDistance(pX, pY, pZ, (double)blockPos.getX() + 0.5, (double)blockPos.getY() + 0.5, (double)blockPos.getZ() + 0.5) > rangeSq) && (this.mc.world.getBlockState(blockPos).getBlock() == Blocks.CHEST || this.mc.world.getBlockState(blockPos).getBlock() == Blocks.BARREL) && !this.openedChestList.contains(blockPos)) {
               this.blocksList.add(this.blockPosPool.get().set(blockPos));
            }
         });
         BlockIterator.after(() -> {
            this.blocksList.sort(Comparator.comparingDouble((value) -> {
               return Utils.squaredDistance(pX, pY, pZ, (double)value.getX() + 0.5, (double)value.getY() + 0.5, (double)value.getZ() + 0.5);
            }));
            if (!this.blocksList.isEmpty()) {
               int count = 0;

               for (BlockPos.Mutable block : this.blocksList) {
                  if (count >= 1) {
                     break;
                  }

                  if (this.delay1 < 3) {
                     ++this.delay1;
                     break;
                  }

                  this.delay1 = 0;
                  if (this.mc.interactionManager.interactBlock(this.mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d((double) block.getX() + 0.5, (double) block.getY() + 0.5, (double) block.getZ() + 0.5), Direction.UP, block, true)).isAccepted()) {
                     ++count;
                     this.chestBlock = new BlockPos(block.getX(), block.getY(), block.getZ());
                     this.isChested = true;
                  }
               }

               for (BlockPos.Mutable block : this.blocksList) {
                  this.blockPosPool.free(block);
               }

               this.blocksList.clear();
            }
         });
      }
   }

   @EventHandler
   private void onInteractBlock(InteractBlockEvent event) {
      if (this.mode.get() == Mode.Manual) {
         this.chestBlock = event.result.getBlockPos();
         this.isChested = true;
      }
   }

   public WWidget getWidget(GuiTheme theme) {
      WButton butt = theme.button("Reset opened chests list");
      List var10001 = this.openedChestList;
      Objects.requireNonNull(var10001);
      butt.action = var10001::clear;
      return butt;
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.render.get()) {
         this.renderBlocks.sort(Comparator.comparingInt((o) -> -o.ticks));
         this.renderBlocks.forEach((renderBlock) -> {
            renderBlock.render(event, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get());
         });
      }
   }

   private Direction getDirectionToOtherChestHalf(BlockState blockState) {
      ChestType chestType;
      try {
         chestType = blockState.get(ChestBlock.CHEST_TYPE);
      } catch (IllegalArgumentException var4) {
         return null;
      }

      return chestType == ChestType.SINGLE ? null : ChestBlock.getFacing(blockState);
   }

   public enum Mode {
      Aura,
      Manual
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
         int preSideA = sides.a;
         int preLineA = lines.a;
         sides.a = (int)((double)sides.a * ((double)this.ticks / (double)this.ticksMax));
         lines.a = (int)((double)lines.a * ((double)this.ticks / (double)this.ticksMax));
         event.renderer.box(this.pos, sides, lines, shapeMode, 0);
         sides.a = preSideA;
         lines.a = preLineA;
      }
   }
}
