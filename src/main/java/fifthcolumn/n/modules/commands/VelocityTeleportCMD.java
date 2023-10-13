package fifthcolumn.n.modules.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.AntiHunger;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

public class VelocityTeleportCMD extends Command {
   public static final List<Module> CONFLICTING_MODULES = List.of(getModule(AntiHunger.class));

   public VelocityTeleportCMD() {
      super("tp", "Jank op-less teleporting");
   }

   public void build(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(argument("x", DoubleArgumentType.doubleArg()).then(argument("y", DoubleArgumentType.doubleArg()).then(argument("z", DoubleArgumentType.doubleArg()).executes(this::runTeleport))));
   }

   private int runTeleport(CommandContext<CommandSource> context) {
      assert MeteorClient.mc.player != null;

      double x = context.getArgument("x", Double.class);
      double y = context.getArgument("y", Double.class);
      double z = context.getArgument("z", Double.class);
      System.out.println("Teleporting to " + x + ", " + y + ", " + z);
      ClientPlayerEntity player = MeteorClient.mc.player;
      double selfX = player.getX();
      double selfY = player.getY();
      double selfZ = player.getZ();
      double distance = Math.pow(Math.pow(x - selfX, 2.0) + Math.pow(y - selfY, 2.0) + Math.pow(z - selfZ, 2.0), 0.5);
      double packetsNeeded = Math.ceil(distance / 0.038 - 1.54);
      float yaw = (float)(Math.atan2(x - selfX, z - selfZ) * 57.29577951308232);
      float pitch = (float)Math.asin((y - selfY) / distance);
      System.out.println("Traveling " + distance + " blocks, need " + packetsNeeded + " packets.");
      List<Module> modules = disengageConflictingModules();
      player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, true));
      player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, Mode.START_SPRINTING));

      for(double i = 0.0; i < packetsNeeded; ++i) {
         player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(selfX, selfY - 1.0E-9, selfZ, true));
         player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(selfX, selfY + 1.0E-9, selfZ, false));
      }

      player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
      reengageModules(modules);
      return 1;
   }

   public static List<Module> disengageConflictingModules() {
      List<Module> modules = CONFLICTING_MODULES.stream().filter(Module::isActive).collect(Collectors.toList());
      modules.forEach(Module::toggle);
      return modules;
   }

   public static void reengageModules(List<Module> modules) {
      modules.forEach(Module::toggle);
   }

   private static Module getModule(Class<? extends Module> module) {
      return Modules.get().get(module);
   }
}
