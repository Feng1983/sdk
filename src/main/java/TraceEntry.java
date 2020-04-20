import java.util.Collection;
import java.util.Iterator;

public final class TraceEntry {
    private final TraceLogger traceLogger;
    private final BaseContext bizContext;
    private StringBuilder bizAppender;

    TraceEntry(final TraceLogger traceLogger, final String traceId, final String rpcId, final int logType, final String bizKey, final String queryKey) {
        this.bizAppender = null;
        this.traceLogger = traceLogger;
        final BaseContext biz = new BaseContext(traceId, rpcId);
        biz.logType = logType;
        biz.serviceName = EagleEyeCoreUtils.checkNotNullEmpty(bizKey, "bizKey");
        biz.methodName = EagleEyeCoreUtils.checkNotNullEmpty(queryKey, "queryKey");
        biz.logTime = -1L;
        this.bizContext = biz;
    }

    BaseContext getBizContext() {
        return this.bizContext;
    }

    public TraceEntry timestamp(final long timestamp) {
        this.bizContext.logTime = timestamp;
        return this;
    }

    public TraceEntry traceBiz(final int bizType, final String bizValue) {
        if (bizValue == null || bizValue.length() == 0) {
            return this;
        }
        StringBuilder bizAppender = this.bizAppender;
        if (bizAppender == null) {
            bizAppender = (this.bizAppender = new StringBuilder(32));
        }
        else {
            bizAppender.append('&');
        }
        bizAppender.append(bizType).append('=').append(bizValue);
        return this;
    }

    public TraceEntry traceBiz(final int bizType, final Collection<String> bizValues) {
        if (bizValues == null || bizValues.size() == 0) {
            return this;
        }
        StringBuilder bizAppender = this.bizAppender;
        if (bizAppender == null) {
            bizAppender = (this.bizAppender = new StringBuilder(128));
        }
        else {
            bizAppender.append('&');
        }
        final Iterator<String> it = bizValues.iterator();
        bizAppender.append(bizType).append('=').append(it.next());
        while (it.hasNext()) {
            bizAppender.append('&').append(bizType).append('=').append(it.next());
        }
        return this;
    }

    public void logLine(final String content) {
        this.bizContext.callBackMsg = content;
        if (this.bizAppender != null) {
            this.bizContext.traceName = this.bizAppender.toString();
        }
        this.traceLogger.logLine(this.bizContext);
    }

    public void escapeAndLogLine(final String content) {
        this.bizContext.rpcType = 1;
        this.logLine(content);
    }
}
