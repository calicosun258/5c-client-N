package fifthcolumn.n.mixins;

import com.mojang.authlib.GameProfile;
import fifthcolumn.n.NMod;
import fifthcolumn.n.modules.StreamerMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerListHud.class})
public class PlayerListHudMixin {
   @Shadow
   @Final
   private MinecraftClient client;
   @Shadow
   @Final
   private static Comparator<PlayerListEntry> ENTRY_ORDERING;

   @Inject(
      method = {"collectPlayerEntries()Ljava/util/List;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void n$addFakePlayerListing(CallbackInfoReturnable<List<PlayerListEntry>> cir) {
      if (this.client.player != null) {
         int fakePlayers = StreamerMode.addFakePlayers();
         List<PlayerListEntry> players = new ArrayList(this.client.player.networkHandler.getListedPlayerListEntries().stream().sorted(ENTRY_ORDERING).limit(80L - (long)fakePlayers).toList());

         for(int i = 0; i < fakePlayers; ++i) {
            UUID uuid = UUID.nameUUIDFromBytes(("FakePlayer" + (i + 1)).getBytes());
            GameProfile fakeProfile = new GameProfile(uuid, NMod.genericNames.getName(uuid));
            players.add(new PlayerListEntry(fakeProfile, false));
         }

         cir.setReturnValue(players);
      }

   }
}
