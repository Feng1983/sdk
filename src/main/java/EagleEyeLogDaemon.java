import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class EagleEyeLogDaemon implements Runnable{
     private static final File bizOnFile;
     private static final File bizOffFile;
     private static final File rpcOnFile;
     private static final File rpcOffFile;
     private static final File samplingFile;
     private static final long LOG_CHECK_INTERVAL;
     private static final long INDEX_FLUSH_INTERVAL;
     private static long nextIndexFlushTime;
     private static AtomicBoolean running;
     private static final CopyOnWriteArrayList<EagleEyeAppender> watchedAppenders;

     static final EagleEyeAppender watch(final EagleEyeAppender appender) {
         EagleEyeLogDaemon.watchedAppenders.addIfAbsent(appender);
         return appender;
     }

     static final boolean unwatch(final EagleEyeAppender appender) {
         return EagleEyeLogDaemon.watchedAppenders.remove(appender);
     }

     @Override
     public void run() {
         while (true) {
             this.checkFileSwitches();
             this.cleanupFiles();
             try {
                 Thread.sleep(EagleEyeLogDaemon.LOG_CHECK_INTERVAL);
             }
             catch (InterruptedException e) {
                 EagleEye.selfLog("[ERROR] LogDeleteThread Interrupted", e);
             }
             this.outputIndexes();
             this.flushAndReload();
             this.resetExceptionThreshold();
         }
     }

     private void resetExceptionThreshold() {
         final int suppressed = EagleEye.exceptionThreshold.getAndSet(0) - EagleEye.MAX_EXCEPTION_COUNT;
         if (suppressed > 0) {
             EagleEye.selfLog("[ERROR] Suppressed " + suppressed + " exceptions in last " + EagleEyeLogDaemon.LOG_CHECK_INTERVAL + " millis");
         }
     }

     private void cleanupFiles() {
         for (final EagleEyeAppender watchedAppender : EagleEyeLogDaemon.watchedAppenders) {
             try {
                 watchedAppender.cleanup();
             }
             catch (Exception e) {
                 EagleEye.selfLog("[ERROR] fail to cleanup: " + watchedAppender, e);
             }
         }
     }

     private void flushAndReload() {
         for (final EagleEyeAppender watchedAppender : EagleEyeLogDaemon.watchedAppenders) {
             try {
                 watchedAppender.reload();
             }
             catch (Exception e) {
                 EagleEye.selfLog("[ERROR] fail to reload: " + watchedAppender, e);
             }
         }
     }

     private void outputIndexes() {
         try {
             final long now = System.currentTimeMillis();
             if (now >= EagleEyeLogDaemon.nextIndexFlushTime) {
                 EagleEyeLogDaemon.nextIndexFlushTime = now + EagleEyeLogDaemon.INDEX_FLUSH_INTERVAL;
                 for (final Map.Entry<String, String> entry : EagleEye.indexes.entrySet()) {
                     EagleEye.index(9, entry.getValue(), entry.getKey());
                 }
             }
         }
         catch (Exception e) {
             EagleEye.selfLog("[ERROR] Output index table error", e);
         }
     }

     private void checkFileSwitches() {
         try {
             if (EagleEye.isBizOff() && EagleEyeLogDaemon.bizOnFile.exists()) {
                 EagleEye.turnBizOn();
             }
             else if (!EagleEye.isBizOff() && EagleEyeLogDaemon.bizOffFile.exists()) {
                 EagleEye.turnBizOff();
             }
             if (EagleEye.isRpcOff() && EagleEyeLogDaemon.rpcOnFile.exists()) {
                 EagleEye.turnRpcOn();
             }
             else if (!EagleEye.isRpcOff() && EagleEyeLogDaemon.rpcOffFile.exists()) {
                 EagleEye.turnRpcOff();
             }
             this.readIntervalFromFile();
         }
         catch (Exception e) {
             EagleEye.selfLog("[ERROR] Check on/off file error", e);
         }
     }

     private void readIntervalFromFile() {
         final long len = EagleEyeLogDaemon.samplingFile.length();
         if (len > 0L && len < 16L) {
             final String str = this.readLineFile(EagleEyeLogDaemon.samplingFile);
             if (str != null && str.length() > 0) {
                 try {
                     final int sampling = Integer.parseInt(str);
                     if (sampling != EagleEye.getSamplingInterval()) {
                         EagleEye.setSamplingInterval(sampling);
                     }
                 }
                 catch (Exception ex) {}
             }
         }
     }

     private String readLineFile(final File file) {
         BufferedReader br = null;
         try {
             br = new BufferedReader(new FileReader(file), 128);
             return br.readLine();
         }
         catch (Exception e) {}
         finally {
             if (br != null) {
                 try {
                     br.close();
                 }
                 catch (IOException ex) {}
             }
         }
         return null;
     }

     static void start() {
         if (EagleEyeLogDaemon.running.compareAndSet(false, true)) {
             final Thread deleteLogThread = new Thread(new EagleEyeLogDaemon());
             deleteLogThread.setDaemon(true);
             deleteLogThread.setName("EagleEye-LogCheck-Thread");
             deleteLogThread.start();
         }
     }

     static void flushAndWait() {
         for (final EagleEyeAppender watchedAppender : EagleEyeLogDaemon.watchedAppenders) {
             try {
                 if (watchedAppender instanceof AsyncAppender) {
                     ((AsyncAppender)watchedAppender).flushAndWait();
                 }
                 else {
                     watchedAppender.flush();
                 }
             }
             catch (Exception e) {
                 EagleEye.selfLog("[ERROR] fail to flush: " + watchedAppender, e);
             }
         }
     }

     private EagleEyeLogDaemon() {
     }


    static {
         bizOnFile = new File(EagleEye.EAGLEEYE_LOG_DIR + "biz_eagleeye.on");
         bizOffFile = new File(EagleEye.EAGLEEYE_LOG_DIR + "biz_eagleeye.off");
         rpcOnFile = new File(EagleEye.EAGLEEYE_LOG_DIR + "rpc_eagleeye.on");
         rpcOffFile = new File(EagleEye.EAGLEEYE_LOG_DIR + "rpc_eagleeye.off");
         samplingFile = new File(EagleEye.EAGLEEYE_LOG_DIR + "eagleeye_sampling");
         LOG_CHECK_INTERVAL = TimeUnit.SECONDS.toMillis(20L);
         INDEX_FLUSH_INTERVAL = TimeUnit.HOURS.toMillis(12L);
         EagleEyeLogDaemon.nextIndexFlushTime = System.currentTimeMillis() + EagleEyeLogDaemon.INDEX_FLUSH_INTERVAL;
         EagleEyeLogDaemon.running = new AtomicBoolean(false);
         watchedAppenders = new CopyOnWriteArrayList<EagleEyeAppender>();
     }
}
