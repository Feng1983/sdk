import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class LogContext implements Serializable {
    private static final long serialVersionUID = -8830017199467779206L;
    private String logKey;
    private Long userId;
    private String userNick;
    private String bizId;
    private Long operatorId;
    private int operateType;
    private String operateContent;
    private String opLevel;
    private String opItem;
    @Deprecated
    private String[] extendArray;
    private Map<String, String> extendInfos;
    static final char KV_SEPARATOR = '\u0001';
    static final char SEPARATOR = '\u0012';
    static final String ULC_EAGLEEYE_APPID = "ulc";

    public LogContext logKey(final String logKey) {
        this.logKey = logKey;
        return this;
    }

    public LogContext userId(final Long userId) {
        this.userId = userId;
        return this;
    }

    public LogContext userNick(final String userNick) {
        this.userNick = userNick;
        return this;
    }

    public LogContext bizId(final String bizId) {
        this.bizId = bizId;
        return this;
    }

    public LogContext operatorId(final Long operatorId) {
        this.operatorId = operatorId;
        return this;
    }

    public LogContext operateType(final int operateType) {
        this.operateType = operateType;
        return this;
    }

    public LogContext operateContent(final String operateContent) {
        this.operateContent = operateContent;
        return this;
    }

    public LogContext opLevel(final String opLevel) {
        this.opLevel = opLevel;
        return this;
    }

    public LogContext opItem(final String opItem) {
        this.opItem = opItem;
        return this;
    }

    public LogContext extendArray(final String... extendArray) {
        this.extendArray = extendArray;
        return this;
    }

    public String getLogKey() {
        return this.logKey;
    }

    public Long getUserId() {
        return this.userId;
    }

    public String getUserNick() {
        return this.userNick;
    }

    public String getBizId() {
        return this.bizId;
    }

    public Long getOperatorId() {
        return this.operatorId;
    }

    public int getOperateType() {
        return this.operateType;
    }

    public String getOperateContent() {
        return this.operateContent;
    }

    public String getOpLevel() {
        return this.opLevel;
    }

    public String getOpItem() {
        return this.opItem;
    }

    public String[] getExtendArray() {
        return this.extendArray;
    }

    public Map<String, String> getExtendInfos() {
        return this.extendInfos;
    }

    public LogContext extendInfos(final Map<String, String> extendInfos) {
        this.extendInfos = extendInfos;
        return this;
    }

    public LogContext put(final String key, final String value) {
        if (this.extendInfos == null) {
            this.extendInfos = new LinkedHashMap<String, String>();
        }
        this.extendInfos.put(key, value);
        return this;
    }

    public void log() {
        final StringBuilder sBuilder = new StringBuilder();
        sBuilder.append(this.logKey);
        sBuilder.append('\u0012');
        EagleEyeCoreUtils.appendWithNullCheck(this.userId, "0", sBuilder);
        sBuilder.append('\u0012');
        EagleEyeCoreUtils.appendWithBlankCheck(this.bizId, "", sBuilder);
        sBuilder.append('\u0012');
        EagleEyeCoreUtils.appendWithNullCheck(this.operatorId, "0", sBuilder);
        sBuilder.append('\u0012');
        EagleEyeCoreUtils.appendWithNullCheck(this.operateType, "0", sBuilder);
        sBuilder.append('\u0012');
        EagleEyeCoreUtils.appendLog(this.operateContent, sBuilder, '|');
        sBuilder.append('\u0012');
        EagleEyeCoreUtils.appendWithBlankCheck(this.userNick, "", sBuilder);
        sBuilder.append('\u0012');
        EagleEyeCoreUtils.appendWithBlankCheck(this.opLevel, "", sBuilder);
        sBuilder.append('\u0012');
        EagleEyeCoreUtils.appendWithBlankCheck(this.opItem, "", sBuilder);
        if (null != this.extendInfos && this.extendInfos.size() > 0) {
            for (final Map.Entry<String, String> entry : this.extendInfos.entrySet()) {
                final String key = entry.getKey();
                if (EagleEyeCoreUtils.isNotBlank(key)) {
                    final Object value = entry.getValue();
                    sBuilder.append('\u0012').append(key).append('\u0001');
                    if (value == null) {
                        continue;
                    }
                    EagleEyeCoreUtils.appendWithBlankCheck(value.toString(), "", sBuilder);
                }
            }
        }
        EagleEye.businessTag("ulc", this.logKey, sBuilder.toString());
    }
}
