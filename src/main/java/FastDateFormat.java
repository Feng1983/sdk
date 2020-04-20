import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

class FastDateFormat {
     private static final long TIMEZONE_GMT_ADD_8_OFFSET = 28800000L;
     private static final long CURRENT_TIMEZONE_OFFSET;
     private final SimpleDateFormat fmt;
     private char[] buffer;
     private long lastSecond;

     FastDateFormat() {
         this.fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
         this.buffer = new char[23];
         this.lastSecond = -1L;
     }

     public String format(final long timestamp) {
         this.formatToBuffer(timestamp);
         return new String(this.buffer, 0, 23);
     }

     public String format(final Date date) {
         return this.format(date.getTime());
     }

     public void formatAndAppendTo(final long timestamp, final StringBuilder appender) {
         this.formatToBuffer(timestamp);
         appender.append(this.buffer, 0, 23);
     }

     private void formatToBuffer(final long timestamp) {
         final long ts = timestamp - FastDateFormat.CURRENT_TIMEZONE_OFFSET;
         final long second = ts / 1000L;
         if (second == this.lastSecond) {
             int ms = (int)(ts % 1000L);
             this.buffer[22] = (char)(ms % 10 + 48);
             ms /= 10;
             this.buffer[21] = (char)(ms % 10 + 48);
             this.buffer[20] = (char)(ms / 10 + 48);
         }
         else {
             final String result = this.fmt.format(new Date(ts));
             result.getChars(0, result.length(), this.buffer, 0);
         }
     }

     String formatWithoutMs(final long timestamp) {
         final long ts = timestamp - FastDateFormat.CURRENT_TIMEZONE_OFFSET;
         final long second = ts / 1000L;
         if (second != this.lastSecond) {
             final String result = this.fmt.format(new Date(ts));
             result.getChars(0, result.length(), this.buffer, 0);
         }
         return new String(this.buffer, 0, 19);
     }

     static {
         CURRENT_TIMEZONE_OFFSET = TimeZone.getDefault().getOffset(0L) + TimeZone.getDefault().getDSTSavings() - 28800000L;
     }
}
