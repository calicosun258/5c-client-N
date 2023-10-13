package fifthcolumn.n.client;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public final class Input {
   private static final Pattern STRIP_PATTERN = Pattern.compile("(?<!<@)[&§](?i)[0-9a-fklmnorx]");
   private static final Pattern ADD_UUID_PATTERN = Pattern.compile("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)");
   private static final Pattern VALID_MC_NAME = Pattern.compile("^\\w{3,16}$");

   public static String stripMinecraft(String input) {
      return input == null ? "" : StringUtils.trimToEmpty(STRIP_PATTERN.matcher(input).replaceAll(""));
   }

   public static boolean isMinecraftFormatted(String input) {
      return STRIP_PATTERN.matcher(input).matches();
   }

   public static boolean isValidMinecraftUsername(String input) {
      return !isMinecraftFormatted(input) && VALID_MC_NAME.matcher(input).matches();
   }

   public static UUID parseUUID(String possibleUUID) {
      try {
         return UUID.fromString(possibleUUID);
      } catch (IllegalArgumentException var3) {
         Matcher matcher = ADD_UUID_PATTERN.matcher(possibleUUID);
         if (matcher.matches()) {
            return UUID.fromString(matcher.replaceAll("$1-$2-$3-$4-$5"));
         } else {
            throw var3;
         }
      }
   }
}
