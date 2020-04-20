import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class EagleEye {
    static final String USER_HOME;
    static final String BASE_LOG_DIR;
    static final String EAGLEEYE_LOG_DIR;
    static final String APP_LOG_DIR;
    static final Charset DEFAULT_CHARSET;
    static final String EAGLEEYE_RPC_LOG_FILE;
    static final String EAGLEEYE_BIZ_LOG_FILE;
    static final String EAGLEEYE_SELF_LOG_FILE;
    static final long MAX_SELF_LOG_FILE_SIZE = 209715200L;
    static final long MAX_RPC_LOG_FILE_SIZE = 314572800L;
    static final long MAX_BIZ_LOG_FILE_SIZE = 314572800L;
    public static final int MAX_BIZ_LOG_SIZE = 4096;
    public static final int MAX_INDEX_SIZE = 512;
    public static final int MAX_USER_DATA_ENTRY_SIZE = 512;
    public static final int MAX_USER_DATA_TOTAL_SIZE = 1024;
    static AsyncAppender rpcAppender;
    static AsyncAppender bizAppender;
    static TraceLogger bizEagleEyeLogger;
    static EagleEyeAppender selfAppender;
    static AtomicInteger exceptionThreshold;
    static int MAX_EXCEPTION_COUNT;
    public static final String ROOT_RPC_ID = "0";
    public static final String MAL_ROOT_RPC_ID = "9";
    @Deprecated
    public static final int TYPE_START_TRACE = 1;
    @Deprecated
    public static final int TYPE_END_TRACE = 2;
    @Deprecated
    public static final int TYPE_START_RPC = 3;
    @Deprecated
    public static final int TYPE_END_RPC = 4;
    @Deprecated
    public static final int TYPE_ANNOTATE_RPC = 5;
    @Deprecated
    public static final int TYPE_ANNOTATE_TRACE = 6;
    static final int LOG_TYPE_BIZ = 0;
    static final int LOG_TYPE_TRACE = 1;
    static final int LOG_TYPE_RPC_CLIENT = 2;
    static final int LOG_TYPE_RPC_SERVER = 3;
    static final int LOG_TYPE_RPC_LOG = 4;
    static final int LOG_TYPE_INDEX = 5;
    static final int LOG_TYPE_EVENT_ILLEGAL = -255;
    public static final int TYPE_TRACE = 0;
    public static final int TYPE_HSF_CLIENT = 1;
    public static final int TYPE_HSF_SERVER = 2;
    public static final int TYPE_NOTIFY = 3;
    public static final int TYPE_TDDL = 4;
    public static final int TYPE_TAIR = 5;
    public static final int TYPE_SEARCH = 6;
    public static final int TYPE_INDEX = 9;
    public static final int TYPE_DUBBO_CLIENT = 11;
    public static final int TYPE_DUBBO_SERVER = 12;
    public static final int TYPE_METAQ = 13;
    public static final int TYPE_TFS = 15;
    public static final int TYPE_ALIPAY = 16;
    public static final int TYPE_HTTP_CLIENT = 25;
    public static final int TYPE_LOCAL = 30;
    @Deprecated
    public static final String TAG_CLIENT_SEND = "CS";
    @Deprecated
    public static final String TAG_SERVER_RECV = "SR";
    @Deprecated
    public static final String TAG_SERVER_SEND = "SS";
    @Deprecated
    public static final String TAG_CLIENT_RECV = "CR";
    @Deprecated
    public static final String TAG_CLIENT_SEND_OLD = "ClientSend";
    @Deprecated
    public static final String TAG_SERVER_RECV_OLD = "ServerRecv";
    @Deprecated
    public static final String TAG_SERVER_SEND_OLD = "ServerSend";
    @Deprecated
    public static final String TAG_CLIENT_RECV_OLD = "ClientRecv";
    public static final String RPC_RESULT_SUCCESS = "00";
    public static final String RPC_RESULT_FAILED = "01";
    public static final String TRACE_ID_KEY = "traceId";
    public static final String RPC_ID_KEY = "rpcId";
    public static final String USER_DATA_KEY = "eagleEyeUserData";
    @Deprecated
    public static final String EAGLEEYE_TRACEID = "tb_eagleeye_traceid";
    public static final char ENTRY_SEPARATOR = '\u0012';
    public static final char KV_SEPARATOR = '\u0001';
    public static final char KV_SEPARATOR2 = '\u0014';
    public static final char ULC_SEPARATOR = '\u0012';
    public static final String ULC_EAGLEEYE_APPID = "ulc";
    private static AtomicBoolean rpcRecord;
    private static AtomicBoolean bizRecord;
    public static final char CLUSTER_TEST_KEY = 't';
    private static AtomicBoolean clusterTestEnabled;
    private static AtomicBoolean userDataEnabled;
    private static AtomicBoolean logDumpEnabled;
    @Deprecated
    public static final String EAGLEEYE_TAIR_SERVICENAME = "EETair";
    private static final long MAX_INDEX_TABLE_SIZE = 1000L;
    static final ConcurrentHashMap<String, String> indexes;
    private static final String INDEX_OUTPUT_TRACE_ID = "54007";
    private static final String INDEX_NOT_INDEXED = "ffffffff";
    private static volatile int samplingInterval;

    static final String getEagleEyeLocation() {
        try {
            final URL resource = EagleEye.class.getProtectionDomain().getCodeSource().getLocation();
            if (resource != null) {
                return resource.toString();
            }
        }
        catch (Throwable t) {}
        return "unknown location";
    }

    static final Charset getDefaultOutputCharset() {
        String charsetName = getSystemProperty("EAGLEEYE.CHARSET");
        if (EagleEyeCoreUtils.isNotBlank(charsetName)) {
            charsetName = charsetName.trim();
            try {
                final Charset cs = Charset.forName(charsetName);
                if (cs != null) {
                    return cs;
                }
            }
            catch (Exception ex) {}
        }
        Charset cs;
        try {
            cs = Charset.forName("GB18030");
        }
        catch (Exception e) {
            try {
                cs = Charset.forName("GBK");
            }
            catch (Exception e2) {
                cs = Charset.forName("UTF-8");
            }
        }
        return cs;
    }

    private static final String locateUserHome() {
        String userHome = getSystemProperty("user.home");
        if (EagleEyeCoreUtils.isNotBlank(userHome)) {
            if (!userHome.endsWith(File.separator)) {
                userHome += File.separator;
            }
        }
        else {
            userHome = "/tmp/";
        }
        return userHome;
    }

    private static final String locateBaseLogPath() {
        String tmpPath = getSystemProperty("JM.LOG.PATH");
        if (EagleEyeCoreUtils.isNotBlank(tmpPath)) {
            if (!tmpPath.endsWith(File.separator)) {
                tmpPath += File.separator;
            }
        }
        else {
            tmpPath = EagleEye.USER_HOME + "logs" + File.separator;
        }
        return tmpPath;
    }

    private static final String locateEagleEyeLogPath() {
        String tmpPath = getSystemProperty("EAGLEEYE.LOG.PATH");
        if (EagleEyeCoreUtils.isNotBlank(tmpPath)) {
            if (!tmpPath.endsWith(File.separator)) {
                tmpPath += File.separator;
            }
        }
        else {
            tmpPath = EagleEye.BASE_LOG_DIR + "eagleeye" + File.separator;
        }
        return tmpPath;
    }

    private static final String locateAppLogPath() {
        final String appName = getSystemProperty("project.name");
        if (EagleEyeCoreUtils.isNotBlank(appName)) {
            return EagleEye.USER_HOME + appName + File.separator + "logs" + File.separator;
        }
        return EagleEye.EAGLEEYE_LOG_DIR;
    }

    private static final String getSystemProperty(final String key) {
        try {
            return System.getProperty(key);
        }
        catch (Exception e) {
            return null;
        }
    }

    private static final long getSystemPropertyForLong(final String key, final long defaultValue) {
        try {
            return Long.parseLong(System.getProperty(key));
        }
        catch (Exception e) {
            return defaultValue;
        }
    }

    private static final EagleEyeAppender createSelfLogger() {
        final EagleEyeRollingFileAppender selfAppender = new EagleEyeRollingFileAppender(EagleEye.EAGLEEYE_SELF_LOG_FILE, getSystemPropertyForLong("EAGLEEYE.LOG.SELF.FILESIZE", 209715200L), false);
        selfAppender.setMaxBackupIndex((int)getSystemPropertyForLong("EAGLEEYE.LOG.SELF.BACKUPSIZE", 1L));
        return EagleEyeLogDaemon.watch(new SyncAppender(selfAppender));
    }

    private static final void createEagleEyeLoggers() {
        EagleEye.rpcAppender = new AsyncAppender((int)getSystemPropertyForLong("EAGLEEYE.LOG.RPC.QUEUESIZE", 2048L), 0);
        EagleEye.bizAppender = new AsyncAppender((int)getSystemPropertyForLong("EAGLEEYE.LOG.BIZ.QUEUESIZE", 4096L), 0);
        final EagleEyeRollingFileAppender rpcLogger = new EagleEyeRollingFileAppender(EagleEye.EAGLEEYE_RPC_LOG_FILE, getSystemPropertyForLong("EAGLEEYE.LOG.RPC.FILESIZE", 314572800L), true);
        rpcLogger.setMaxBackupIndex((int)getSystemPropertyForLong("EAGLEEYE.LOG.RPC.BACKUPSIZE", 2L));
        EagleEye.rpcAppender.start(rpcLogger, new DefaultRpcContextEncoder(), "RpcLog");
        EagleEyeLogDaemon.watch(EagleEye.rpcAppender);
        final EagleEyeRollingFileAppender bizLogger = new EagleEyeRollingFileAppender(EagleEye.EAGLEEYE_BIZ_LOG_FILE, getSystemPropertyForLong("EAGLEEYE.LOG.BIZ.FILESIZE", 314572800L), true);
        bizLogger.setMaxBackupIndex((int)getSystemPropertyForLong("EAGLEEYE.LOG.BIZ.BACKUPSIZE", 2L));
        EagleEye.bizAppender.start(bizLogger, new DefaultBizEncoder(), "BizLog");
        EagleEyeLogDaemon.watch(EagleEye.bizAppender);
        EagleEye.bizEagleEyeLogger = traceLoggerBuilder("biz-eagleeye").appender(EagleEye.bizAppender).buildSingleton();
    }

    private EagleEye() {
    }

    public static void turnRpcOn() {
        selfLog("[INFO] turnRpcOn");
        EagleEye.rpcRecord.set(true);
    }

    public static void turnRpcOff() {
        selfLog("[INFO] turnRpcOff");
        EagleEye.rpcRecord.set(false);
    }

    public static final boolean isRpcOff() {
        return !EagleEye.rpcRecord.get();
    }

    public static void turnBizOn() {
        selfLog("[INFO] turnBizOn");
        EagleEye.bizRecord.set(true);
    }

    public static void turnBizOff() {
        selfLog("[INFO] turnBizOff");
        EagleEye.bizRecord.set(false);
    }

    public static final boolean isBizOff() {
        return !EagleEye.bizRecord.get();
    }

    public static int getSamplingInterval() {
        return EagleEye.samplingInterval;
    }

    public static void setSamplingInterval(int interval) {
        if (interval < 1 || interval > 9999) {
            interval = 1;
        }
        selfLog("[INFO] setSamplingInterval=" + interval);
        EagleEye.samplingInterval = interval;
    }

    public static void setUserDataEnabled(final boolean enable) {
        selfLog("[INFO] setUserDataEnable: " + enable);
        EagleEye.userDataEnabled.set(enable);
    }

    public static final boolean isUserDataEnabled() {
        return EagleEye.userDataEnabled.get();
    }

    public static void setClusterTestEnabled(final boolean enable) {
        selfLog("[INFO] setClusterTestEnable: " + enable);
        EagleEye.clusterTestEnabled.set(enable);
    }

    public static final boolean isClusterTestEnabled() {
        return EagleEye.clusterTestEnabled.get();
    }

    public static void setLogDumpEnabled(final boolean enable) {
        selfLog("[INFO] setLogDumpEnabled: " + enable);
        EagleEye.logDumpEnabled.set(enable);
    }

    public static final boolean isLogDumpEnabled() {
        return EagleEye.logDumpEnabled.get();
    }

    public static void startTrace(final String traceId, final String traceName) {
        startTrace(traceId, null, traceName);
    }

    public static void startTrace(String traceId, String rpcId, final String traceName) {
        if (traceName == null) {
            return;
        }
        RpcContext_inner ctx = RpcContext_inner.get();
        if (ctx != null && ctx.traceId != null) {
            if (ctx.traceId.equals(traceId) && traceName.equals(ctx.traceName)) {
                return;
            }
            selfLog("[WARN] duplicated startTrace detected, overrided " + ctx.traceId + " (" + ctx.traceName + ") to " + traceId + " (" + traceName + ")");
            endTrace();
        }
        if (traceId == null || traceId.isEmpty()) {
            traceId = TraceIdGenerator.generate();
            rpcId = "0";
        }
        else if (rpcId == null || rpcId.length() > 64) {
            rpcId = "0";
        }
        try {
            ctx = new RpcContext_inner(traceId, rpcId);
            RpcContext_inner.set(ctx);
            ctx.startTrace(traceName);
        }
        catch (Throwable re) {
            selfLog("[ERROR] startTrace", re);
        }
    }

    public static String startTrace4Top(String traceId) {
        RpcContext_inner ctx = RpcContext_inner.get();
        if (ctx != null && ctx.traceId != null) {
            return ctx.traceId;
        }
        try {
            if (traceId == null || traceId.isEmpty()) {
                traceId = TraceIdGenerator.generate();
            }
            ctx = new RpcContext_inner(traceId, "0");
            ctx.isTopRpc = true;
            RpcContext_inner.set(ctx);
            return traceId;
        }
        catch (Throwable re) {
            selfLog("[ERROR] startTrace4Top", re);
            return null;
        }
    }

    public static void endTrace() {
        endTrace(null, 0);
    }

    public static void endTrace(final String resultCode, final int type) {
        try {
            RpcContext_inner root = RpcContext_inner.get();
            if (null == root) {
                return;
            }
            while (null != root.parentRpc) {
                root = root.parentRpc;
            }
            if (root.isTopRpc) {
                return;
            }
            root.endTrace(resultCode, type);
            commitRpcContext(root);
        }
        catch (Throwable re) {
            selfLog("[ERROR] endTrace", re);
        }
        finally {
            clearRpcContext();
        }
    }

    static final RpcContext_inner createContextIfNotExists(final boolean setToThreadLocal) {
        final RpcContext_inner ctx = RpcContext_inner.get();
        if (null == ctx) {
            final RpcContext_inner newCtx = new RpcContext_inner(TraceIdGenerator.generate(), "9");
            if (setToThreadLocal) {
                RpcContext_inner.set(newCtx);
            }
            return newCtx;
        }
        return ctx;
    }

    @Deprecated
    public static void businessTag(final String appId, final String queryKey, final String logContent) {
        EagleEye.bizEagleEyeLogger.logLine(appId, queryKey, logContent);
    }

    public static Object currentRpcContext() {
        try {
            final RpcContext_inner ctx = RpcContext_inner.get();
            if (null != ctx) {
                return ctx.toMap();
            }
        }
        catch (Throwable re) {
            selfLog("[ERROR] currentRpcContext", re);
        }
        return null;
    }

    public static RpcContext_inner getRpcContext() {
        return RpcContext_inner.get();
    }

    public static RpcContext_inner createRootRpcContext(String traceId, String rpcId) {
        if (traceId == null) {
            traceId = TraceIdGenerator.generate();
        }
        if (rpcId == null) {
            rpcId = "9";
        }
        return new RpcContext_inner(traceId, rpcId);
    }

    public static RpcContext_inner createRpcContextFromMap(final Map<String, String> map) {
        return RpcContext_inner.fromMap(map);
    }

    public static void setRpcContext(final Object rpcCtx) {
        try {
            RpcContext_inner ctx = null;
            if (rpcCtx instanceof Map) {
                ctx = RpcContext_inner.fromMap((Map<String, String>)rpcCtx);
            }
            else if (rpcCtx instanceof RpcContext_inner) {
                ctx = (RpcContext_inner)rpcCtx;
            }
            RpcContext_inner.set(ctx);
        }
        catch (Throwable re) {
            selfLog("[ERROR] setRpcContext", re);
        }
    }

    public static void setRpcContext(final RpcContext_inner context) {
        RpcContext_inner.set(context);
    }

    public static void clearRpcContext() {
        RpcContext_inner.set(null);
    }

    public static RpcContext_inner popRpcContext() {
        final RpcContext_inner ctx = RpcContext_inner.get();
        if (null == ctx) {
            return null;
        }
        RpcContext_inner.set(ctx.parentRpc);
        return ctx;
    }

    public static TraceGroup traceGroup(final String appName) {
        return createContextIfNotExists(true).traceGroup(appName);
    }

    public static StatLogger statLogger(final String loggerName) {
        return statLoggerBuilder(loggerName).buildSingleton();
    }

    public static StatLoggerBuilder statLoggerBuilder(final String loggerName) {
        return new StatLoggerBuilder(loggerName);
    }

    public static TraceLogger traceLogger(final String loggerName) {
        return traceLoggerBuilder(loggerName).buildSingleton();
    }

    public static TraceLoggerBuilder traceLoggerBuilder(final String loggerName) {
        return new TraceLoggerBuilder(loggerName);
    }

    public static void dump(final String appId, final String operationKey, final Object obj, final Object... params) {
        AtpTraceClient.trace(appId, operationKey, obj, params);
    }

    public static void dumpImportant(final String appId, final String operationKey, final Object obj, final Object... params) {
        AtpTraceClient.trace(true, appId, operationKey, obj, params);
    }

    public static void startRpc(final String serviceName, final String methodName) {
        try {
            final RpcContext_inner ctx = RpcContext_inner.get();
            RpcContext_inner childCtx;
            if (null == ctx) {
                childCtx = new RpcContext_inner(TraceIdGenerator.generate(), "9");
            }
            else {
                childCtx = ctx.createChildRpc();
            }
            RpcContext_inner.set(childCtx);
            if (!childCtx.isTopRpc) {
                childCtx.startRpc(serviceName, methodName);
            }
        }
        catch (Throwable re) {
            selfLog("[ERROR] startRpc", re);
        }
    }

    public static void startLocal(final String serviceName, final String methodName) {
        try {
            final RpcContext_inner ctx = RpcContext_inner.get();
            if (null == ctx) {
                selfLog("[ERROR] startLocal, no eagleeye trace in context");
                return;
            }
            final RpcContext_inner cloneCtx = ctx.cloneInstance();
            RpcContext_inner.set(cloneCtx);
            cloneCtx.startLocal(serviceName, methodName);
        }
        catch (Throwable re) {
            selfLog("[ERROR] startLocal", re);
        }
    }

    public static void endLocal(final String resultCode, final String appendMsg) {
        try {
            final RpcContext_inner ctx = RpcContext_inner.get();
            if (null == ctx) {
                selfLog("[ERROR] endLocal, no eagleeye trace in context");
                return;
            }
            ctx.endLocal(resultCode, appendMsg);
        }
        catch (Throwable re) {
            selfLog("[ERROR] endLocal", re);
        }
    }

    @Deprecated
    public static void annotateRpc(final String tag) {
        try {
            final RpcContext_inner ctx = RpcContext_inner.get();
            if (null == ctx) {
                return;
            }
            if (tag.equals("CS")) {
                rpcClientSend();
            }
            else if (tag.equals("SR")) {
                rpcServerRecv(null, null);
            }
            else if (tag.equals("SS")) {
                rpcServerSend(2);
            }
            else if (tag.equals("CR")) {
                rpcClientRecv("00", 1);
            }
            else if (tag.equals("ClientSend")) {
                rpcClientSend();
            }
            else if (tag.equals("ServerRecv")) {
                rpcServerRecv(null, null);
            }
            else if (tag.equals("ServerSend")) {
                rpcServerSend(2);
            }
            else if (tag.equals("ClientRecv")) {
                rpcClientRecv("00", 1);
            }
            else {
                selfLog("[ERROR] Unknown rpc tag:" + tag);
            }
        }
        catch (Throwable re) {
            selfLog("[ERROR] annotateRpc ERROR", re);
        }
    }

    @Deprecated
    public static void rpcClientSend(final String serverIp, final String service, final String method) {
        rpcClientSend();
        remoteIp(serverIp);
    }

    @Deprecated
    public static void rpcClientSend(final String msg) {
        rpcClientSend();
    }

    public static void rpcClientSend() {
        try {
            final RpcContext_inner ctx = RpcContext_inner.get();
            if (null == ctx) {
                return;
            }
            if (!ctx.isTopRpc) {
                ctx.rpcClientSend();
            }
        }
        catch (Throwable re) {
            selfLog("[ERROR] rpcClientSend", re);
        }
    }

    @Deprecated
    public static void rpcClientRecv() {
        rpcClientRecv("00");
    }

    @Deprecated
    public static void rpcClientRecv(final String resultCode) {
        rpcClientRecv(resultCode, 1);
    }

    public static void rpcClientRecv(final String resultCode, final int type) {
        rpcClientRecv(resultCode, type, null);
    }

    public static void rpcClientRecv(final String resultCode, final int type, final String appendMsg) {
        try {
            final RpcContext_inner ctx = RpcContext_inner.get();
            if (null == ctx) {
                return;
            }
            if (!ctx.isTopRpc) {
                ctx.endRpc(resultCode, type, appendMsg);
                commitRpcContext(ctx);
            }
            RpcContext_inner.set(ctx.parentRpc);
        }
        catch (Throwable re) {
            selfLog("[ERROR] rpcClientRecv", re);
        }
    }

    @Deprecated
    public static void rpcFail(final String resultCode) {
        rpcClientRecv(resultCode, 1);
    }

    @Deprecated
    public static void rpcFail(final String resultCode, final int type) {
        rpcClientRecv(resultCode, type);
    }

    @Deprecated
    public static void rpcServerRecv(final String clientIp, final String service, final String method) {
        rpcServerRecv(service, method);
        remoteIp(clientIp);
    }

    public static void rpcServerRecv(final String service, final String method) {
        try {
            createContextIfNotExists(true).rpcServerRecv(service, method);
        }
        catch (Throwable re) {
            selfLog("[ERROR] rpcServerRecv", re);
        }
    }

    @Deprecated
    public static void rpcServerSend() {
        rpcServerSend(2);
    }

    public static void rpcServerSend(final int type) {
        rpcServerSend(null, type);
    }

    public static void rpcServerSend(final String resultCode, final int type) {
        rpcServerSend(resultCode, type, null);
    }

    public static void rpcServerSend(final String resultCode, final int type, final String appendMsg) {
        try {
            final RpcContext_inner ctx = RpcContext_inner.get();
            if (ctx != null) {
                ctx.rpcServerSend(type, resultCode, appendMsg);
                commitRpcContext(ctx);
            }
        }
        catch (Throwable re) {
            selfLog("[ERROR] rpcServerRecv", re);
        }
        finally {
            clearRpcContext();
        }
    }

    public static String getTraceId() {
        final RpcContext_inner ctx = RpcContext_inner.get();
        return (null == ctx) ? null : ctx.traceId;
    }

    public static String getRpcId() {
        final RpcContext_inner ctx = RpcContext_inner.get();
        return (null == ctx) ? null : ctx.rpcId;
    }

    public static String getLocalId() {
        final RpcContext_inner ctx = RpcContext_inner.get();
        return (null == ctx) ? "" : ctx.getLocalId();
    }

    public static void attribute(final String key, final String value) {
        createContextIfNotExists(true).putLocalAttribute(key, value);
    }

    public static String getUserData(final String key) {
        final RpcContext_inner ctx = RpcContext_inner.get();
        return (null != ctx) ? ctx.getUserData(key) : null;
    }

    public static String putUserData(final String key, final String value) {
        return createContextIfNotExists(true).putUserData(key, value);
    }

    public static String removeUserData(final String key) {
        final RpcContext_inner ctx = RpcContext_inner.get();
        return (null != ctx) ? ctx.removeUserData(key) : null;
    }

    public static Map<String, String> getUserDataMap() {
        final RpcContext_inner ctx = RpcContext_inner.get();
        return (Map<String, String>)((null != ctx) ? ctx.getUserDataMap() : null);
    }

    public static String exportUserData() {
        final RpcContext_inner ctx = RpcContext_inner.get();
        return (null != ctx) ? ctx.exportUserData() : null;
    }

    public static void callBack(final String msg) {
        if (msg != null && msg.length() < 4096) {
            final RpcContext_inner ctx = RpcContext_inner.get();
            if (null == ctx) {
                return;
            }
            ctx.callBackMsg = msg;
        }
    }

    public static void requestSize(final long size) {
        final RpcContext_inner ctx = RpcContext_inner.get();
        if (null == ctx) {
            return;
        }
        ctx.requestSize = size;
    }

    public static void remoteIp(final String remoteIp) {
        final RpcContext_inner ctx = RpcContext_inner.get();
        if (null == ctx) {
            return;
        }
        ctx.remoteIp = remoteIp;
    }

    public static void responseSize(final long size) {
        final RpcContext_inner ctx = RpcContext_inner.get();
        if (null == ctx) {
            return;
        }
        ctx.responseSize = size;
    }

    @Deprecated
    public static void rpcLog(final int type, final String msg) {
        rpcLog(type, msg, true);
    }

    @Deprecated
    public static void rpcLog(final int type, final String msg, final boolean appendRpcId) {
        try {
            final RpcContext_inner ctx = RpcContext_inner.get();
            if (null == ctx || ctx.isTopRpc) {
                return;
            }
            if (msg != null && msg.length() < 4096) {
                final RpcContext_inner sub = new RpcContext_inner(ctx.traceId, appendRpcId ? ctx.rpcId : null);
                sub.rpcLog(type, msg, appendRpcId);
                commitRpcContext(sub);
            }
        }
        catch (Throwable e) {
            selfLog("[ERROR] rpcLog", e);
        }
    }

    public static String generateTraceId(final String ip) {
        return TraceIdGenerator.generate(ip);
    }

    public static String generateMulticastRpcId(String rpcId, final String identifier) {
        if (rpcId == null || rpcId.length() == 0) {
            rpcId = "0";
        }
        return rpcId + "." + TraceIdGenerator.generateIpv4Id();
    }

    public static String index(final String msg) {
        try {
            if (msg == null || msg.length() <= 8) {
                return msg;
            }
            String index = EagleEye.indexes.get(msg);
            if (index != null) {
                return index;
            }
            if (EagleEye.indexes.size() >= 1000L) {
                return "ffffffff";
            }
            index = EagleEyeCoreUtils.digest(msg);
            final String rs = EagleEye.indexes.putIfAbsent(msg, index);
            if (rs == null) {
                index(9, index, msg);
                selfLog("[INFO] generate index: " + index + " => " + msg);
            }
            return index;
        }
        catch (Throwable e) {
            selfLog("[ERROR] index: " + msg, e);
            return msg;
        }
    }

    static void index(final int type, final String index, final String msg) {
        final RpcContext_inner sub = new RpcContext_inner("54007", "9");
        sub.index(type, index, EagleEyeCoreUtils.filterInvalidCharacters(msg));
        commitRpcContext(sub);
    }

    static String exportIndexes() {
        final StringBuilder builder = new StringBuilder(128 * EagleEye.indexes.size());
        for (final Map.Entry<String, String> entry : EagleEye.indexes.entrySet()) {
            builder.append(entry.getValue()).append(" -> ").append(entry.getKey()).append('\n');
        }
        return builder.toString();
    }

    public static void commitRpcContext(final RpcContext_inner ctx) {
        if (ctx.logType >= 0 && !isRpcOff() && ctx.isTraceSampled()) {
            EagleEye.rpcAppender.append(ctx);
        }
    }

    static void commitLocalContext(final LocalContext_inner ctx) {
        if (ctx.logType >= 0 && !isRpcOff() && ctx.isTraceSampled()) {
            EagleEye.rpcAppender.append(ctx);
        }
    }

    @Deprecated
    public static void commitBusinessTag(final RpcContext_inner ctx, final String appId, final String queryKey, final String logContent) {
        EagleEye.bizEagleEyeLogger.logLineWithContext(ctx, appId, queryKey, logContent);
    }

    public static void setEagelEyeRpcAppender(final EagleEyeAppender appender) {
        EagleEye.rpcAppender.setEagleEyeAppender(appender);
    }

    public static void setEagelEyeBizAppender(final EagleEyeAppender appender) {
        EagleEye.bizAppender.setEagleEyeAppender(appender);
    }

    static void setEagelEyeSelfAppender(final EagleEyeAppender appender) {
        EagleEye.selfAppender = appender;
    }

    public static void selfLog(final String log) {
        try {
            final String timestamp = EagleEyeCoreUtils.formatTime(System.currentTimeMillis());
            final String line = "[" + timestamp + "] " + log + "\r\n";
            EagleEye.selfAppender.append(line);
        }
        catch (Throwable t) {}
    }

    public static void selfLog(final String log, final Throwable e) {
        if (EagleEye.exceptionThreshold.incrementAndGet() <= EagleEye.MAX_EXCEPTION_COUNT) {
            try {
                final String timestamp = EagleEyeCoreUtils.formatTime(System.currentTimeMillis());
                final StringWriter sw = new StringWriter(4096);
                final PrintWriter pw = new PrintWriter(sw, false);
                pw.append('[').append(timestamp).append("] ").append(log).append("\r\n");
                e.printStackTrace(pw);
                pw.println();
                pw.flush();
                EagleEye.selfAppender.append(sw.toString());
            }
            catch (Throwable t) {}
        }
    }

    public static void flush() {
        EagleEyeLogDaemon.flushAndWait();
    }

    @Deprecated
    public static void log(final String logKey, final Long userId, final String bizId, final int operateType, final String operateContent) {
        if (EagleEyeCoreUtils.isBlank(logKey)) {
            return;
        }
        log(logKey, userId, bizId, null, operateType, operateContent, new LinkedHashMap<String, String>());
    }

    @Deprecated
    public static void log(final String logKey, final Long userId, final String bizId, final Long operatorId, final int operateType, final String operateContent) {
        if (EagleEyeCoreUtils.isBlank(logKey)) {
            return;
        }
        log(logKey, userId, bizId, operatorId, operateType, operateContent, new LinkedHashMap<String, String>());
    }

    @Deprecated
    public static void log(final String logKey, final Long userId, final String bizId, final int operateType, final String operateContent, String... extendInfos) {
        if (EagleEyeCoreUtils.isBlank(logKey)) {
            return;
        }
        if (null == extendInfos || extendInfos.length == 0) {
            extendInfos = EagleEyeCoreUtils.EMPTY_STRING_ARRAY;
        }
        log(logKey, userId, bizId, null, operateType, operateContent, extendInfos);
    }

    @Deprecated
    public static void log(final String logKey, final Long userId, final String bizId, final int operateType, final String operateContent, Map<String, String> extendInfos) {
        if (EagleEyeCoreUtils.isBlank(logKey)) {
            return;
        }
        if (null == extendInfos || extendInfos.size() == 0) {
            extendInfos = new LinkedHashMap<String, String>();
        }
        log(logKey, userId, bizId, null, operateType, operateContent, extendInfos);
    }

    @Deprecated
    public static void log(final String logKey, final Long userId, final String bizId, final Long operatorId, final int operateType, final String operateContent, final String... extendInfos) {
        if (EagleEyeCoreUtils.isBlank(logKey)) {
            return;
        }
        final String msg = getMsg(logKey, userId, bizId, operatorId, operateType, operateContent, extendInfos);
        businessTag("ulc", logKey, msg.toString());
    }

    @Deprecated
    public static void log(final String logKey, final Long userId, final String bizId, final Long operatorId, final int operateType, final String operateContent, final Map<String, String> extendInfos) {
        if (EagleEyeCoreUtils.isBlank(logKey)) {
            return;
        }
        final String msg = getMsg(logKey, userId, bizId, operatorId, operateType, operateContent, extendInfos);
        businessTag("ulc", logKey, msg.toString());
    }

    @Deprecated
    public static void log(final LogContext logContext) {
        log("ulc", logContext);
    }

    @Deprecated
    public static void log(final String appId, final LogContext logContext) {
        if (EagleEyeCoreUtils.isBlank(logContext.getLogKey())) {
            return;
        }
        final String msg = getMsg(logContext);
        businessTag(appId, logContext.getLogKey(), msg.toString());
    }

    @Deprecated
    private static String getMsg(final String logKey, final Long userId, final String bizId, final Long operatorId, final int operateType, final String operateContent, final String... extendInfos) {
        final LogContext logContext = new LogContext();
        logContext.logKey(logKey).userId(userId).bizId(bizId).operatorId(operatorId).operateType(operateType).operateContent(operateContent).extendArray(extendInfos);
        return getMsg(logContext);
    }

    @Deprecated
    private static String getMsg(final String logKey, final Long userId, final String bizId, final Long operatorId, final int operateType, final String operateContent, final Map<String, String> extendInfos) {
        final LogContext logContext = new LogContext();
        logContext.logKey(logKey).userId(userId).bizId(bizId).operatorId(operatorId).operateType(operateType).operateContent(operateContent).extendInfos(extendInfos);
        return getMsg(logContext);
    }

    @Deprecated
    private static String getMsg(final LogContext logContext) {
        final StringBuilder sBuilder = new StringBuilder();
        sBuilder.append(logContext.getLogKey()).append('\u0012');
        if (null != logContext.getUserId()) {
            sBuilder.append(logContext.getUserId());
        }
        else {
            sBuilder.append(0L);
        }
        sBuilder.append('\u0012');
        if (EagleEyeCoreUtils.isNotBlank(logContext.getBizId())) {
            sBuilder.append(logContext.getBizId());
        }
        else {
            sBuilder.append("");
        }
        sBuilder.append('\u0012');
        if (null != logContext.getOperatorId()) {
            sBuilder.append(logContext.getOperatorId());
        }
        else {
            sBuilder.append(0L);
        }
        sBuilder.append('\u0012');
        sBuilder.append(logContext.getOperateType()).append('\u0012');
        if (EagleEyeCoreUtils.isNotBlank(logContext.getOperateContent())) {
            sBuilder.append(logContext.getOperateContent());
        }
        else {
            sBuilder.append("");
        }
        sBuilder.append('\u0012');
        if (EagleEyeCoreUtils.isNotBlank(logContext.getUserNick())) {
            sBuilder.append(logContext.getUserNick());
        }
        else {
            sBuilder.append("");
        }
        sBuilder.append('\u0012');
        if (EagleEyeCoreUtils.isNotBlank(logContext.getOpLevel())) {
            sBuilder.append(logContext.getOpLevel());
        }
        else {
            sBuilder.append("");
        }
        sBuilder.append('\u0012');
        if (EagleEyeCoreUtils.isNotBlank(logContext.getOpItem())) {
            sBuilder.append(logContext.getOpItem());
        }
        else {
            sBuilder.append("");
        }
        if (null != logContext.getExtendArray() && logContext.getExtendArray().length > 0) {
            for (String info : logContext.getExtendArray()) {
                if (EagleEyeCoreUtils.isBlank(info)) {
                    info = "";
                }
                sBuilder.append('\u0012').append(info);
            }
        }
        if (null != logContext.getExtendInfos() && logContext.getExtendInfos().size() > 0) {
            for (final Map.Entry<String, String> entry : logContext.getExtendInfos().entrySet()) {
                final String key = entry.getKey();
                if (EagleEyeCoreUtils.isNotBlank(key)) {
                    final Object value = entry.getValue();
                    sBuilder.append('\u0012').append(key).append('\u0001');
                    if (value == null) {
                        continue;
                    }
                    EagleEyeCoreUtils.appendWithBlankCheck(value.toString(), "", sBuilder);
                }
            }
        }
        return sBuilder.toString();
    }

    static {
        USER_HOME = locateUserHome();
        BASE_LOG_DIR = locateBaseLogPath();
        EAGLEEYE_LOG_DIR = locateEagleEyeLogPath();
        APP_LOG_DIR = locateAppLogPath();
        DEFAULT_CHARSET = getDefaultOutputCharset();
        EAGLEEYE_RPC_LOG_FILE = EagleEye.EAGLEEYE_LOG_DIR + "eagleeye.log";
        EAGLEEYE_BIZ_LOG_FILE = EagleEye.EAGLEEYE_LOG_DIR + "biz_eagleeye.log";
        EAGLEEYE_SELF_LOG_FILE = EagleEye.EAGLEEYE_LOG_DIR + "eagleeye-self.log";
        EagleEye.selfAppender = createSelfLogger();
        EagleEye.exceptionThreshold = new AtomicInteger(0);
        EagleEye.MAX_EXCEPTION_COUNT = 5;
        EagleEye.rpcRecord = new AtomicBoolean(true);
        EagleEye.bizRecord = new AtomicBoolean(true);
        EagleEye.clusterTestEnabled = new AtomicBoolean(true);
        EagleEye.userDataEnabled = new AtomicBoolean(true);
        EagleEye.logDumpEnabled = new AtomicBoolean(true);
        indexes = new ConcurrentHashMap<String, String>();
        EagleEye.samplingInterval = 1;
        selfLog("[INFO] EagleEye started (" + getEagleEyeLocation() + ")");
        try {
            createEagleEyeLoggers();
        }
        catch (Throwable e) {
            selfLog("[ERROR] fail to create EagleEye logger", e);
        }
        try {
            EagleEyeLogDaemon.start();
        }
        catch (Throwable e) {
            selfLog("[ERROR] fail to start EagleEyeLogDaemon", e);
        }
        try {
            StatLogController.start();
        }
        catch (Throwable e) {
            selfLog("[ERROR] fail to start StatLogController", e);
        }
        try {
            EagleEyeJVMPatchImpl.setupInstance();
        }
        catch (Throwable e) {
            selfLog("[ERROR] fail to setup EagleEyeJVMPatchImpl", e);
        }
    }
}
