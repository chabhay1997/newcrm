package dto;

import java.time.LocalDateTime;

public class LeadMeetingReminderRequest {
    private LocalDateTime remindAt;
    private String message;

    public LocalDateTime getRemindAt() { return remindAt; }
    public void setRemindAt(LocalDateTime remindAt) { this.remindAt = remindAt; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}