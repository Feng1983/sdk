import java.util.ArrayList;
import java.util.List;

final class EagleEyeCoreUtils {
    public static final String EMPTY_STRING = "";
    public static final String NEWLINE = "\r\n";
    public static final String[] EMPTY_STRING_ARRAY;
    private static final String LOCAL_IP_ADDRESS;
    private static final ThreadLocal<FastDateFormat> dateFmt;

    public static boolean isBlank(final String str) {
        final int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; ++i) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String checkNotNullEmpty(final String value, final String name) throws IllegalArgumentException {
        if (isBlank(value)) {
            throw new IllegalArgumentException(name + " is null or empty");
        }
        return value;
    }

    public static <T> T checkNotNull(final T value, final String name) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException(name + " is null");
        }
        return value;
    }

    public static <T> T defaultIfNull(final T value, final T defaultValue) {
        return (value == null) ? defaultValue : value;
    }

    public static boolean isNotBlank(final String str) {
        return !isBlank(str);
    }

    public static String trim(final String str) {
        return (str == null) ? null : str.trim();
    }

    public static String[] split(final String str, final char separatorChar) {
        return splitWorker(str, separatorChar, false);
    }

    private static String[] splitWorker(final String str, final char separatorChar, final boolean preserveAllTokens) {
        if (str == null) {
            return null;
        }
        final int len = str.length();
        if (len == 0) {
            return EagleEyeCoreUtils.EMPTY_STRING_ARRAY;
        }
        final List<String> list = new ArrayList<String>();
        int i = 0;
        int start = 0;
        boolean match = false;
        boolean lastMatch = false;
        while (i < len) {
            if (str.charAt(i) == separatorChar) {
                if (match || preserveAllTokens) {
                    list.add(str.substring(start, i));
                    match = false;
                    lastMatch = true;
                }
                start = ++i;
            }
            else {
                lastMatch = false;
                match = true;
                ++i;
            }
        }
        if (match || (preserveAllTokens && lastMatch)) {
            list.add(str.substring(start, i));
        }
        return list.toArray(new String[list.size()]);
    }

    public static StringBuilder appendWithBlankCheck(final String str, final String defaultValue, final StringBuilder appender) {
        if (isNotBlank(str)) {
            appender.append(str);
        }
        else {
            appender.append(defaultValue);
        }
        return appender;
    }

    public static StringBuilder appendWithNullCheck(final Object obj, final String defaultValue, final StringBuilder appender) {
        if (obj != null) {
            appender.append(obj.toString());
        }
        else {
            appender.append(defaultValue);
        }
        return appender;
    }

    public static StringBuilder appendLog(final String str, final StringBuilder appender, final char delimiter) {
        if (str != null) {
            final int len = str.length();
            appender.ensureCapacity(appender.length() + len);
            for (int i = 0; i < len; ++i) {
                char c = str.charAt(i);
                if (c == '\n' || c == '\r' || c == delimiter) {
                    c = ' ';
                }
                appender.append(c);
            }
        }
        return appender;
    }

    public static String filterInvalidCharacters(final String str) {
        final StringBuilder appender = new StringBuilder(str.length());
        return appendLog(str, appender, '|').toString();
    }

    public static String digest(final String str) {
        final CRC32 crc = new CRC32();
        crc.update(str.getBytes());
        return Long.toHexString(crc.getValue());
    }

    public static String formatTime(final long timestamp) {
        return EagleEyeCoreUtils.dateFmt.get().format(timestamp);
    }

    private static String getLocalInetAddress() {
        try {
            final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress address = null;
            while (interfaces.hasMoreElements()) {
                final NetworkInterface ni = interfaces.nextElement();
                final Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address.getHostAddress().indexOf(":") == -1) {
                        return address.getHostAddress();
                    }
                }
            }
        }
        catch (Throwable t) {}
        return "127.0.0.1";
    }

    public static String getLocalAddress() {
        return EagleEyeCoreUtils.LOCAL_IP_ADDRESS;
    }

    public static boolean isClusterTestEnabled(final RpcContext_inner ctx) {
        return ctx != null && "1".equals(ctx.getUserData("t"));
    }

    static {
        EMPTY_STRING_ARRAY = new String[0];
        LOCAL_IP_ADDRESS = getLocalInetAddress();
        dateFmt = new ThreadLocal<FastDateFormat>() {
            @Override
            protected FastDateFormat initialValue() {
                return new FastDateFormat();
            }
        };
    }
}
