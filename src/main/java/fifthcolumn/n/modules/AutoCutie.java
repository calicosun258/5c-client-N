package fifthcolumn.n.modules;

import fifthcolumn.n.copenheimer.CopeService;
import fifthcolumn.n.events.GrieferUpdateEvent;
import java.util.List;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class AutoCutie extends Module {
   private final SettingGroup sgGeneral;
   private final Setting<Boolean> announcer;

   public AutoCutie() {
      super(NAddOn.FIFTH_COLUMN_CATEGORY, "Auto Cutie", "Automatically adds all online griefers to meteor friends");
      this.sgGeneral = this.settings.getDefaultGroup();
      this.announcer = this.sgGeneral.add(new BoolSetting.Builder()
              .name("Announcer")
              .description("Announces new friends in chat")
              .defaultValue(true)
              .build());
   }

   @EventHandler
   private void onGrieferListUpdate(GrieferUpdateEvent event) {
      this.addFriends(event.griefers);
   }

   private void addFriends(List<CopeService.Griefer> griefers) {
      if (this.mc != null && this.mc.player != null && this.mc.world != null) {
         griefers.stream().map((griefer) -> griefer.playerName).filter((playerName) -> !this.mc.player.getEntityName().equalsIgnoreCase(playerName)).forEach(this::addFriend);
      }
   }

   private void addFriend(String playerName) {
      final Friends friends = Friends.get();
      if (this.mc != null && this.mc.world != null && this.mc.player != null) {
         this.mc.world.getPlayers().stream().filter((player) -> playerName.equalsIgnoreCase(player.getEntityName())).findFirst().map(Friend::new).ifPresent((friend) -> {
            if (friends.add(friend) && this.announcer.get()) {
               this.mc.player.sendMessage(Text.of(Formatting.GREEN + "Griefer " + playerName + " friended successfully!"), true);
            }
         });
      }
   }
}
