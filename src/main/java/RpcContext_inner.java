import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class RpcContext_inner extends AbstractContext {
    private static final ThreadLocal<RpcContext_inner> threadLocal;
    public static final String EAGLEEYE_TRACE_SIGNATURE_KEY = "s";
    final RpcContext_inner parentRpc;
    private final AtomicInteger childRpcIdx;
    LocalContext_inner localContext;

    RpcContext_inner(final String _traceId, final String _rpcId) {
        this(_traceId, _rpcId, (RpcContext_inner)null);
    }

    RpcContext_inner(final String _traceId, final String _rpcId, final RpcContext_inner _parentRpc) {
        this(_traceId, _rpcId, _parentRpc, new AtomicInteger(0), new AtomicInteger(0));
    }

    RpcContext_inner(final String _traceId, final String _rpcId, final RpcContext_inner _parentRpc, final AtomicInteger _localIdx) {
        this(_traceId, _rpcId, _parentRpc, new AtomicInteger(0), _localIdx);
    }

    RpcContext_inner(final String _traceId, final String _rpcId, final RpcContext_inner _parentRpc, final AtomicInteger _childRpcIdx, final AtomicInteger _localIdx) {
        super(_traceId, _rpcId, _localIdx);
        this.parentRpc = _parentRpc;
        this.childRpcIdx = _childRpcIdx;
    }

    String nextChildRpcId() {
        return this.rpcId + "." + this.childRpcIdx.incrementAndGet();
    }

    public void startLocal(final String serviceName, final String methodName) {
        LocalContext_inner tmpContext = this.getCurrentLocalContext();
        if (tmpContext != null && tmpContext.localId.length() > 64) {
            EagleEye.selfLog("[WARN] LocalContext leak detected, traceId=" + this.getTraceId() + ", localId=" + this.getLocalId());
            tmpContext = new LocalContext_inner(this.getTraceId(), this.getRpcId(), "9");
        }
        final LocalContext_inner localCtx = new LocalContext_inner(this.traceId, this.getRpcId());
        localCtx.startRpc(serviceName, methodName);
        localCtx.localId = this.nextLocalId(tmpContext);
        localCtx.traceGroup = this.traceGroup;
        localCtx.attributes = this.attributes;
        localCtx.localAttributes = this.localAttributes;
        localCtx.localParent = tmpContext;
        this.localContext = localCtx;
    }

    public void endLocal(final String resultCode, final String appendMsg) {
        final LocalContext_inner tmpContext = this.localContext;
        if (null == tmpContext) {
            EagleEye.selfLog("[ERROR] endLocal, no localRpc to end");
            return;
        }
        if (tmpContext.isEnd) {
            EagleEye.selfLog("[ERROR] localId " + tmpContext.localId + " already end");
            return;
        }
        tmpContext.isEnd = true;
        this.localContext = tmpContext.localParent;
        tmpContext.endRpc(resultCode, 30, appendMsg);
        EagleEye.commitLocalContext(tmpContext);
    }

    protected RpcContext_inner cloneInstance() {
        final RpcContext_inner clone = new RpcContext_inner(this.traceId, this.getRpcId(), this.parentRpc, this.childRpcIdx, this.localIdx);
        clone.attributes = this.attributes;
        clone.localAttributes = this.localAttributes;
        clone.localContext = this.localContext;
        clone.traceName = this.traceName;
        clone.serviceName = this.serviceName;
        clone.methodName = this.methodName;
        clone.remoteIp = this.remoteIp;
        clone.callBackMsg = this.callBackMsg;
        clone.logType = this.logType;
        clone.rpcType = this.rpcType;
        clone.span0 = this.span0;
        clone.span1 = this.span1;
        clone.isTopRpc = this.isTopRpc;
        clone.startTime = this.startTime;
        clone.logTime = this.logTime;
        clone.requestSize = this.requestSize;
        clone.responseSize = this.responseSize;
        clone.traceGroup = this.traceGroup;
        clone.localId = this.localId;
        return clone;
    }

    LocalContext_inner getCurrentLocalContext() {
        LocalContext_inner tmpContext;
        for (tmpContext = this.localContext; null != tmpContext && tmpContext.isEnd; tmpContext = tmpContext.localParent) {}
        return this.localContext = tmpContext;
    }

    String nextLocalId(final LocalContext_inner tmpContext) {
        if (tmpContext == null) {
            return "0." + this.localIdx.incrementAndGet();
        }
        return tmpContext.localId + "." + tmpContext.localIdx.incrementAndGet();
    }

    String getInnerLocalId() {
        if (null == this.localContext) {
            return "";
        }
        final LocalContext_inner tmpContext = this.getCurrentLocalContext();
        if (tmpContext == null) {
            return "";
        }
        return tmpContext.getLocalId();
    }

    public RpcContext_inner createChildRpc() {
        RpcContext_inner parent;
        if (this.rpcId.length() > 64 && this.parentRpc != null && this.parentRpc.parentRpc != null) {
            EagleEye.selfLog("[WARN] RpcContext leak detected, traceId=" + this.traceId + ", rpcId=" + this.rpcId);
            parent = new RpcContext_inner(this.traceId, "9", (RpcContext_inner)null);
        }
        else {
            parent = this;
        }
        final RpcContext_inner ctx = new RpcContext_inner(this.traceId, this.nextChildRpcId(), parent, this.localIdx);
        ctx.isTopRpc = this.isTopRpc;
        ctx.attributes = this.attributes;
        ctx.traceGroup = this.traceGroup;
        ctx.localId = this.getInnerLocalId();
        ctx.localContext = this.localContext;
        return ctx;
    }

    public RpcContext_inner getParentRpcContext() {
        return this.parentRpc;
    }

    public void startTrace(final String traceName) {
        this.logType = 1;
        this.startTime = System.currentTimeMillis();
        this.traceName = traceName;
    }

    public void endTrace(final String result, final int type) {
        if (this.logType != 1) {
            EagleEye.selfLog("[WARN] context mismatch at endTrace(), logType=" + this.logType);
            this.logType = -255;
            return;
        }
        this.logTime = System.currentTimeMillis();
        this.serviceName = result;
        this.rpcType = type;
    }

    public void rpcClientSend() {
        if (this.logType != 2) {
            return;
        }
        this.span0 = (int)(System.currentTimeMillis() - this.startTime);
    }

    public void rpcServerRecv(final String serviceName, final String methodName) {
        this.logType = 3;
        this.startTime = System.currentTimeMillis();
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.putUserData("s", updateSignature(this.getUserData("s"), serviceName, methodName));
    }

    @Deprecated
    public void rpcServerSend(final int type) {
        this.rpcServerSend(type, null, null);
    }

    public void rpcServerSend(final int type, final String result, final String appendMsg) {
        if (this.logType != 3) {
            EagleEye.selfLog("[WARN] context mismatch at rpcServerSend(), logType=" + this.logType);
            this.logType = -255;
            return;
        }
        this.logTime = System.currentTimeMillis();
        this.traceName = result;
        if (appendMsg != null) {
            this.callBackMsg = appendMsg;
        }
        this.rpcType = type;
    }

    public void rpcLog(final int type, final String msg, final boolean appendRpcId) {
        this.logType = 4;
        this.logTime = System.currentTimeMillis();
        this.rpcType = type;
        this.callBackMsg = msg;
    }

    public void index(final int type, final String index, final String msg) {
        this.logType = 5;
        this.logTime = System.currentTimeMillis();
        this.rpcType = type;
        this.traceName = index;
        this.callBackMsg = msg;
    }

    static String updateSignature(final String oldSig, final String serviceName, final String methodName) {
        int sig = 0;
        if (oldSig != null) {
            try {
                sig = (int)Long.parseLong(oldSig, 16);
            }
            catch (NumberFormatException ex) {}
        }
        final int a = (serviceName != null) ? serviceName.hashCode() : "null".hashCode();
        final int b = (methodName != null) ? methodName.hashCode() : "null".hashCode();
        return Integer.toHexString((sig * 31 + a) * 31 + b);
    }

    static void set(final RpcContext_inner ctx) {
        RpcContext_inner.threadLocal.set(ctx);
    }

    static RpcContext_inner get() {
        return RpcContext_inner.threadLocal.get();
    }

    public Map<String, String> toMap() {
        final HashMap<String, String> map = new HashMap<String, String>(3);
        map.put("traceId", this.traceId);
        map.put("rpcId", this.rpcId);
        map.put("eagleEyeUserData", this.exportUserData());
        return map;
    }

    static RpcContext_inner fromMap(final Map<String, String> map) {
        final String traceId = map.get("traceId");
        final String rpcId = map.get("rpcId");
        final String userData = map.get("eagleEyeUserData");
        if (null == traceId || null == rpcId) {
            return null;
        }
        final RpcContext_inner ctx = new RpcContext_inner(traceId, rpcId);
        ctx.importUserData(userData);
        return ctx;
    }

    static {
        threadLocal = new ThreadLocal<RpcContext_inner>();
    }
}
