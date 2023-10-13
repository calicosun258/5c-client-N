package fifthcolumn.n.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class AutoLava extends Module {
   private final SettingGroup sgGeneral;
   private final Setting<Double> distance;
   private final Setting<Integer> tickInterval;
   private final Setting<Boolean> rotate;
   private Entity entity;
   private int ticks;

   public AutoLava() {
      super(NAddOn.FIFTH_COLUMN_CATEGORY, "AutoLava", "do it");
      this.sgGeneral = this.settings.getDefaultGroup();
      this.distance = this.sgGeneral.add(new DoubleSetting.Builder()
              .name("distance")
              .description("how far")
              .min(0.0).max(4.5)
              .defaultValue(4.5)
              .build());
      this.tickInterval = this.sgGeneral.add(new IntSetting.Builder()
              .name("tick-interval")
              .defaultValue(5)
              .build());
      this.rotate = this.sgGeneral.add(new BoolSetting.Builder()
              .name("rotate")
              .description("rot own u")
              .defaultValue(true)
              .build());
      this.ticks = 0;
   }

   public void onDeactivate() {
      this.entity = null;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      this.entity = null;
      ++this.ticks;

      for (Entity entity : this.mc.world.getEntities()) {
         if (!(entity instanceof PlayerEntity) || this.mc.player.distanceTo(entity) > this.distance.get().doubleValue() || entity == this.mc.player || entity
                 .isInLava())
            continue;
         this.entity = entity;
         Vec3d go = new Vec3d(entity.prevX + entity.getX() - entity.prevX, entity.prevY + entity.getY() - entity.prevY, entity.prevZ + entity.getZ() - entity.prevZ);
         Rotations.rotate(Rotations.getYaw(go), Rotations.getPitch(go), 100, this::placeLava);
         toggle();
         return;
      }
   }

   private void placeLava() {
      FindItemResult findItemResult = InvUtils.findInHotbar(Items.LAVA_BUCKET);
      if (!findItemResult.found()) {
         this.error("No lava bucket found.");
         this.toggle();
      } else {
         int prevSlot = this.mc.player.getInventory().selectedSlot;
         this.mc.player.getInventory().selectedSlot = findItemResult.slot();
         this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
         this.mc.player.getInventory().selectedSlot = prevSlot;
      }
   }
}
