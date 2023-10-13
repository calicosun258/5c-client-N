package fifthcolumn.n.mixins;

import fifthcolumn.n.NMod;
import fifthcolumn.n.copenheimer.CopeService;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractClientPlayerEntity.class})
public abstract class AbstractClientPlayerEntityMixin {
   @Inject(
      method = {"getCapeTexture"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void n$modifyCapeTexture(CallbackInfoReturnable<Identifier> info) {
      PlayerEntity entity = (PlayerEntity)(Object)this;
      CopeService copeService = NMod.getCopeService();
      if (MeteorClient.mc.player != null && MeteorClient.mc.player.getEntityName().equals(entity.getEntityName()) || copeService.griefers().stream().anyMatch((griefer) -> entity.getEntityName().equals(griefer.playerName))) {
         info.setReturnValue(NMod.CAPE_TEXTURE);
      }

   }
}
