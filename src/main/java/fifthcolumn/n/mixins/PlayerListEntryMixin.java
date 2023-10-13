package fifthcolumn.n.mixins;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import fifthcolumn.n.NMod;
import fifthcolumn.n.client.ProfileCache;
import fifthcolumn.n.copenheimer.CopeService;
import fifthcolumn.n.modules.LarpModule;
import fifthcolumn.n.modules.StreamerMode;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerListEntry.class})
public abstract class PlayerListEntryMixin {
   @Shadow
   @Final
   private Map<Type, Identifier> textures;
   @Shadow
   @Final
   private GameProfile profile;
   @Shadow
   private @Nullable String model;

   @Inject(
      method = {"getDisplayName"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void n$modifyPlayerDisplayName(CallbackInfoReturnable<Text> cir) {
      if (this.profile.getId().equals(MeteorClient.mc.player.getUuid())) {
         LarpModule larpModule = Modules.get().get(LarpModule.class);
         if (larpModule.isActive()) {
            UUID uuid = UUID.fromString(larpModule.alias.get());
            NMod.profileCache.findByUUID(uuid).ifPresent((gameProfile) -> {
               cir.setReturnValue(Text.of(gameProfile.getName()));
            });
         }
      } else {

         for (CopeService.Griefer griefer : NMod.getCopeService().griefers()) {
            if (this.profile.getId().equals(griefer.playerId)) {
               Optional<GameProfile> profile = NMod.profileCache.findByUUID(griefer.playerId);
               if (profile.isPresent()) {
                  cir.setReturnValue(Text.of(profile.get().getName()));
                  return;
               }
            }
         }

         if (StreamerMode.isGenerifyNames()) {
            String fakeName = NMod.genericNames.getName(this.profile.getId());
            cir.setReturnValue(Text.of(fakeName));
         }
      }

   }

   @Redirect(
      method = {"getSkinTexture"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/PlayerListEntry;loadTextures()V"
)
   )
   private void n$modifyPlayerSkinTexture(PlayerListEntry instance) {
      synchronized(this) {
         if (this.profile.getId().equals(MeteorClient.mc.player.getUuid())) {
            LarpModule larpModule = Modules.get().get(LarpModule.class);
            if (larpModule.isActive()) {
               UUID larpUid = UUID.fromString(larpModule.alias.get());
               NMod.profileCache.texture(larpUid).ifPresent(this::setSkinTexture);
            } else {
               NMod.profileCache.texture(this.profile.getId()).ifPresent(this::setSkinTexture);
            }
         } else {
            List<CopeService.Griefer> griefers = NMod.getCopeService().griefers();
            if (StreamerMode.isGenerifyNames()) {
               this.model = "default";
            }

            for (CopeService.Griefer griefer : griefers) {
               if (this.profile.getId().equals(griefer.playerId)) {
                  Optional<GameProfile> var10000 = NMod.profileCache.findPlayerName(griefer.playerNameAlias);
                  var10000.flatMap(NMod.profileCache::texture).ifPresent(this::setSkinTexture);
               }
            }
         }

      }
   }

   private void setSkinTexture(ProfileCache.TextureResult textureResult) {
      this.textures.put(textureResult.type, textureResult.id);
      if (textureResult.type == Type.SKIN) {
         String modelName = textureResult.texture.getMetadata("model");
         this.model = modelName == null ? "default" : modelName;
      }

   }
}
