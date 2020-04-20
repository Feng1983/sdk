class AtpTraceClient {

    private static final TraceLogger traceLogger;
    private static final String DUMP_SIGN = "dump";
    private static final String DUMP_VALUE = "1";
    private static final char ATP_PARAM_SEPARATOR = '\u0012';
    private static final char ATP_OBJECT_SEPARATOR = '\u0014';
    private static final int DEFAULT_BUFFER_SIZE = 4096;

    public static void trace(final String appId, final String operationKey, final Object obj, final Object... params) {
        trace("1".equals(EagleEye.getUserData("dump")), appId, operationKey, obj, params);
    }

    static void trace(final boolean enabled, final String appId, final String operationKey, final Object obj, final Object... params) {
        if (enabled && EagleEye.isLogDumpEnabled()) {
            final StringBuilder appender = new StringBuilder(4096);
            try {
                if (null != params && params.length > 0) {
                    appendObj(params[0], appender);
                    for (int i = 1; i < params.length; ++i) {
                        appender.append('\u0012');
                        appendObj(params[i], appender);
                    }
                }
                appender.append('\u0014');
                appendObj(obj, appender);
                AtpTraceClient.traceLogger.logLine(appId, operationKey, appender.toString());
            }
            catch (Exception e) {
                EagleEye.selfLog("[WARN] AtpTraceClient exception, appId=" + appId + ", operationKey=" + operationKey, e);
            }
        }
    }

    private static void appendObj(final Object obj, final StringBuilder appender) {
        if (Void.TYPE == obj) {
            appender.append("VOID");
        }
        else if (null == obj) {
            appender.append("NULL");
        }
        else {
            appender.append(EagleEyeJSONImpl.toJSONString(obj));
        }
    }

    static {
        traceLogger = EagleEye.traceLogger("atp");
    }
}
