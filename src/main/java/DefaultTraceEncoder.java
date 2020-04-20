import java.io.IOException;

class DefaultTraceEncoder extends BaseContextEncoder {
    private static final int DEFAULT_BUFFER_SIZE = 256;
    static final int REQUIRED_LINE_FEED_ESCAPE = 1;
    private final char entryDelimiter;
    private StringBuilder buffer;
    private FastDateFormat fmt;

    DefaultTraceEncoder(final char entryDelimiter) {
        this.buffer = new StringBuilder(256);
        this.fmt = new FastDateFormat();
        this.entryDelimiter = entryDelimiter;
    }

    @Override
    public void encode(final BaseContext ctx, final EagleEyeAppender eea) throws IOException {
        final char entryDelimiter = this.entryDelimiter;
        final StringBuilder buffer = this.buffer;
        buffer.delete(0, buffer.length());
        this.fmt.formatAndAppendTo(ctx.logTime, buffer);
        buffer.append(entryDelimiter).append(ctx.traceId).append(entryDelimiter).append(ctx.rpcId).append(entryDelimiter).append(ctx.serviceName).append(entryDelimiter).append(ctx.methodName).append(entryDelimiter).append(ctx.logType).append(entryDelimiter).append(ctx.traceName).append(entryDelimiter);
        if (ctx.rpcType == 1) {
            EagleEyeCoreUtils.appendLog(ctx.callBackMsg, buffer, '\0');
        }
        else {
            buffer.append(ctx.callBackMsg);
        }
        buffer.append("\r\n");
        eea.append(buffer.toString());
    }
}
