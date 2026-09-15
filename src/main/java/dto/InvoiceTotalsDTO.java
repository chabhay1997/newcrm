package dto;

public class InvoiceTotalsDTO {

    private double total;
    private double paid;
    private double half;
    private double unpaid;
    private Counts counts;

    public InvoiceTotalsDTO(double total, double paid, double half, double unpaid, Counts counts) {
        this.total = total;
        this.paid = paid;
        this.half = half;
        this.unpaid = unpaid;
        this.counts = counts;
    }

    public double getTotal() { return total; }
    public double getPaid() { return paid; }
    public double getHalf() { return half; }
    public double getUnpaid() { return unpaid; }
    public Counts getCounts() { return counts; }

    public static class Counts {
        private final long total;
        private final long paid;
        private final long half;
        private final long unpaid;

        public Counts(long total, long paid, long half, long unpaid) {
            this.total = total;
            this.paid = paid;
            this.half = half;
            this.unpaid = unpaid;
        }

        public long getTotal() { return total; }
        public long getPaid() { return paid; }
        public long getHalf() { return half; }
        public long getUnpaid() { return unpaid; }
    }
}