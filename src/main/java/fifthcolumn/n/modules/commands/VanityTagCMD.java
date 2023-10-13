package fifthcolumn.n.modules.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;

public class VanityTagCMD extends Command {
   public VanityTagCMD() {
      super("column", "Sends discord link in chat", new String[0]);
   }

   public void build(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes((context) -> {
         ChatUtils.sendPlayerMsg("https://discord.gg/thefifthcolumn");
         return 1;
      });
   }
}
