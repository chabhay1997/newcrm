package enums;

public enum InvoiceStatus {
    PAID(1, "Paid"),
    HALF(2, "Half"),
    UNPAID(3, "UnPaid");

    private final int id;
    private final String label;

    InvoiceStatus(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() { return id; }
    public String getLabel() { return label; }

    public static InvoiceStatus fromId(Integer id) {
        if (id == null) return UNPAID;
        for (InvoiceStatus s : values()) {
            if (s.id == id) return s;
        }
        return UNPAID;
    }

    public static boolean isValid(Integer id) {
        if (id == null) return false;
        for (InvoiceStatus s : values()) {
            if (s.id == id) return true;
        }
        return false;
    }
}