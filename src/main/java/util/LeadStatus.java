package util;

public enum LeadStatus {
    FRESH_LEADS(1, "Fresh Leads"),
    FOLLOWING_UP(2, "Following Up"),
    HOT_FOLLOW_UP(3, "Hot Follow Up"),
    CONVERTED(4, "Converted"),
    DENIED(5, "Denied"),
    PENDING(6, "Pending"),
    WRONG_LEAD(7, "Wrong Lead"),
    CALL_NOT_PICKED(8, "Call Not Picked");

    private final int id;
    private final String label;

    LeadStatus(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() { return id; }
    public String getLabel() { return label; }

    public static String labelOf(String idAsString) {
        if (idAsString == null) return "-";
        try {
            int id = Integer.parseInt(idAsString);
            for (LeadStatus s : values()) {
                if (s.id == id) return s.label;
            }
        } catch (NumberFormatException ignored) { }
        return "-";
    }
}