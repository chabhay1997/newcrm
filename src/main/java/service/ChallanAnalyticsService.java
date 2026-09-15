package service;

import dto.ChallanAnalyticsResponse;
import model.Challan;
import model.State;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.ChallanRepository;
import repository.StateRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChallanAnalyticsService {
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
    private final ChallanRepository challanRepository;
    private final StateRepository stateRepository;

    public ChallanAnalyticsService(ChallanRepository challanRepository, StateRepository stateRepository) {
        this.challanRepository = challanRepository;
        this.stateRepository = stateRepository;
    }

    @Transactional(readOnly = true)
    public ChallanAnalyticsResponse analytics(LocalDate startDate, LocalDate endDate) {
        List<Challan> challans = challanRepository.findFiltered("", startDate, endDate, Sort.by("date").ascending().and(Sort.by("id").ascending()));
        Map<Long, State> states = stateRepository.findAllById(challans.stream().map(Challan::getStateId)
                        .filter(Objects::nonNull).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(State::getId, Function.identity()));

        BigDecimal totalValue = challans.stream().map(this::storedValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalQuantity = challans.stream().map(Challan::getQty).map(this::number)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long uniqueClients = challans.stream().map(Challan::getClientName).map(this::label)
                .filter(value -> !value.equals("Unspecified")).map(value -> value.toLowerCase(Locale.ROOT)).distinct().count();
        BigDecimal averageValue = challans.isEmpty() ? BigDecimal.ZERO
                : totalValue.divide(BigDecimal.valueOf(challans.size()), 2, RoundingMode.HALF_UP);
        LocalDate actualStart = startDate != null ? startDate : challans.stream().map(Challan::getDate)
                .filter(Objects::nonNull).min(LocalDate::compareTo).orElse(null);
        LocalDate actualEnd = endDate != null ? endDate : challans.stream().map(Challan::getDate)
                .filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);

        Map<YearMonth, MutableMetric> monthly = new TreeMap<>();
        Map<String, MutableMetric> clients = new HashMap<>();
        Map<String, MutableMetric> items = new HashMap<>();
        Map<String, MutableMetric> stateMetrics = new HashMap<>();
        for (Challan challan : challans) {
            BigDecimal value = storedValue(challan);
            if (challan.getDate() != null) monthly.computeIfAbsent(YearMonth.from(challan.getDate()), key -> new MutableMetric()).add(value);
            clients.computeIfAbsent(label(challan.getClientName()), key -> new MutableMetric()).add(value);
            items.computeIfAbsent(label(challan.getItemName()), key -> new MutableMetric()).add(value);
            State state = challan.getStateId() == null ? null : states.get(challan.getStateId());
            stateMetrics.computeIfAbsent(state == null ? "Unspecified" : label(state.getName()), key -> new MutableMetric()).add(value);
        }

        List<ChallanAnalyticsResponse.MonthlyPoint> trend = monthly.entrySet().stream()
                .map(entry -> new ChallanAnalyticsResponse.MonthlyPoint(entry.getKey().toString(),
                        entry.getKey().format(MONTH_LABEL), entry.getValue().count, money(entry.getValue().value))).toList();
        return new ChallanAnalyticsResponse(
                new ChallanAnalyticsResponse.Summary(challans.size(), money(totalValue), quantity(totalQuantity),
                        uniqueClients, money(averageValue), actualStart, actualEnd),
                trend, breakdown(clients, 8), breakdown(items, 8), breakdown(stateMetrics, 8));
    }

    private List<ChallanAnalyticsResponse.Breakdown> breakdown(Map<String, MutableMetric> metrics, int limit) {
        return metrics.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, MutableMetric>>comparingLong(entry -> entry.getValue().count).reversed()
                        .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
                .limit(limit).map(entry -> new ChallanAnalyticsResponse.Breakdown(entry.getKey(),
                        entry.getValue().count, money(entry.getValue().value))).toList();
    }

    private BigDecimal storedValue(Challan challan) {
        BigDecimal total = number(challan.getTotalAmount());
        return total.signum() != 0 || !blank(challan.getTotalAmount()) ? total : number(challan.getAmount());
    }

    private BigDecimal number(String value) {
        if (blank(value)) return BigDecimal.ZERO;
        try { return new BigDecimal(value.replace(",", "").trim()); }
        catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }

    private String label(String value) { return blank(value) ? "Unspecified" : value.trim(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal quantity(BigDecimal value) { return value.stripTrailingZeros(); }

    private static final class MutableMetric {
        private long count;
        private BigDecimal value = BigDecimal.ZERO;
        private void add(BigDecimal amount) { count++; value = value.add(amount); }
    }
}
