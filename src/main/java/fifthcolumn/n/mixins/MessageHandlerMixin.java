package fifthcolumn.n.mixins;

import com.mojang.authlib.GameProfile;
import fifthcolumn.n.events.ReceiveMsgEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MessageHandler.class})
public class MessageHandlerMixin {
   @Inject(
      method = {"onChatMessage"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void n$msgReceiveEvent(SignedMessage msg, GameProfile sender, MessageType.Parameters p, CallbackInfo ci) {
      if (MeteorClient.EVENT_BUS.post(ReceiveMsgEvent.get(msg.getContent(), sender.getName(), p)).isCancelled()) {
         ci.cancel();
      }

   }
}
