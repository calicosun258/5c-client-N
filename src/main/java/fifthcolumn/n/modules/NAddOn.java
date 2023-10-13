package fifthcolumn.n.modules;

import fifthcolumn.n.modules.commands.CopyIPCMD;
import fifthcolumn.n.modules.commands.VanityTagCMD;
import fifthcolumn.n.modules.commands.VelocityTeleportCMD;
import fifthcolumn.n.modules.hud.SocialEngineeringHud;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;

public class NAddOn extends MeteorAddon {
   public static final Category FIFTH_COLUMN_CATEGORY;

   public String getPackage() {
      return "fifthcolumn.n";
   }

   public void onInitialize() {
      Modules modules = Modules.get();
      modules.add(new AutoSign());
      modules.add(new BanEvasion());
      modules.add(new InventoryDupe());
      modules.add(new BetterFlight());
      modules.add(new StreamerMode());
      modules.add(new BuildPoop());
      modules.add(new AutoWither());
      modules.add(new FastProjectile());
      modules.add(new GrieferTracer());
      modules.add(new WaypointSync());
      modules.add(new AutoCutie());
      modules.add(new LecternCrash());
      modules.add(new AutoLava());
      modules.add(new AntiAim());
      modules.add(new Gun());
      modules.add(new ChestStealerAura());
      modules.add(new AutoTranslate());
      modules.add(new OriginsModule());
      modules.add(new SitBypass());
      modules.add(new LarpModule());
      modules.add(new GameModeNotifier());
      Commands.add(new CopyIPCMD());
      Commands.add(new VanityTagCMD());
      Commands.add(new VelocityTeleportCMD());
      Hud.get().register(SocialEngineeringHud.INFO);
   }

   public void onRegisterCategories() {
      Modules.registerCategory(FIFTH_COLUMN_CATEGORY);
   }

   static {
      FIFTH_COLUMN_CATEGORY = new Category("5c", Items.TNT.getDefaultStack());
   }
}
