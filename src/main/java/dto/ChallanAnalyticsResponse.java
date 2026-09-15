package dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ChallanAnalyticsResponse(Summary summary, List<MonthlyPoint> monthlyTrend,
                                       List<Breakdown> topClients, List<Breakdown> topItems,
                                       List<Breakdown> stateDistribution) {
    public record Summary(long totalChallans, BigDecimal totalValue, BigDecimal totalQuantity,
                          long uniqueClients, BigDecimal averageValue, LocalDate startingDate,
                          LocalDate endingDate) { }

    public record MonthlyPoint(String month, String label, long count, BigDecimal value) { }

    public record Breakdown(String label, long count, BigDecimal value) { }
}
