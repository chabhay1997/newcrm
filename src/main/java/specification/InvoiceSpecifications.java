package specification;

import jakarta.persistence.criteria.Predicate;
import model.Invoice;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class InvoiceSpecifications {

    private InvoiceSpecifications() {}

    public static Specification<Invoice> byInvType(Integer invType) {
        return (root, query, cb) -> invType == null ? null : cb.equal(root.get("invType"), invType);
    }

    public static Specification<Invoice> byStatus(Integer status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Invoice> createdWithinDays(Integer days) {
        return (root, query, cb) -> {
            if (days == null) return null;
            LocalDateTime since = LocalDate.now().minusDays(days).atStartOfDay();
            return cb.greaterThanOrEqualTo(root.get("createdAt"), since);
        };
    }

    public static Specification<Invoice> createdWithinMonths(Integer months) {
        return (root, query, cb) -> {
            if (months == null) return null;
            LocalDateTime since = LocalDate.now().minusMonths(months).atStartOfDay();
            return cb.greaterThanOrEqualTo(root.get("createdAt"), since);
        };
    }

    public static Specification<Invoice> createdInYear(Integer year) {
        return (root, query, cb) -> {
            if (year == null) return null;
            LocalDateTime start = LocalDate.of(year, 1, 1).atStartOfDay();
            LocalDateTime end = start.plusYears(1);
            return cb.and(cb.greaterThanOrEqualTo(root.get("createdAt"), start), cb.lessThan(root.get("createdAt"), end));
        };
    }

    public static Specification<Invoice> nameContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("invNo")), pattern),
                    cb.like(cb.lower(root.get("poNo")), pattern)
            );
        };
    }

    /** Combines the common filters used by both the listing and the totals query. */
    public static Specification<Invoice> build(Integer invType, Integer statusFilter, Integer monthRange, Integer year, String nameKeyword) {
        return Specification.allOf(
                byInvType(invType),
                byStatus(statusFilter),
                year == null ? createdWithinMonths(monthRange) : createdInYear(year),
                nameContains(nameKeyword)
        );
    }

    public static Specification<Invoice> build(Integer invType, Integer statusFilter, Integer monthRange, String nameKeyword) {
        return build(invType, statusFilter, monthRange, null, nameKeyword);
    }
}
