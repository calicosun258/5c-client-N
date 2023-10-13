package fifthcolumn.n.mixins;

import fifthcolumn.n.events.PlayerSpawnPositionEvent;
import fifthcolumn.n.events.SpawnPlayerEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerSpawnPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerSpawnS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {ClientPlayNetworkHandler.class},
   priority = 100
)
public class ClientPlayNetworkHandlerMixin {
   @Inject(
      method = {"onPlayerSpawnPosition"},
      at = {@At("TAIL")}
   )
   private void n$playerSpawnPositionEvent(PlayerSpawnPositionS2CPacket packet, CallbackInfo cinfo) {
      MeteorClient.EVENT_BUS.post(new PlayerSpawnPositionEvent(packet.getPos()));
   }

   @Inject(
      method = {"onPlayerSpawn"},
      at = {@At("TAIL")}
   )
   private void n$playerSpawnEvent(PlayerSpawnS2CPacket packet, CallbackInfo ci) {
      MeteorClient.EVENT_BUS.post(new SpawnPlayerEvent(packet.getPlayerUuid(), new BlockPos((int)packet.getX(), (int)packet.getY(), (int)packet.getZ())));
   }
}
