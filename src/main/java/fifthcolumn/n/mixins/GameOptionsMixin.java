package fifthcolumn.n.mixins;

import fifthcolumn.n.modules.StreamerMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin({GameOptions.class})
public class GameOptionsMixin {
   @ModifyArg(
      method = {"sendClientSettings"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V"
)
   )
   private Packet<?> n$forceDisableServerListing(Packet<?> packet) {
      if (StreamerMode.isStreaming()) {
         ClientSettingsC2SPacket p = (ClientSettingsC2SPacket)packet;
         return new ClientSettingsC2SPacket(p.language(), p.viewDistance(), p.chatVisibility(), p.chatColors(), p.playerModelBitMask(), p.mainArm(), p.filterText(), false);
      } else {
         return packet;
      }
   }
}
