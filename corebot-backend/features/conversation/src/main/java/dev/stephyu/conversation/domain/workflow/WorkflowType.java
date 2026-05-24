package dev.stephyu.conversation.domain.workflow;

public enum WorkflowType {
    RESERVATION_CREATE,
    RESERVATION_CHECK,
    RESERVATION_CANCEL,
    ASK_MENU,
    ASK_MENU_ITEM,
    GREETING,
    THANKS,
    GOODBYE,
    UNKNOWN;

    public String messageKeyPrefix() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
