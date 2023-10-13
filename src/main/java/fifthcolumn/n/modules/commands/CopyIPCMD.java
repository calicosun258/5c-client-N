package fifthcolumn.n.modules.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.command.CommandSource;

public class CopyIPCMD extends Command {
   public CopyIPCMD() {
      super("ip", "Copies the current server IP to clipboard");
   }

   public void build(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes((context) -> {
         ServerInfo serverEntry = MeteorClient.mc.getCurrentServerEntry();
         MeteorClient.mc.keyboard.setClipboard(serverEntry.address);
         this.info("Copied server IP to clipboard!");
         return 1;
      });
   }
}
