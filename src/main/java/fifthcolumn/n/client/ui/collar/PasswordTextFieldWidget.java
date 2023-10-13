package fifthcolumn.n.client.ui.collar;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class PasswordTextFieldWidget extends TextFieldWidget {
   public PasswordTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
      super(textRenderer, x, y, width, height, text);
      this.setRenderTextProvider((string, integer) -> {
         return OrderedText.styledForwardsVisitedString("*".repeat(string.length()), Style.EMPTY);
      });
   }

   public PasswordTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, @Nullable TextFieldWidget copyFrom, Text text) {
      super(textRenderer, x, y, width, height, copyFrom, text);
   }
}
