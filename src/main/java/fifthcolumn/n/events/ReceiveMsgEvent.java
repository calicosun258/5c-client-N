package fifthcolumn.n.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.network.message.MessageType;
import net.minecraft.text.Text;

public class ReceiveMsgEvent extends Cancellable {
   private static final ReceiveMsgEvent INSTANCE = new ReceiveMsgEvent();
   private Text message;
   private String sender;
   private MessageType.Parameters params;
   private boolean modified;

   public static ReceiveMsgEvent get(Text message, String sender, MessageType.Parameters params) {
      INSTANCE.setCancelled(false);
      INSTANCE.message = message;
      INSTANCE.sender = sender;
      INSTANCE.params = params;
      INSTANCE.modified = false;
      return INSTANCE;
   }

   public Text getMessage() {
      return this.message;
   }

   public String getSender() {
      return this.sender;
   }

   public MessageType.Parameters getParams() {
      return this.params;
   }

   public void setMessage(Text message) {
      this.message = message;
      this.modified = true;
   }

   public boolean isModified() {
      return this.modified;
   }
}
