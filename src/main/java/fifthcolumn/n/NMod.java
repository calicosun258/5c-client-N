package fifthcolumn.n;

import fifthcolumn.n.client.ProfileCache;
import fifthcolumn.n.client.ui.copenheimer.servers.CopeMultiplayerScreen;
import fifthcolumn.n.collar.CollarLogin;
import fifthcolumn.n.copenheimer.CopeService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import meteordevelopment.meteorclient.MeteorClient;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.NameGenerator;

public class NMod implements ModInitializer {
   private static final Pattern STRIP_PATTERN = Pattern.compile("(?<!<@)[&§](?i)[0-9a-fklmnorx]");
   private static NMod INSTANCE;
   public static final CopeService copeService = new CopeService();
   public static final Identifier CAPE_TEXTURE = new Identifier("nc:cape.png");
   public static final Identifier cockSound = new Identifier("nc:cock");
   public static final Identifier shotgunSound = new Identifier("nc:shot");
   public static SoundEvent shotgunSoundEvent;
   public static SoundEvent cockSoundEvent;
   public static ProfileCache profileCache;
   public static GenericNames genericNames;
   private CopeMultiplayerScreen multiplayerScreen;

   public void onInitialize() {
      MinecraftClient mc = MinecraftClient.getInstance();
      INSTANCE = new NMod();
      ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
         copeService.clearTranslations();
         copeService.startUpdating();
         copeService.setLastServerInfo(mc.getCurrentServerEntry());
      });
      ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
         copeService.clearTranslations();
         copeService.stopUpdating();
         copeService.setDefaultSession();
         genericNames.clear();
      });
      copeService.setDefaultSession(mc.getSession());
      CollarLogin.refreshSession();
      Registry.register(Registries.SOUND_EVENT, shotgunSound, shotgunSoundEvent);
      Registry.register(Registries.SOUND_EVENT, cockSound, cockSoundEvent);
   }

   public static CopeService getCopeService() {
      return copeService;
   }

   public static CopeMultiplayerScreen getMultiplayerScreen() {
      return INSTANCE.multiplayerScreen;
   }

   public static CopeMultiplayerScreen getOrCreateMultiplayerScreen(Screen parent) {
      if (INSTANCE.multiplayerScreen == null) {
         INSTANCE.multiplayerScreen = new CopeMultiplayerScreen(parent, copeService);
      }

      return INSTANCE.multiplayerScreen;
   }

   public static void setMultiplayerScreen(CopeMultiplayerScreen multiplayerScreen) {
      INSTANCE.multiplayerScreen = multiplayerScreen;
   }

   public static boolean is2b2t() {
      ServerInfo serverEntry = MeteorClient.mc.getCurrentServerEntry();
      return serverEntry != null && serverEntry.address.contains("2b2t.org");
   }

   static {
      shotgunSoundEvent = SoundEvent.of(shotgunSound);
      cockSoundEvent = SoundEvent.of(cockSound);
      profileCache = new ProfileCache();
      genericNames = new GenericNames();
   }

   public static class GenericNames {
      private final Map<UUID, String> names = new HashMap<>();

      public String getName(UUID uuid) {
         this.names.computeIfAbsent(uuid, (k) -> NameGenerator.name(uuid));
         return this.names.get(uuid);
      }

      public void clear() {
         this.names.clear();
      }
   }
}
