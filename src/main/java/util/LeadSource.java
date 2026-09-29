package util;

public enum LeadSource {
    INDIAMART(1, "Indiamart"),
    JUSTDIAL(2, "Justdial"),
    BY_OWN(3, "ByOwn"),
    NEW_EVTL_WEBSITE(4, "New EVTL Website"),
    AXIS_COMPLIANCE(5, "Axis Compliance"),
    GOOGLE_ADS(6, "Google Ads"),
    BY_CALL(7, "By Call"),
    VARUN_SINGH(8, "Varun Singh"),
    AKHIL_SINGH(9, "Akhil Singh"),
    NIKHIL_SINGH(10, "Nikhil Singh"),
    PROLIX_INDIA(11, "Prolix India"),
    APPROACHING_MAIL(12, "Approaching Mail"),
    FACEBOOK_ADS(13, "Facebook Ads"),
    FURNITURE(14, "Furniture"),
    CDSCO_LANDING_PAGE(15, "CDSCO Landing Page");

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