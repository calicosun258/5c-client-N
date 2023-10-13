package fifthcolumn.n.mixins;

import fifthcolumn.n.modules.FastProjectile;
import fifthcolumn.n.modules.Gun;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayerInteractionManager.class})
public class ClientPlayerInteractionManagerMixin {
   @Inject(
      at = {@At("HEAD")},
      method = {"stopUsingItem"}
   )
   private void n$onStopUsingItem(PlayerEntity player, CallbackInfo ci) {
      FastProjectile fp = Modules.get().get(FastProjectile.class);
      if (fp.shouldEngage()) {
         fp.engage();
      }

      Gun gun = Modules.get().get(Gun.class);
      if (gun.shouldShoot()) {
         gun.shoot();
      }

   }
}
