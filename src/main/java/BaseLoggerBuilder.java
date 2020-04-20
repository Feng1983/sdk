class BaseLoggerBuilder <T extends BaseLoggerBuilder<T>>{
    protected final String loggerName;
    protected String filePath;
    protected long maxFileSize;
    protected char entryDelimiter;
    protected int maxBackupIndex;

    BaseLoggerBuilder(final String loggerName) {
        this.filePath = null;
        this.maxFileSize = 314572800L;
        this.entryDelimiter = '|';
        this.maxBackupIndex = 3;
        this.loggerName = loggerName;
    }

    public T logFilePath(final String logFilePath) {
        return this.configLogFilePath(logFilePath, EagleEye.EAGLEEYE_LOG_DIR);
    }

    public T appFilePath(final String appFilePath) {
        return this.configLogFilePath(appFilePath, EagleEye.APP_LOG_DIR);
    }

    public T baseLogFilePath(final String baseLogFilePath) {
        return this.configLogFilePath(baseLogFilePath, EagleEye.BASE_LOG_DIR);
    }

    private T configLogFilePath(String filePathToConfig, final String basePath) {
        EagleEyeCoreUtils.checkNotNullEmpty(filePathToConfig, "filePath");
        if (filePathToConfig.charAt(0) != '/') {
            filePathToConfig = basePath + filePathToConfig;
        }
        this.filePath = filePathToConfig;
        return (T)this;
    }

    public T maxFileSizeMB(final long maxFileSizeMB) {
        if (this.maxFileSize < 10L) {
            throw new IllegalArgumentException("\u8bbe\u7f6e\u6587\u4ef6\u5927\u5c0f\u81f3\u5c11\u8981 10MB: " + maxFileSizeMB);
        }
        this.maxFileSize = maxFileSizeMB * 1024L * 1024L;
        return (T)this;
    }

    public T maxBackupIndex(final int maxBackupIndex) {
        if (maxBackupIndex < 1) {
            throw new IllegalArgumentException("\u5f52\u6863\u6570\u91cf\u81f3\u5c11\u4e3a 1: " + maxBackupIndex);
        }
        this.maxBackupIndex = maxBackupIndex;
        return (T)this;
    }

    public T entryDelimiter(final char entryDelimiter) {
        this.entryDelimiter = entryDelimiter;
        return (T)this;
    }

    String getLoggerName() {
        return this.loggerName;
    }
}
