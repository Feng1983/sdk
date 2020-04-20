public class LocalContext_inner extends  AbstractContext{
    LocalContext_inner localParent;
    volatile boolean isEnd;

    LocalContext_inner(final String _traceId, final String _rpcId) {
        super(_traceId, _rpcId);
    }

    LocalContext_inner(final String _traceId, final String _rpcId, final String _localId) {
        super(_traceId, _rpcId);
        this.localId = _localId;
    }

    @Override
    void logContextData(final StringBuilder appender) {
        final boolean appendTraceGroup = this.traceGroup != null;
        if (!appendTraceGroup) {
            return;
        }
        appender.append("|@");
        if (appendTraceGroup) {
            this.doAppendTraceGroup(appender, true);
        }
    }
}
