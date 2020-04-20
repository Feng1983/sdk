import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

abstract class AbstractContext extends BaseContext {
    public static final String EAGLEEYE_TRACE_GROUP_KEY = "g";
    protected static final String EMPTY_LOCALID = "";
    String remoteIp;
    int span0;
    int span1;
    boolean isTopRpc;
    long startTime;
    long requestSize;
    long responseSize;
    TraceGroup traceGroup;
    Map<String, String> attributes;
    Map<String, String> localAttributes;
    String localId;
    final AtomicInteger localIdx;

    AbstractContext(final String _traceId, final String _rpcId) {
        this(_traceId, _rpcId, new AtomicInteger(0));
    }

    AbstractContext(final String _traceId, final String _rpcId, final AtomicInteger _localIdx) {
        super(_traceId, _rpcId);
        this.remoteIp = "";
        this.span0 = 0;
        this.span1 = 0;
        this.isTopRpc = false;
        this.startTime = 0L;
        this.requestSize = 0L;
        this.responseSize = 0L;
        this.traceGroup = null;
        this.attributes = null;
        this.localAttributes = null;
        this.localId = "";
        this.localIdx = _localIdx;
    }

    public String getLocalId() {
        return this.localId;
    }

    public long getRequestSize() {
        return this.requestSize;
    }

    public void setRequestSize(final long requestSize) {
        this.requestSize = requestSize;
    }

    public long getResponseSize() {
        return this.responseSize;
    }

    public void setResponseSize(final long responseSize) {
        this.responseSize = responseSize;
    }

    public String getRemoteIp() {
        return this.remoteIp;
    }

    public void setRemoteIp(final String remoteIp) {
        this.remoteIp = remoteIp;
    }

    public void setStartTime(final long startTime) {
        this.startTime = startTime;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public void setTimeSpan0(final int span0) {
        this.span0 = span0;
    }

    public int getTimeSpan0() {
        return this.span0;
    }

    public void setTimeSpan1(final int span1) {
        this.span1 = span1;
    }

    public int getTimeSpan1() {
        return this.span1;
    }

    public void startRpc(final String serviceName, final String methodName) {
        this.logType = 2;
        this.startTime = System.currentTimeMillis();
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.span0 = 0;
    }

    public void endRpc(final String result, final int type, final String appendMsg) {
        if (this.logType != 2) {
            EagleEye.selfLog("[WARN] context mismatch at endRpc(), logType=" + this.logType);
            this.logType = -255;
            return;
        }
        this.logTime = System.currentTimeMillis();
        this.rpcType = type;
        this.traceName = result;
        if (appendMsg != null) {
            this.callBackMsg = appendMsg;
        }
        this.span1 = (int)(this.logTime - this.startTime);
    }

    public boolean isTraceSampled() {
        if (this.traceId == null) {
            return false;
        }
        final int si = EagleEye.getSamplingInterval();
        if (si <= 1 || si >= 10000) {
            return true;
        }
        if (this.traceId.length() < 25) {
            return this.traceId.hashCode() % si == 0;
        }
        int count = this.traceId.charAt(21) - '0';
        count = count * 10 + this.traceId.charAt(22) - 48;
        count = count * 10 + this.traceId.charAt(23) - 48;
        count = count * 10 + this.traceId.charAt(24) - 48;
        return count % si == 0;
    }

    boolean isAppendExtendMsg() {
        final boolean appendTraceGroup = this.traceGroup != null;
        return appendTraceGroup && !appendTraceGroup;
    }

    protected void doAppendTraceGroup(final StringBuilder appender, final boolean escapeChars) {
        final TraceGroup traceGroup = this.traceGroup;
        if (traceGroup != null) {
            appender.append('@').append("g").append('\u0014');
            if (escapeChars) {
                EagleEyeCoreUtils.appendLog(traceGroup.getAppName(), appender, '|').append('$');
                EagleEyeCoreUtils.appendLog(traceGroup.getKey1(), appender, '|').append('$');
                EagleEyeCoreUtils.appendLog(traceGroup.getKey2(), appender, '|').append('$');
                EagleEyeCoreUtils.appendLog(traceGroup.getKey3(), appender, '|');
            }
            else {
                appender.append(traceGroup.getAppName()).append('$').append(traceGroup.getKey1()).append('$').append(traceGroup.getKey2()).append('$').append(traceGroup.getKey3());
            }
            appender.append('\u0012');
        }
    }

    void logContextData(final StringBuilder appender) {
        final boolean appendAttributes = this.attributes != null && !this.attributes.isEmpty();
        final boolean appendLocalAttributes = this.localAttributes != null && !this.localAttributes.isEmpty();
        final boolean appendTraceGroup = this.traceGroup != null;
        if (!appendAttributes && !appendLocalAttributes && !appendTraceGroup) {
            return;
        }
        appender.append("|@");
        final int startLen = appender.length();
        if (appendAttributes) {
            this.doAppendUserData(appender, true, startLen);
        }
        if (appendLocalAttributes) {
            this.doAppendLocalAttributes(appender, true, startLen);
        }
        if (appendTraceGroup) {
            this.doAppendTraceGroup(appender, true);
        }
    }

    protected void doAppendUserData(final StringBuilder appender, final boolean escapeChars, final int startLen) {
        for (final Map.Entry<String, String> entry : this.attributes.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (EagleEyeCoreUtils.isNotBlank(key) && value != null) {
                appender.append(key).append('\u0014');
                if (escapeChars) {
                    EagleEyeCoreUtils.appendLog(value, appender, '|').append('\u0012');
                }
                else {
                    appender.append(value).append('\u0012');
                }
            }
            if (appender.length() - startLen >= 1024) {
                EagleEye.selfLog("[WARN] UserData is too long, size=" + appender.length());
                break;
            }
        }
    }

    protected void doAppendLocalAttributes(final StringBuilder appender, final boolean escapeChars, final int startLen) {
        for (final Map.Entry<String, String> entry : this.localAttributes.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (EagleEyeCoreUtils.isNotBlank(key) && value != null) {
                appender.append('@').append(key).append('\u0014');
                if (escapeChars) {
                    EagleEyeCoreUtils.appendLog(value, appender, '|').append('\u0012');
                }
                else {
                    appender.append(value).append('\u0012');
                }
            }
            if (appender.length() - startLen >= 1024) {
                EagleEye.selfLog("[WARN] UserData is too long, size=" + appender.length());
                break;
            }
        }
    }

    public String putUserData(final String key, final String value) {
        if (EagleEyeCoreUtils.isBlank(key) || key.length() > 512) {
            EagleEye.selfLog("[WARN] userData is not accepted since key is blank or too long: " + key);
            return null;
        }
        if (value != null && value.length() > 512) {
            EagleEye.selfLog("[WARN] userData is not accepted since value is too long: " + value);
            return null;
        }
        if (this.attributes == null) {
            this.attributes = new LinkedHashMap<String, String>();
        }
        return this.attributes.put(key, value);
    }

    public String removeUserData(final String key) {
        if (this.attributes != null) {
            return this.attributes.remove(key);
        }
        return null;
    }

    public String getUserData(final String key) {
        return (this.attributes != null) ? this.attributes.get(key) : null;
    }

    public Map<String, String> getUserDataMap() {
        Map<String,String> emptiness = Collections.emptyMap();
        return (this.attributes == null) ? emptiness : Collections.unmodifiableMap((Map<? extends String, ? extends String>)this.attributes);
    }

    public String putLocalAttribute(final String key, final String value) {
        if (this.localAttributes == null) {
            this.localAttributes = new LinkedHashMap<String, String>();
        }
        return this.localAttributes.put(key, value);
    }

    public String removeLocalAttribute(final String key) {
        if (this.localAttributes != null) {
            return this.localAttributes.remove(key);
        }
        return null;
    }

    public String getLocalAttribute(final String key) {
        return (this.localAttributes != null) ? this.localAttributes.get(key) : null;
    }

    public Map<String, String> getLocalAttributeMap() {
        if (this.localAttributes == null) {
            this.localAttributes = new LinkedHashMap<String, String>();
        }
        return this.localAttributes;
    }

    public TraceGroup traceGroup(final String appName) {
        if (this.traceGroup == null) {
            this.traceGroup = new TraceGroup(appName);
        }
        return this.traceGroup;
    }

    public String exportUserData() {
        final Map<String, String> userData = this.attributes;
        if (userData == null || userData.isEmpty()) {
            return null;
        }
        final StringBuilder appender = new StringBuilder(256);
        this.doAppendUserData(appender, false, 0);
        if (appender.length() == 0) {
            return null;
        }
        return appender.toString();
    }

    public void importUserData(final String userData) {
        if (EagleEyeCoreUtils.isNotBlank(userData) && EagleEye.isUserDataEnabled()) {
            final String[] entries = EagleEyeCoreUtils.split(userData, '\u0012');
            final Map<String, String> map = new LinkedHashMap<String, String>(entries.length);
            for (final String entry : entries) {
                final int p = entry.indexOf(20);
                if (p > 0 && p < entry.length()) {
                    if (p != 1 || entry.charAt(0) != 't' || EagleEye.isClusterTestEnabled()) {
                        map.put(entry.substring(0, p), entry.substring(p + 1));
                    }
                }
            }
            if (!map.isEmpty()) {
                this.attributes = map;
            }
        }
    }
}
