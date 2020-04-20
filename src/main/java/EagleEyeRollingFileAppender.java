import java.io.*;
import java.nio.channels.FileLock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class EagleEyeRollingFileAppender extends EagleEyeAppender{
     private static final long LOG_FLUSH_INTERVAL;
     private static final int DEFAULT_BUFFER_SIZE = 4096;
     private int maxBackupIndex;
     private final long maxFileSize;
     private final int bufferSize = 4096;
     private final String filePath;
     private final AtomicBoolean isRolling;
     private BufferedOutputStream bos;
     private long nextFlushTime;
     private long lastRollOverTime;
     private long outputByteSize;
     private final boolean selfLogEnabled;
     private boolean multiProcessDetected;
     private static final String DELETE_FILE_SUBFIX = ".deleted";

     public EagleEyeRollingFileAppender(final String filePath, final long maxFileSize) {
         this(filePath, maxFileSize, true);
     }

     public EagleEyeRollingFileAppender(final String filePath, final long maxFileSize, final boolean selfLogEnabled) {
         this.maxBackupIndex = 3;
         this.isRolling = new AtomicBoolean(false);
         this.bos = null;
         this.nextFlushTime = 0L;
         this.lastRollOverTime = 0L;
         this.outputByteSize = 0L;
         this.multiProcessDetected = false;
         this.filePath = filePath;
         this.maxFileSize = maxFileSize;
         this.selfLogEnabled = selfLogEnabled;
         this.setFile();
     }

     private void setFile() {
         try {
             final File logFile = new File(this.filePath);
             if (!logFile.exists()) {
                 final File parentFile = logFile.getParentFile();
                 if (!parentFile.exists() && !parentFile.mkdirs()) {
                     this.doSelfLog("[ERROR] Fail to mkdirs: " + parentFile.getAbsolutePath());
                     return;
                 }
                 try {
                     if (!logFile.createNewFile()) {
                         this.doSelfLog("[ERROR] Fail to create file, it exists: " + logFile.getAbsolutePath());
                     }
                 }
                 catch (IOException e) {
                     this.doSelfLog("[ERROR] Fail to create file: " + logFile.getAbsolutePath() + ", error=" + e.getMessage());
                 }
             }
             if (!logFile.isFile() || !logFile.canWrite()) {
                 this.doSelfLog("[ERROR] Invalid file, exists=" + logFile.exists() + ", isFile=" + logFile.isFile() + ", canWrite=" + logFile.canWrite() + ", path=" + logFile.getAbsolutePath());
                 return;
             }
             final FileOutputStream ostream = new FileOutputStream(logFile, true);
             this.bos = new BufferedOutputStream(ostream, 4096);
             this.lastRollOverTime = System.currentTimeMillis();
             this.outputByteSize = logFile.length();
         }
         catch (Throwable e2) {
             this.doSelfLog("[ERROR] Fail to create file to write: " + this.filePath + ", error=" + e2.getMessage());
         }
     }

     @Override
     public void append(final String log) {
         final BufferedOutputStream bos = this.bos;
         if (bos != null) {
             try {
                 this.waitUntilRollFinish();
                 final byte[] bytes = log.getBytes(EagleEye.DEFAULT_CHARSET);
                 int len = bytes.length;
                 if (len > 4096 && this.multiProcessDetected) {
                     len = 4096;
                     bytes[len - 1] = 10;
                 }
                 bos.write(bytes, 0, len);
                 this.outputByteSize += len;
                 if (this.outputByteSize >= this.maxFileSize) {
                     this.rollOver();
                 }
                 else if (System.currentTimeMillis() >= this.nextFlushTime) {
                     this.flush();
                 }
             }
             catch (Exception e) {
                 this.doSelfLog("[ERROR] fail to write log to file " + this.filePath + ", error=" + e.getMessage());
                 this.close();
                 this.setFile();
             }
         }
     }

     @Override
     public void flush() {
         final BufferedOutputStream bos = this.bos;
         if (bos != null) {
             try {
                 bos.flush();
                 this.nextFlushTime = System.currentTimeMillis() + EagleEyeRollingFileAppender.LOG_FLUSH_INTERVAL;
             }
             catch (Exception e) {
                 this.doSelfLog("[WARN] Fail to flush OutputStream: " + this.filePath + ", " + e.getMessage());
             }
         }
     }

     @Override
     public void rollOver() {
         final String lockFilePath = this.filePath + ".lock";
         final File lockFile = new File(lockFilePath);
         RandomAccessFile raf = null;
         FileLock fileLock = null;
         if (!this.isRolling.compareAndSet(false, true)) {
             return;
         }
         try {
             raf = new RandomAccessFile(lockFile, "rw");
             fileLock = raf.getChannel().tryLock();
             if (fileLock != null) {
                 final int maxBackupIndex = this.maxBackupIndex;
                 this.reload();
                 if (this.outputByteSize >= this.maxFileSize) {
                     File file = new File(this.filePath + '.' + maxBackupIndex);
                     if (file.exists()) {
                         final File target = new File(this.filePath + '.' + maxBackupIndex + ".deleted");
                         if (!file.renameTo(target) && !file.delete()) {
                             this.doSelfLog("[ERROR] Fail to delete or rename file: " + file.getAbsolutePath() + " to " + target.getAbsolutePath());
                         }
                     }
                     for (int i = maxBackupIndex - 1; i >= 1; --i) {
                         file = new File(this.filePath + '.' + i);
                         if (file.exists()) {
                             final File target = new File(this.filePath + '.' + (i + 1));
                             if (!file.renameTo(target) && !file.delete()) {
                                 this.doSelfLog("[ERROR] Fail to delete or rename file: " + file.getAbsolutePath() + " to " + target.getAbsolutePath());
                             }
                         }
                     }
                     final File target = new File(this.filePath + "." + 1);
                     this.close();
                     file = new File(this.filePath);
                     if (file.renameTo(target)) {
                         this.doSelfLog("[INFO] File rolled to " + target.getAbsolutePath() + ", " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - this.lastRollOverTime) + " minutes since last roll");
                     }
                     else {
                         this.doSelfLog("[WARN] Fail to rename file: " + file.getAbsolutePath() + " to " + target.getAbsolutePath());
                     }
                     this.setFile();
                 }
             }
         }
         catch (IOException e) {
             this.doSelfLog("[ERROR] Fail rollover file: " + this.filePath + ", error=" + e.getMessage());
         }
         finally {
             this.isRolling.set(false);
             if (fileLock != null) {
                 try {
                     fileLock.release();
                 }
                 catch (IOException e2) {
                     this.doSelfLog("[ERROR] Fail to release file lock: " + lockFilePath + ", error=" + e2.getMessage());
                 }
             }
             if (raf != null) {
                 try {
                     raf.close();
                 }
                 catch (IOException e2) {
                     this.doSelfLog("[WARN] Fail to close file lock: " + lockFilePath + ", error=" + e2.getMessage());
                 }
             }
             if (fileLock != null && !lockFile.delete() && lockFile.exists()) {
                 this.doSelfLog("[WARN] Fail to delete file lock: " + lockFilePath);
             }
         }
     }

     @Override
     public void close() {
         final BufferedOutputStream bos = this.bos;
         if (bos != null) {
             try {
                 bos.close();
             }
             catch (IOException e) {
                 this.doSelfLog("[WARN] Fail to close OutputStream: " + e.getMessage());
             }
             this.bos = null;
         }
     }

     @Override
     public void reload() {
         this.flush();
         final File logFile = new File(this.filePath);
         final long fileSize = logFile.length();
         if (this.bos == null || fileSize < this.outputByteSize) {
             this.doSelfLog("[INFO] Log file rolled over by outside: " + this.filePath + ", force reload");
             this.close();
             this.setFile();
         }
         else if (fileSize > this.outputByteSize) {
             this.outputByteSize = fileSize;
             if (!this.multiProcessDetected) {
                 this.multiProcessDetected = true;
                 if (this.selfLogEnabled) {
                     this.doSelfLog("[WARN] Multi-process file write detected: " + this.filePath);
                 }
             }
         }
     }

     @Override
     public void cleanup() {
         try {
             final File logFile = new File(this.filePath);
             final File parentDir = logFile.getParentFile();
             if (parentDir != null && parentDir.isDirectory()) {
                 final String baseFileName = logFile.getName();
                 final File[] filesToDelete = parentDir.listFiles(new FilenameFilter() {
                     @Override
                     public boolean accept(final File dir, final String name) {
                         return name != null && name.startsWith(baseFileName) && name.endsWith(".deleted");
                     }
                 });
                 if (filesToDelete != null && filesToDelete.length > 0) {
                     for (final File f : filesToDelete) {
                         final boolean success = f.delete() || !f.exists();
                         if (success) {
                             this.doSelfLog("[INFO] Deleted log file: " + f.getAbsolutePath());
                         }
                         else if (f.exists()) {
                             this.doSelfLog("[ERROR] Fail to delete log file: " + f.getAbsolutePath());
                         }
                     }
                 }
             }
         }
         catch (Exception e) {
             this.doSelfLog("[ERROR] Fail to cleanup log file, error=" + e.getMessage());
         }
     }

     void waitUntilRollFinish() {
         while (this.isRolling.get()) {
             try {
                 Thread.sleep(1L);
             }
             catch (Exception e) {
                 e.printStackTrace();
             }
         }
     }

     private void doSelfLog(final String log) {
         if (this.selfLogEnabled) {
             EagleEye.selfLog(log);
         }
         else {
             System.out.println("[EagleEye]" + log);
         }
     }

     public int getMaxBackupIndex() {
         return this.maxBackupIndex;
     }

     public void setMaxBackupIndex(final int maxBackupIndex) {
         if (maxBackupIndex < 1) {
             throw new IllegalArgumentException("maxBackupIndex < 1: " + maxBackupIndex);
         }
         this.maxBackupIndex = maxBackupIndex;
     }

     @Override
     public String toString() {
         return "EagleEyeRollingFileAppender [filePath=" + this.filePath + "]";
     }

     static {
         LOG_FLUSH_INTERVAL = TimeUnit.SECONDS.toMillis(1L);
     }
}
