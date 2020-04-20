import java.util.HashMap;
import java.util.Map;

public final  class TraceLoggerBuilder extends BaseLoggerBuilder<TraceLoggerBuilder>{
    private static final Map<String, TraceLogger> traceLoggers;
    private EagleEyeAppender appender;
    private int asyncQueueSize;
    private int maxWaitMillis;

    TraceLoggerBuilder(final String loggerName) {
        super(loggerName);
        this.appender = null;
        this.asyncQueueSize = 4096;
        this.maxWaitMillis = 0;
    }

    TraceLoggerBuilder appender(final EagleEyeAppender appender) {
        this.appender = appender;
        return this;
    }

    TraceLogger create() {
        String filePath;
        if (this.filePath == null) {
            filePath = EagleEye.EAGLEEYE_LOG_DIR + "trace-" + this.loggerName + ".log";
        }
        else if (this.filePath.endsWith("/") || this.filePath.endsWith("\\")) {
            filePath = this.filePath + "trace-" + this.loggerName + ".log";
        }
        else {
            filePath = this.filePath;
        }
        EagleEyeAppender appender = this.appender;
        if (appender == null) {
            final EagleEyeRollingFileAppender rfappender = new EagleEyeRollingFileAppender(filePath, this.maxFileSize);
            rfappender.setMaxBackupIndex(this.maxBackupIndex);
            appender = rfappender;
        }
        AsyncAppender asyncAppender;
        if (appender instanceof AsyncAppender) {
            asyncAppender = (AsyncAppender)appender;
        }
        else {
            asyncAppender = new AsyncAppender(this.asyncQueueSize, this.maxWaitMillis);
            asyncAppender.start(appender, new DefaultTraceEncoder(this.entryDelimiter), "TraceLog-" + this.loggerName);
        }
        EagleEyeLogDaemon.watch(asyncAppender);
        return new TraceLogger(this.loggerName, asyncAppender);
    }

    public TraceLoggerBuilder asyncQueueSize(final int asyncQueueSize) {
        if (asyncQueueSize < 128) {
            throw new IllegalArgumentException("\u8bbe\u7f6e\u65e5\u5fd7\u5f02\u6b65\u961f\u5217\u7684\u5927\u5c0f\u4e0d\u80fd\u5c0f\u4e8e 128: " + asyncQueueSize);
        }
        this.asyncQueueSize = asyncQueueSize;
        return this;
    }

    public TraceLoggerBuilder maxWaitMillis(final int maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
        return this;
    }

    public TraceLogger buildSingleton() {
        synchronized (TraceLoggerBuilder.class) {
            TraceLogger traceLogger = TraceLoggerBuilder.traceLoggers.get(this.loggerName);
            if (traceLogger == null) {
                traceLogger = this.create();
                TraceLoggerBuilder.traceLoggers.put(this.loggerName, traceLogger);
                EagleEye.selfLog("[INFO] created traceLogger[" + traceLogger.getLoggerName() + "]: " + traceLogger.getAppender());
            }
            return traceLogger;
        }
    }

    static {
        traceLoggers = new HashMap<String, TraceLogger>();
    }
}
