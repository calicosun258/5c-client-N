package fifthcolumn.n.modules;

import com.mojang.authlib.GameProfile;
import fifthcolumn.n.NMod;
import fifthcolumn.n.copenheimer.CopeService;
import java.util.Optional;
import java.util.UUID;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

public class LarpModule extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

   public final Setting<String> alias = this.sgGeneral.add(new StringSetting.Builder()
           .name("player uid")
           .description("player uuid to larp as")
           .defaultValue("24b82429-15f2-4d7f-91f6-d277a1858949")
           .build());

   public final Setting<String> aliasName = this.sgGeneral.add(new StringSetting.Builder()
           .name("player name")
           .description("player name to larp as")
           .defaultValue("orsond").build());

   public LarpModule() {
      super(NAddOn.FIFTH_COLUMN_CATEGORY, "Larping", "Make all griefers larp as another player");
   }

   public static @Nullable String modifyPlayerNameInstances(String text) {

      for (CopeService.Griefer entity : NMod.getCopeService().griefers()) {
         if (entity.playerNameAlias != null) {
            Optional<GameProfile> profile = NMod.profileCache.findPlayerName(entity.playerNameAlias);
            if (profile.isPresent()) {
               text = StringUtils.replace(text, entity.playerName, entity.playerNameAlias);
            }
         }
      }

      if (MeteorClient.mc != null && MeteorClient.mc.player != null) {
         LarpModule larpModule = Modules.get().get(LarpModule.class);
         if (larpModule.isActive()) {
            String aliasName = larpModule.aliasName.get();
            text = StringUtils.replace(text, MeteorClient.mc.player.getEntityName(), aliasName);
         }
      }

      return text;
   }

   public static Optional<String> getPlayerEntityName(PlayerEntity player) {
      if (MeteorClient.mc.player != null && player.getGameProfile().getId().equals(MeteorClient.mc.player.getUuid())) {
         LarpModule larpModule = Modules.get().get(LarpModule.class);
         if (larpModule.isActive()) {
            UUID uuid = UUID.fromString(larpModule.alias.get());
            Optional<GameProfile>profile = NMod.profileCache.findByUUID(uuid);
            if (profile.isPresent()) {
               return profile.map(GameProfile::getName);
            }
         }
      } else {
         for (CopeService.Griefer griefer : NMod.getCopeService().griefers()) {
            if (player.getGameProfile().getId().equals(griefer.playerId)) {
               Optional<GameProfile> profile = NMod.profileCache.findByUUID(griefer.playerId);
               if (profile.isPresent()) {
                  return profile.map(GameProfile::getName);
               }
            }
         }
      }

      return Optional.empty();
   }
}
