package util;

public enum LeadService {
    BIS_CRS_CERTIFICATION(1, "BIS-CRS Certification"),
    ISI_DOMESTIC_MANUFACTURER(2, "ISI Domestic Manufacturer"),
    BIS_FMCS_REGISTRATION(3, "BIS FMCS Registration"),
    EPR_PLASTIC_WASTE(4, "EPR Plastic Waste"),
    EPR_ELECTRONIC_WASTE(5, "EPR Electronic Waste"),
    EPR_BATTERY_WASTE(6, "EPR Battery Waste"),
    EPR_TYRE_WASTE(7, "EPR Tyre Waste"),
    WPC_ETA_APPROVAL(8, "WPC ETA Approval"),
    BEE_REGISTRATION(9, "BEE Registration"),
    GEM_REGISTRATION(10, "GeM Registration"),
    FSSAI_REGISTRATION(11, "FSSAI Registration"),
    LMPC_REGISTRATION(12, "LMPC Registration"),
    STARTUP_REGISTRATION(13, "Startup Registration"),
    IMEI_REGISTRATION(14, "IMEI Registration"),
    NSIC_REGISTRATION(15, "NSIC Registration"),
    CDSCO_REGISTRATION(16, "CDSCO Registration"),
    NOC_REGISTRATION(17, "NOC Registration"),
    CE_UL_CERTIFICATION(18, "CE and UL Certification"),
    TRADEMARK_REGISTRATION(19, "Trademark Registration"),
    TEC_REGISTRATION(20, "TEC Registration");

    private final int id;
    private final String label;

    LeadService(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() { return id; }
    public String getLabel() { return label; }

    public static String labelOf(Long id) {
        if (id == null) return "-";
        for (LeadService s : values()) {
            if (s.id == id) return s.label;
        }
        return "-";
    }
}