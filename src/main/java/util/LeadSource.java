package util;

public enum LeadSource {
    WEBSITE(1, "Website"),
    REFERRAL(2, "Referral"),
    SOCIAL_MEDIA(3, "Social Media"),
    EMAIL_CAMPAIGN(4, "Email Campaign"),
    COLD_OUTREACH(5, "Cold Outreach"),
    JUSTDIAL(6, "Justdial"),
    INDIAMART(7, "IndiaMART"),
    OTHERS(8, "Others");

    private final int id;
    private final String label;

    LeadSource(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() { return id; }
    public String getLabel() { return label; }

    public static String labelOf(Long id) {
        if (id == null) return "-";
        for (LeadSource s : values()) {
            if (s.id == id) return s.label;
        }
        return "-";
    }
}