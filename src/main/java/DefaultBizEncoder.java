import java.io.IOException;

class DefaultBizEncoder extends BaseContextEncoder{
     private int DEFAULT_BUFFER_SIZE;
     private StringBuilder buffer;

     DefaultBizEncoder() {
         this.DEFAULT_BUFFER_SIZE = 256;
         this.buffer = new StringBuilder(this.DEFAULT_BUFFER_SIZE);
     }

     @Override
     public void encode(final BaseContext ctx, final EagleEyeAppender eea) throws IOException {
         if (ctx.logType == 0) {
             final StringBuilder buffer = this.buffer;
             buffer.delete(0, buffer.length());
             buffer.append(ctx.traceId).append('|').append(ctx.logTime).append('|').append(ctx.rpcId).append('|').append(ctx.serviceName).append('|').append(ctx.methodName).append('|').append(ctx.callBackMsg).append("\r\n");
             eea.append(buffer.toString());
         }
     }
}
