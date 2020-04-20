public  final class TraceLogger {
    private final String loggerName;
    private final AsyncAppender appender;

    TraceLogger(final String loggerName, final AsyncAppender appender) {
        this.loggerName = loggerName;
        this.appender = appender;
    }

    public String getLoggerName() {
        return this.loggerName;
    }

    AsyncAppender getAppender() {
        return this.appender;
    }

    public TraceEntry trace(final String bizKey, final String queryKey) {
        return this.traceWithContext(EagleEye.createContextIfNotExists(false), bizKey, queryKey);
    }

    public TraceEntry traceWithContext(final RpcContext_inner ctx, final String bizKey, final String queryKey) {
        String traceId;
        String rpcId;
        int logType;
        if (ctx != null) {
            traceId = ctx.getTraceId();
            rpcId = ctx.getRpcId();
            logType = (EagleEyeCoreUtils.isClusterTestEnabled(ctx) ? 1 : 0);
        }
        else {
            traceId = "";
            rpcId = "";
            logType = 0;
        }
        return this.traceWithContext(traceId, rpcId, logType, bizKey, queryKey);
    }

    public TraceEntry traceWithContext(final String traceId, final String rpcId, final int logType, final String bizKey, final String queryKey) {
        return new TraceEntry(this, traceId, rpcId, logType, bizKey, queryKey);
    }

    public void logLine(final String bizKey, final String queryKey, final String logContent) {
        this.logLineWithContext(EagleEye.createContextIfNotExists(false), bizKey, queryKey, logContent);
    }

    void logLineWithContext(final RpcContext_inner ctx, final String bizKey, final String queryKey, final String logContent) {
        final BaseContext biz = new BaseContext(ctx.getTraceId(), ctx.getRpcId());
        biz.logType = (EagleEyeCoreUtils.isClusterTestEnabled(ctx) ? 1 : 0);
        biz.serviceName = EagleEyeCoreUtils.checkNotNullEmpty(bizKey, "bizKey");
        biz.methodName = EagleEyeCoreUtils.checkNotNullEmpty(queryKey, "queryKey");
        biz.callBackMsg = logContent;
        this.logLine(biz);
    }

    void logLine(final BaseContext biz) {
        if (!EagleEye.isBizOff()) {
            final String bizKey = biz.serviceName;
            final String queryKey = biz.methodName;
            if (bizKey == null || bizKey.length() >= 4096 || queryKey == null || queryKey.length() >= 4096) {
                EagleEye.selfLog("[WARN] TraceLogger[" + this.loggerName + "] not logged " + "for bizKey or query key is invalid");
                return;
            }
            final String logContent = biz.callBackMsg;
            if (logContent == null) {
                EagleEye.selfLog("[WARN] TraceLogger[" + this.loggerName + "] not logged " + "for logContent is null, bizKey=" + bizKey + ", queryKey=" + queryKey);
                return;
            }
            if (biz.logTime <= 0L) {
                biz.logTime = System.currentTimeMillis();
            }
            if (logContent.length() > 4096) {
                EagleEye.selfLog("[WARN] TraceLogger[" + this.loggerName + "] logContent is too long, len=" + logContent.length() + ", bizKey=" + bizKey + ", queryKey=" + queryKey);
                this.appender.append(biz);
                this.appender.flushAndWait();
                return;
            }
            this.appender.append(biz);
        }
    }
}
