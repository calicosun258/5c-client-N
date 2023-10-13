package fifthcolumn.n.modules;

import fifthcolumn.n.NMod;
import fifthcolumn.n.copenheimer.CopeService;
import java.util.Iterator;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

public class GrieferTracer extends Module {
   private final SettingGroup sgGeneral;
   private final Setting<SettingColor> playersColor;

   public GrieferTracer() {
      super(NAddOn.FIFTH_COLUMN_CATEGORY, "Griefer Tracer", "Tracers to fellow Griefers. Disabled on 2b2t.org.");
      this.sgGeneral = this.settings.getDefaultGroup();
      this.playersColor = this.sgGeneral.add(((ColorSetting.Builder)((ColorSetting.Builder)(new ColorSetting.Builder()).name("players-colors")).description("The griefers color.")).defaultValue(new SettingColor(205, 205, 205, 127)).build());
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      Iterator var2 = NMod.getCopeService().griefers().iterator();

      while(var2.hasNext()) {
         CopeService.Griefer entity = (CopeService.Griefer)var2.next();
         if (entity.location != null) {
            System.out.println(entity.playerName + " is at " + entity.location);
            Color color = (Color)this.playersColor.get();
            double x = entity.location.x;
            double y = entity.location.y;
            double z = entity.location.z;
            event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, x, y, z, color);
         }
      }

   }
}
