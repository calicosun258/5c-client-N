package fifthcolumn.n.modules;

import fifthcolumn.n.utils.BlockPosUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Scaffold;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;

public class BetterFlight extends Module {
   private final SettingGroup sgGeneral;
   private final Setting<Double> speed;
   private final Setting<Double> maxSpeed;
   private final Setting<Boolean> velocitySpeed;
   private final Setting<AntiKickMode> antiKickModeSetting;
   private float speedDelta;

   public BetterFlight() {
      super(NAddOn.FIFTH_COLUMN_CATEGORY, "Better Flight", "Fly like a motherfucker");
      this.sgGeneral = this.settings.getDefaultGroup();
      this.speed = this.sgGeneral.add(new DoubleSetting.Builder()
              .name("base speed")
              .description("Your speed when you start to fly.")
              .defaultValue(1.0).min(0.0)
              .build());
      this.maxSpeed = this.sgGeneral.add(new DoubleSetting.Builder()
              .name("maximum speed")
              .description("The maximum speed used for velocity speed")
              .defaultValue(5.0).min(0.0).max(10.0)
              .build());
      this.velocitySpeed = this.sgGeneral.add(new BoolSetting.Builder()
              .name("velocity speed")
              .description("increases the velocity the longer you fly")
              .defaultValue(true)
              .build());
      this.antiKickModeSetting = this.sgGeneral.add(new EnumSetting.Builder<AntiKickMode>()
              .name("anti-kick mode")
              .description("method for anti-kick")
              .defaultValue(AntiKickMode.PACKET)
              .build());
      this.speedDelta = 0.0F;
   }

   @EventHandler
   private void onSendPacket(PacketEvent.Send event) {
      if (event.packet instanceof ClientCommandC2SPacket) {
         event.cancel();
      }

   }

   public void onActivate() {
      super.onActivate();
      if (!this.mc.player.getAbilities().creativeMode) {
         this.mc.player.getAbilities().allowFlying = true;
      }
   }

   public void onDeactivate() {
      super.onDeactivate();
      if (!this.mc.player.getAbilities().creativeMode) {
         this.mc.player.getAbilities().allowFlying = false;
      }
   }

   @EventHandler
   private void onPostTick(TickEvent.Post ignored) {
      if (this.isActive()) {
         if (this.velocitySpeed.get() && this.isMoving()) {
            this.speedDelta += 0.1F;
         }

         if (!this.isMoving() && this.speedDelta > 0.0F) {
            this.speedDelta = 0.0F;
            this.mc.player.setVelocity(Vec3d.ZERO);
         }

         float speed;
         if (!Modules.get().get(Scaffold.class).isActive() && !Modules.get().get(BuildPoop.class).isActive()) {
            if (this.velocitySpeed.get()) {
               speed = this.speed.get().floatValue() * this.speedDelta;
               float maxSpeed = this.maxSpeed.get().floatValue();
               if (speed >= maxSpeed) {
                  speed = maxSpeed;
               }
            } else {
               speed = this.speed.get().floatValue();
            }
         } else {
            speed = 0.8F;
         }

         Vec3d antiKickVel = this.getAntiKickVec();
         this.mc.player.setVelocity(antiKickVel);
         Vec3d forward = (new Vec3d(0.0, 0.0, speed)).rotateY(-((float)Math.toRadians(this.mc.player.getYaw())));
         Vec3d strafe = forward.rotateY((float)Math.toRadians(90.0));
         if (this.mc.options.jumpKey.isPressed()) {
            if (speed == 0.0F) {
               this.mc.player.setVelocity(this.mc.player.getVelocity().add(0.0, this.speed.get().floatValue(), 0.0));
            } else {
               this.mc.player.setVelocity(this.mc.player.getVelocity().add(0.0, speed, 0.0));
            }
         }

         if (this.mc.options.sneakKey.isPressed()) {
            if (speed == 0.0F) {
               this.mc.player.setVelocity(this.mc.player.getVelocity().add(0.0, -this.speed.get().floatValue(), 0.0));
            } else {
               this.mc.player.setVelocity(this.mc.player.getVelocity().add(0.0, -speed, 0.0));
            }
         }

         if (this.mc.options.backKey.isPressed()) {
            this.mc.player.setVelocity(this.mc.player.getVelocity().add(-forward.x, 0.0, -forward.z));
         }

         if (this.mc.options.forwardKey.isPressed()) {
            this.mc.player.setVelocity(this.mc.player.getVelocity().add(forward.x, 0.0, forward.z));
         }

         if (this.mc.options.leftKey.isPressed()) {
            this.mc.player.setVelocity(this.mc.player.getVelocity().add(strafe.x, 0.0, strafe.z));
         }

         if (this.mc.options.rightKey.isPressed()) {
            this.mc.player.setVelocity(this.mc.player.getVelocity().add(-strafe.x, 0.0, -strafe.z));
         }

      }
   }

   private Vec3d getAntiKickVec() {
      Vec3d var10000;
      Vec3d position;
      switch (this.antiKickModeSetting.get()) {
         case NONE:
            var10000 = Vec3d.ZERO;
            break;
         case FALL:
            position = this.mc.player.getPos().add(0.0, -0.069, 0.0);
            BlockPos blockPos = BlockPosUtils.from(position);
            var10000 = this.mc.player.age % 7 == 0 && this.mc.world.getBlockState(blockPos).isReplaceable() ? Vec3d.ZERO.add(0.0, -0.069, 0.0) : Vec3d.ZERO;
            break;
         case BOB:
            if (this.mc.player.age % 40 == 0) {
               position = this.mc.player.getPos().add(0.0, 0.15, 0.0);
               if (this.mc.world.getBlockState(BlockPosUtils.from(position)).isReplaceable()) {
                  var10000 = Vec3d.ZERO.add(0.0, 0.15, 0.0);
                  break;
               }
            } else if (this.mc.player.age % 20 == 0) {
               position = this.mc.player.getPos().add(0.0, -0.15, 0.0);
               if (this.mc.world.getBlockState(BlockPosUtils.from(position)).isReplaceable()) {
                  var10000 = Vec3d.ZERO.add(0.0, -0.15, 0.0);
                  break;
               }
            }

            var10000 = Vec3d.ZERO;
            break;
         case PACKET:
            if (this.mc.player.age % 20 == 0) {
               this.mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(this.mc.player.getX(), this.mc.player.getY() - 0.069, this.mc.player.getZ(), false));
               this.mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(this.mc.player.getX(), this.mc.player.getY() + 0.069, this.mc.player.getZ(), true));
            }

            var10000 = Vec3d.ZERO;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   private boolean isMoving() {
      return this.mc.options.sneakKey.isPressed() || this.mc.options.backKey.isPressed() || this.mc.options.forwardKey.isPressed() || this.mc.options.leftKey.isPressed() || this.mc.options.rightKey.isPressed();
   }

   public enum AntiKickMode {
      NONE,
      FALL,
      BOB,
      PACKET;

      // $FF: synthetic method
      private static AntiKickMode[] $values() {
         return new AntiKickMode[]{NONE, FALL, BOB, PACKET};
      }
   }
}
