package dev.stephyu.shared.exception;

import java.util.Map;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class BusinessException extends RuntimeException {
    private final int status;
    private final String code;
    private final String type;
    private final String title;
    private final Map<String, Object> properties;

    protected BusinessException(
        int status,
        String code,
        String type,
        String title,
        String detail
    ) {
        this(status, code, type, title, detail, Map.of());
    }

    protected BusinessException(
        int status,
        String code,
        String type,
        String title,
        String detail,
        Map<String, Object> properties
    ) {
        super(detail);
        this.status = status;
        this.code = code;
        this.type = type;
        this.title = title;
        this.properties = properties;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String type() {
        return type;
    }

    public String title() {
        return title;
    }

    public Map<String, Object> properties() {
        return properties;
    }
}
