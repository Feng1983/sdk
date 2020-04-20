public class BaseContext {
    final String traceId;
    final String rpcId;
    String traceName;
    String serviceName;
    String methodName;
    String callBackMsg;
    long logTime;
    int logType;
    int rpcType;

    BaseContext(final String _traceId, final String _rpcId) {
        this.traceName = "";
        this.serviceName = "";
        this.methodName = "";
        this.callBackMsg = null;
        this.logTime = 0L;
        this.logType = 0;
        this.rpcType = 0;
        this.traceId = _traceId;
        this.rpcId = _rpcId;
    }

    BaseContext(final int logType) {
        this("", "");
        this.logType = logType;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public void setServiceName(final String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public void setMethodName(final String methodName) {
        this.methodName = methodName;
    }

    public long getLogTime() {
        return this.logTime;
    }

    public void setLogTime(final long logTime) {
        this.logTime = logTime;
    }

    public int getRpcType() {
        return this.rpcType;
    }

    public void setRpcType(final int rpcType) {
        this.rpcType = rpcType;
    }

    public String getTraceId() {
        return this.traceId;
    }

    public String getRpcId() {
        return this.rpcId;
    }

    public String getCallBackMsg() {
        return this.callBackMsg;
    }

    public void setCallBackMsg(final String callBackMsg) {
        this.callBackMsg = callBackMsg;
    }
}
