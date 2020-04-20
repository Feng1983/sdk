import java.io.IOException;

class DefaultRpcContextEncoder extends BaseContextEncoder{
     private int DEFAULT_BUFFER_SIZE;
     private StringBuilder buffer;

     DefaultRpcContextEncoder() {
         this.DEFAULT_BUFFER_SIZE = 256;
         this.buffer = new StringBuilder(this.DEFAULT_BUFFER_SIZE);
     }

     @Override
     public void encode(final BaseContext base, final EagleEyeAppender eea) throws IOException {
         if (base instanceof AbstractContext) {
             final AbstractContext ctx = (AbstractContext)base;
             final StringBuilder buffer = this.buffer;
             buffer.delete(0, buffer.length());
             switch (ctx.logType) {
                 case 1: {
                     buffer.append(ctx.traceId).append('|').append(ctx.startTime).append('|').append(ctx.rpcType).append('|').append(ctx.logTime - ctx.startTime).append('|');
                     if (ctx.rpcId != null && ctx.rpcId.length() >= 3) {
                         buffer.append(ctx.rpcId).append('|');
                     }
                     if (EagleEyeCoreUtils.isNotBlank(ctx.serviceName)) {
                         buffer.append(ctx.serviceName).append('|');
                     }
                     buffer.append(ctx.traceName);
                     break;
                 }
                 case 2: {
                     buffer.append(ctx.traceId).append('|').append(ctx.startTime).append('|').append(ctx.rpcType).append('|').append(ctx.rpcId).append('|').append(ctx.serviceName).append('|').append(ctx.methodName).append('|').append(ctx.remoteIp).append('|').append('[').append(ctx.span0).append(", ").append(ctx.span1).append(']').append('|').append(ctx.traceName).append('|').append(ctx.requestSize).append('|').append(ctx.responseSize);
                     break;
                 }
                 case 3: {
                     buffer.append(ctx.traceId).append('|').append(ctx.startTime).append('|').append(ctx.rpcType).append('|').append(ctx.rpcId).append('|');
                     if (EagleEyeCoreUtils.isNotBlank(ctx.serviceName)) {
                         buffer.append(ctx.serviceName).append('|');
                         buffer.append(ctx.methodName).append('|');
                     }
                     if (EagleEyeCoreUtils.isNotBlank(ctx.traceName)) {
                         buffer.append(ctx.traceName).append('|');
                     }
                     buffer.append(ctx.remoteIp).append('|').append(ctx.logTime - ctx.startTime).append('|').append(ctx.responseSize);
                     break;
                 }
                 case 4: {
                     buffer.append(ctx.traceId).append('|').append(ctx.logTime).append('|').append(ctx.rpcType);
                     if (EagleEyeCoreUtils.isNotBlank(ctx.rpcId)) {
                         buffer.append('|').append(ctx.rpcId);
                         break;
                     }
                     break;
                 }
                 case 5: {
                     buffer.append(ctx.traceId).append('|').append(ctx.logTime).append('|').append(ctx.rpcType).append('|').append(ctx.traceName).append('|').append(ctx.callBackMsg).append("\r\n");
                     eea.append(buffer.toString());
                     return;
                 }
                 default: {
                     return;
                 }
             }
             if (EagleEyeCoreUtils.isNotBlank(ctx.callBackMsg)) {
                 buffer.append('|').append(ctx.callBackMsg);
             }
             final int samplingInterval = EagleEye.getSamplingInterval();
             if (samplingInterval >= 2 && samplingInterval <= 9999) {
                 buffer.append("|#").append(samplingInterval);
             }
             if (!"".equals(ctx.localId) && ctx.localId != null) {
                 buffer.append("|!").append(ctx.getLocalId());
             }
             ctx.logContextData(buffer);
             buffer.append("\r\n");
             eea.append(buffer.toString());
         }
     }
}
