package fifthcolumn.n.modules;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;

public class OriginsModule extends Module {
   private final SettingGroup sgGeneral;
   public final Setting<String> versionSetting;

   public OriginsModule() {
      super(NAddOn.FIFTH_COLUMN_CATEGORY, "Origins compat", "You will need to set a version");
      this.sgGeneral = this.settings.getDefaultGroup();
      this.versionSetting = this.sgGeneral.add((new StringSetting.Builder()).name("version").description("version of origins").defaultValue("1.7.1").build());
   }

   public int[] getSemVer() {
      String VERSION = this.versionSetting.get();
      if (VERSION.contains("+")) {
         VERSION = VERSION.split("\\+")[0];
      }

      if (VERSION.contains("-")) {
         VERSION = VERSION.split("-")[0];
      }

      String[] splitVersion = VERSION.split("\\.");
      int[] semver = new int[splitVersion.length];

      for(int i = 0; i < semver.length; ++i) {
         semver[i] = Integer.parseInt(splitVersion[i]);
      }

      return semver;
   }
}
