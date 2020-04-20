public class TraceGroup {
    private final String appName;
    private String key1;
    private String key2;
    private String key3;

    public TraceGroup(final String appName) {
        this.key1 = "";
        this.key2 = "";
        this.key3 = "";
        this.appName = appName;
    }

    @Override
    public String toString() {
        return "TraceGroup [appName=" + this.appName + ", key1=" + this.key1 + ", key2=" + this.key2 + ", key3=" + this.key3 + "]";
    }

    public String getAppName() {
        return this.appName;
    }

    public TraceGroup key1(final String key1) {
        this.key1 = key1;
        return this;
    }

    public String getKey1() {
        return this.key1;
    }

    public TraceGroup key2(final String key2) {
        this.key2 = key2;
        return this;
    }

    public String getKey2() {
        return this.key2;
    }

    public TraceGroup key3(final String key3) {
        this.key3 = key3;
        return this;
    }

    public String getKey3() {
        return this.key3;
    }
}
