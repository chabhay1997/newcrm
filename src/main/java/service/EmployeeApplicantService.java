package service;

import model.EmployeeApplicant;
import org.springframework.stereotype.Service;
import repository.EmployeeApplicantRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
public class EmployeeApplicantService {

    private final EmployeeApplicantRepository repository;

    public EmployeeApplicantService(EmployeeApplicantRepository repository) {
        this.repository = repository;
    }

    public List<EmployeeApplicant> findAll() {
        return repository.findAllForListing();
    }

    public Page<EmployeeApplicant> findPage(String search, String size, int page) {
        String normalizedSearch = search == null ? "" : search.trim();
        String normalizedSize = size == null || size.trim().isBlank() ? "all" : size.trim();

        if ("all".equalsIgnoreCase(normalizedSize)) {
            List<EmployeeApplicant> applicants = normalizedSearch.isBlank()
                    ? findAll()
                    : repository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContainingIgnoreCaseOrderByCreatedAtDesc(
                            normalizedSearch, normalizedSearch, normalizedSearch, Pageable.unpaged()).getContent();
            int safePageSize = Math.max(applicants.size(), 1);
            return new PageImpl<>(applicants, PageRequest.of(0, safePageSize), applicants.size());
        }

        int requestedPage = Math.max(page, 0);
        Page<EmployeeApplicant> result = queryPage(normalizedSearch,
                PageRequest.of(requestedPage, pageSize(normalizedSize)));

        // A page number can remain in the URL after a search or bulk deletion.
        // Do not make an existing list appear empty solely because that page no
        // longer exists; show the final available page instead.
        if (result.getTotalPages() > 0 && requestedPage >= result.getTotalPages()) {
            result = queryPage(normalizedSearch,
                    PageRequest.of(result.getTotalPages() - 1, pageSize(normalizedSize)));
        }
        return result;
    }

    private Page<EmployeeApplicant> queryPage(String search, Pageable pageable) {
        if (search.isBlank()) {
            return repository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return repository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContainingIgnoreCaseOrderByCreatedAtDesc(
                search, search, search, pageable);
    }

    public List<EmployeeApplicant> findForExport(String search) {
        return findPage(search, "all", 0).getContent();
    }

    public int pageSize(String size) {
        return switch (size == null ? "25" : size) {
            case "50" -> 50;
            case "100" -> 100;
            default -> 25;
        };
    }

    public EmployeeApplicant findById(Long id) {
        return repository.findById(Objects.requireNonNull(id, "Applicant id is required"))
                .orElseThrow(() -> new IllegalArgumentException("Employee applicant not found"));
    }

    public EmployeeApplicant save(EmployeeApplicant applicant) {
        return repository.save(Objects.requireNonNull(applicant, "Applicant is required"));
    }

    public void delete(Long id) {
        Long applicantId = Objects.requireNonNull(id, "Applicant id is required");
        if (!repository.existsById(applicantId)) {
            throw new IllegalArgumentException("Employee applicant not found");
        }
        repository.deleteById(applicantId);
    }

    public long deleteOlderThanDays(int days) {
        return repository.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(days));
    }

    public void deleteAll(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        repository.deleteAllByIdInBatch(ids);
    }
}
