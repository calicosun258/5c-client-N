package fifthcolumn.n.mixins;

import fifthcolumn.n.modules.LarpModule;
import fifthcolumn.n.modules.StreamerMode;
import java.util.Optional;
import meteordevelopment.meteorclient.systems.modules.render.Nametags;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({Nametags.class})
public abstract class NameTagMixin {
   @ModifyVariable(
      method = {"renderNametagPlayer(Lmeteordevelopment/meteorclient/events/render/Render2DEvent;Lnet/minecraft/entity/player/PlayerEntity;Z)V"},
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private PlayerEntity n$modifyPlayerNametag(final PlayerEntity player) {
      return new PlayerEntity(player.getWorld(), player.getBlockPos(), player.getYaw(), player.getGameProfile()) {
         public String getEntityName() {
            Optional<String> playerEntityName = LarpModule.getPlayerEntityName(player);
            if (playerEntityName.isEmpty()) {
               playerEntityName = StreamerMode.getPlayerEntityName(player);
            }

            return (String)playerEntityName.orElse(player.getEntityName());
         }

         public boolean isSpectator() {
            return player.isSpectator();
         }

         public boolean isCreative() {
            return player.isCreative();
         }
      };
   }
}
