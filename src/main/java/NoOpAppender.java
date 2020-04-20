public class NoOpAppender extends  EagleEyeAppender{
    @Override
    public void append(final String log) {
    }

    @Override
    public void flush() {
    }

    @Override
    public void rollOver() {
    }

    @Override
    public void reload() {
    }

    @Override
    public void close() {
    }

    @Override
    public void cleanup() {
    }

    @Override
    public String toString() {
        return "NoOpAppender";
    }
}
