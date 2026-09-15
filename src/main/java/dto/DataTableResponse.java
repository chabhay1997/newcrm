package dto;

import java.util.List;

public class DataTableResponse<T> {
    private int draw;
    private long recordsTotal;
    private long recordsFiltered;
    private List<T> data;
    private InvoiceTotalsDTO totals;

    public DataTableResponse(int draw, long recordsTotal, long recordsFiltered, List<T> data, InvoiceTotalsDTO totals) {
        this.draw = draw;
        this.recordsTotal = recordsTotal;
        this.recordsFiltered = recordsFiltered;
        this.data = data;
        this.totals = totals;
    }

    public int getDraw() { return draw; }
    public long getRecordsTotal() { return recordsTotal; }
    public long getRecordsFiltered() { return recordsFiltered; }
    public List<T> getData() { return data; }
    public InvoiceTotalsDTO getTotals() { return totals; }
}