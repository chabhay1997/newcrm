package enums;

/** Encapsulates everything that varies by invoice type — labels, slugs, prefixes, tax defaults. */
public enum InvoiceType {

    EVTL(1, "Evtl India", "evtl", "EVTL/ACC/26-27/", "EVTL/PO/26-27/", "07AAFCE4483M1Z7", "AAFCE4483M"),
    PROLIX(2, "Prolix", "prolix", "PROLIX/ACC/26-27/", "PROLIX/PO/26-27/", "09MXYPS7492H1ZW", "MXYPS7492H"),
    GOVERNMENT(3, "Government", "government", "GOV/ACC/26-27/", "GOV/PO/26-27/", "", "");

    private final int id;
    private final String label;
    private final String slug;
    private final String invoicePrefix;
    private final String poPrefix;
    private final String defaultGstin;
    private final String defaultPan;

    InvoiceType(int id, String label, String slug, String invoicePrefix, String poPrefix, String defaultGstin, String defaultPan) {
        this.id = id;
        this.label = label;
        this.slug = slug;
        this.invoicePrefix = invoicePrefix;
        this.poPrefix = poPrefix;
        this.defaultGstin = defaultGstin;
        this.defaultPan = defaultPan;
    }

    public int getId() { return id; }
    public String getLabel() { return label; }
    public String getSlug() { return slug; }
    public String getInvoicePrefix() { return invoicePrefix; }
    public String getPoPrefix() { return poPrefix; }
    public String getDefaultGstin() { return defaultGstin; }
    public String getDefaultPan() { return defaultPan; }

    public static InvoiceType fromId(Integer id) {
        if (id == null) return EVTL;
        for (InvoiceType t : values()) {
            if (t.id == id) return t;
        }
        return EVTL;
    }

    /** Mirrors Laravel's switch(strtolower($type)) in index(). */
    public static InvoiceType fromSlug(String slug) {
        if (slug == null) return EVTL;
        for (InvoiceType t : values()) {
            if (t.slug.equalsIgnoreCase(slug)) return t;
        }
        return EVTL;
    }

    public static String labelFor(Integer id) {
        return fromId(id).label;
    }
}