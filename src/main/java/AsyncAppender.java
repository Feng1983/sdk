import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

 class AsyncAppender  extends  EagleEyeAppender{
    private static final int DEFAULT_NOTIFY_THRESHOLD = 512;
    static final int LOG_TYPE_EVENT_FLUSH = -1;
    static final BaseContext EVENT_LOG_FLUSH;
    static final int LOG_TYPE_EVENT_ROLLOVER = -2;
    static final BaseContext EVENT_LOG_ROLLOVER;
    static final int LOG_TYPE_EVENT_RELOAD = -3;
    static final BaseContext EVENT_LOG_RELOAD;
    private final BaseContext[] entries;
    private final int queueSize;
    private final int indexMask;
    private final int notifyThreshold;
    private final int maxWaitMillis;
    private final ReentrantLock lock;
    private final Condition notEmpty;
    private AtomicLong putIndex;
    private AtomicLong discardCount;
    private AtomicLong takeIndex;
    private EagleEyeAppender appender;
    private BaseContextEncoder encoder;
    private String workerName;
    private Thread worker;
    private AtomicBoolean running;

    public AsyncAppender(int queueSize, final int maxWaitMillis) {
        queueSize = 1 << 32 - Integer.numberOfLeadingZeros(queueSize - 1);
        this.queueSize = queueSize;
        this.maxWaitMillis = maxWaitMillis;
        this.entries = new BaseContext[queueSize];
        this.indexMask = queueSize - 1;
        this.notifyThreshold = ((queueSize >= 512) ? 512 : queueSize);
        this.putIndex = new AtomicLong(0L);
        this.discardCount = new AtomicLong(0L);
        this.takeIndex = new AtomicLong(0L);
        this.running = new AtomicBoolean(false);
        this.lock = new ReentrantLock(false);
        this.notEmpty = this.lock.newCondition();
    }

    void start(final EagleEyeAppender appender, final BaseContextEncoder encoder, final String workerName) {
        if (appender instanceof AsyncAppender) {
            throw new IllegalArgumentException("nested AsyncAppender is not allow: " + workerName);
        }
        this.appender = EagleEyeCoreUtils.checkNotNull(appender, "appender");
        this.encoder = encoder;
        this.workerName = workerName;
        (this.worker = new Thread(new AsyncRunnable(), "EagleEye-AsyncAppender-Thread-" + workerName)).setDaemon(true);
        this.worker.start();
    }

    int size() {
        return (int)(this.putIndex.get() - this.takeIndex.get());
    }

    boolean append(final BaseContext ctx) {
        final long qsize = this.queueSize;
        long startTime = 0L;
        while (true) {
            final long put = this.putIndex.get();
            final long size = put - this.takeIndex.get();
            if (size >= qsize) {
                if (this.maxWaitMillis <= 0) {
                    this.discardCount.incrementAndGet();
                    return false;
                }
                final long now = System.currentTimeMillis();
                if (startTime == 0L) {
                    startTime = now;
                }
                else if (now - startTime >= this.maxWaitMillis) {
                    this.discardCount.incrementAndGet();
                    return false;
                }
                LockSupport.parkNanos(1000L);
            }
            else {
                if (this.putIndex.compareAndSet(put, put + 1L)) {
                    this.entries[(int)put & this.indexMask] = ctx;
                    if (size >= this.notifyThreshold && !this.running.get() && this.lock.tryLock()) {
                        try {
                            this.notEmpty.signal();
                        }
                        catch (Exception e) {
                            EagleEye.selfLog("[ERROR] fail to signal notEmpty: " + this.workerName, e);
                        }
                        finally {
                            this.lock.unlock();
                        }
                    }
                    return true;
                }
                continue;
            }
        }
    }

    @Override
    public void append(final String log) {
        throw new UnsupportedOperationException("use append(BaseContext ctx) instead in AsyncAppender");
    }

    @Override
    public void rollOver() {
        this.append(AsyncAppender.EVENT_LOG_ROLLOVER);
    }

    @Override
    public void reload() {
        this.append(AsyncAppender.EVENT_LOG_RELOAD);
    }

    @Override
    public void flush() {
        this.append(AsyncAppender.EVENT_LOG_FLUSH);
    }

    @Override
    public void close() {
        final EagleEyeAppender appender0 = this.appender;
        this.appender = new NoOpAppender();
        appender0.close();
    }

    @Override
    public void cleanup() {
        this.appender.cleanup();
    }

    void flushAndWait() {
        this.append(AsyncAppender.EVENT_LOG_FLUSH);
        final long end = System.currentTimeMillis() + 500L;
        while (this.size() > 0 && System.currentTimeMillis() <= end) {
            if (this.running.get()) {
                try {
                    Thread.sleep(1L);
                    continue;
                }
                catch (InterruptedException e2) {
                    break;
                }
            }
            if (this.lock.tryLock()) {
                try {
                    this.notEmpty.signal();
                }
                catch (Exception e) {
                    EagleEye.selfLog("[ERROR] fail to signal notEmpty: " + this.workerName, e);
                }
                finally {
                    this.lock.unlock();
                }
            }
        }
    }

    EagleEyeAppender getEagleEyeAppender() {
        return this.appender;
    }

    void setEagleEyeAppender(final EagleEyeAppender appender) {
        this.appender = EagleEyeCoreUtils.checkNotNull(appender, "appender");
    }

    @Override
    public String toString() {
        return "AsyncAppender [appender=" + this.appender + "]";
    }

    static {
        EVENT_LOG_FLUSH = new BaseContext(-1);
        EVENT_LOG_ROLLOVER = new BaseContext(-2);
        EVENT_LOG_RELOAD = new BaseContext(-3);
    }

    class AsyncRunnable implements Runnable
    {
        @Override
        public void run() {
            final AsyncAppender parent = AsyncAppender.this;
            final int indexMask = parent.indexMask;
            final int queueSize = parent.queueSize;
            final BaseContextEncoder encoder = parent.encoder;
            final String workerName = parent.workerName;
            final BaseContext[] entries = parent.entries;
            final AtomicLong putIndex = parent.putIndex;
            final AtomicLong takeIndex = parent.takeIndex;
            final AtomicLong discardCount = parent.discardCount;
            final AtomicBoolean running = parent.running;
            final ReentrantLock lock = parent.lock;
            final Condition notEmpty = parent.notEmpty;
            final long outputSpan = TimeUnit.MINUTES.toMillis(1L);
            long lastOutputTime = System.currentTimeMillis();
            while (true) {
                try {
                    while (true) {
                        running.set(true);
                        long take = takeIndex.get();
                        long size = putIndex.get() - take;
                        if (size > 0L) {
                            do {
                                int idx;
                                BaseContext ctx;
                                for (idx = ((int)take & indexMask), ctx = entries[idx]; ctx == null; ctx = entries[idx]) {
                                    Thread.yield();
                                }
                                entries[idx] = null;
                                takeIndex.set(++take);
                                --size;
                                this.processContext(ctx, parent.appender, encoder);
                            } while (size > 0L);
                            long discardNum = discardCount.get();
                            final long now;
                            if (discardNum > 0L && (now = System.currentTimeMillis()) - lastOutputTime > outputSpan) {
                                discardNum = discardCount.get();
                                discardCount.lazySet(0L);
                                EagleEye.selfLog("[WARN] " + workerName + " discarded " + discardNum + " logs, queueSize=" + queueSize);
                                lastOutputTime = now;
                            }
                            parent.appender.flush();
                        }
                        else {
                            if (!lock.tryLock()) {
                                continue;
                            }
                            try {
                                running.set(false);
                                notEmpty.await(1L, TimeUnit.SECONDS);
                            }
                            finally {
                                lock.unlock();
                            }
                        }
                    }
                }
                catch (InterruptedException e2) {
                    EagleEye.selfLog("[WARN] " + workerName + " async thread is iterrupted");
                }
                catch (Exception e) {
                    EagleEye.selfLog("[ERROR] Fail to async write log " + workerName, e);
                    continue;
                }
                break;
            }
            running.set(false);
        }

        private final void processContext(final BaseContext ctx, final EagleEyeAppender appender, final BaseContextEncoder encoder) throws IOException {
            final int logType = ctx.logType;
            if (logType < 0) {
                if (logType == -1) {
                    appender.flush();
                }
                else if (logType == -2) {
                    appender.rollOver();
                }
                else if (logType == -3) {
                    appender.reload();
                }
            }
            else {
                encoder.encode(ctx, appender);
            }
        }
    }
}
