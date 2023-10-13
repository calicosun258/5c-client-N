package fifthcolumn.n.modules;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.ServerConnectEndEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ServerInfo;

public class BanEvasion extends Module {
   private final SettingGroup sgGeneral;
   public ServerInfo lastServer;
   private final Setting<Boolean> addSpacesToName;
   public final Setting<Boolean> evadeAndReconnect;

   public BanEvasion() {
      super(NAddOn.FIFTH_COLUMN_CATEGORY, "Ban Evasion", "Options for evading bans");
      this.sgGeneral = this.settings.getDefaultGroup();
      this.addSpacesToName = this.sgGeneral.add(new BoolSetting.Builder()
              .name("add spaces to name")
              .description("makes it easy to evade bans")
              .defaultValue(false)
              .build());
      this.evadeAndReconnect = this.sgGeneral.add(new BoolSetting.Builder()
              .name("Toggle from evade and reconnect button")
              .description("makes it easy to evade bans")
              .defaultValue(true).build());
      MeteorClient.EVENT_BUS.subscribe(new StaticListener());
   }

   public static boolean isSpacesToNameEnabled() {
      BanEvasion banEvasion = Modules.get().get(BanEvasion.class);
      return banEvasion != null && banEvasion.isActive() && banEvasion.addSpacesToName.get();
   }

   private class StaticListener {
      @EventHandler
      private void onConnectToServer(ServerConnectEndEvent event) {
         BanEvasion.this.lastServer = BanEvasion.this.mc.isInSingleplayer() ? null : BanEvasion.this.mc.getCurrentServerEntry();
      }
   }
}
