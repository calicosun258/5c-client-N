package fifthcolumn.n.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import java.io.File;
import java.net.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.UserCache;

public class ProfileCache {
   private final Map<UUID, TextureResult> textureMap = new ConcurrentHashMap();
   private final UserCache cache;

   public ProfileCache() {
      this.cache = new UserCache((new YggdrasilAuthenticationService(Proxy.NO_PROXY)).createProfileRepository(), new File(MinecraftClient.getInstance().runDirectory, "larp-cache.json"));
   }

   public Optional<GameProfile> findPlayerName(String name) {
      return name == null ? Optional.empty() : this.cache.findByName(name);
   }

   public Optional<GameProfile> findByUUID(UUID uuid) {
      return this.cache.getByUuid(uuid);
   }

   public Optional<TextureResult> texture(GameProfile larpProfile) {
      return this.texture(larpProfile.getId());
   }

   public Optional<TextureResult> texture(UUID larpUid) {
      if (this.textureMap.containsKey(larpUid)) {
         return Optional.of(this.textureMap.get(larpUid));
      } else {
         MinecraftClient.getInstance().getSkinProvider().loadSkin(new GameProfile(larpUid, "a user"), (type, id, texture) -> {
            this.textureMap.put(larpUid, new TextureResult(type, id, texture));
         }, true);
         return Optional.empty();
      }
   }

   public static class TextureResult {
      public final MinecraftProfileTexture.Type type;
      public final Identifier id;
      public final MinecraftProfileTexture texture;

      public TextureResult(MinecraftProfileTexture.Type type, Identifier id, MinecraftProfileTexture texture) {
         this.type = type;
         this.id = id;
         this.texture = texture;
      }
   }
}
