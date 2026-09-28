package service;

import dto.BisIsiAmcQuotationForm;
import model.BisIsiAmcQuotation;
import model.BisIsiAmcQuotationRevision;
import model.BisIsiOperation;
import model.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import repository.BisIsiAmcQuotationRepository;
import repository.BisIsiAmcQuotationRevisionRepository;
import repository.ProjectStatusRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BisIsiAmcQuotationService {
    private final BisIsiService operations;
    private final BisIsiAmcQuotationRepository quotations;
    private final BisIsiAmcQuotationRevisionRepository revisions;
    private final ProjectStatusRepository projectStatuses;

    public BisIsiAmcQuotationService(BisIsiService operations, BisIsiAmcQuotationRepository quotations,
                                     BisIsiAmcQuotationRevisionRepository revisions, ProjectStatusRepository projectStatuses) {
        this.operations = operations;
        this.quotations = quotations;
        this.revisions = revisions;
        this.projectStatuses = projectStatuses;
    }

    public Map<Long, BisIsiAmcQuotation> forOperations(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return quotations.findByOperationIdIn(ids).stream()
                .collect(Collectors.toMap(BisIsiAmcQuotation::getOperationId, quote -> quote));
    }

    public Map<String, Object> form(User user, long operationId, Integer requestedRevision) {
        BisIsiOperation operation = licensedOperation(user, operationId);
        BisIsiAmcQuotation quote = quotations.findByOperationId(operationId).orElse(null);
        List<Integer> savedNumbers = revisions.findByOperationIdOrderByRevisionNumberAsc(operationId).stream()
                .map(BisIsiAmcQuotationRevision::getRevisionNumber).collect(Collectors.toCollection(java.util.ArrayList::new));
        int active = quote == null ? 0 : revisionOf(quote);
        if (active > 0 && !savedNumbers.contains(active)) savedNumbers.add(active);
        savedNumbers.sort(Integer::compareTo);
        int next = savedNumbers.stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        int selected = requestedRevision == null ? (active == 0 ? 1 : active) : requestedRevision;
        if (selected != next && !savedNumbers.contains(selected))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quotation revision not found");
        boolean draft = selected == next;
        BisIsiAmcQuotationRevision savedRevision = !draft && selected != active
                ? revisions.findByOperationIdAndRevisionNumber(operationId, selected)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quotation revision not found")) : null;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("editing", !draft);
        result.put("revisionNumber", selected);
        result.put("activeRevision", active);
        result.put("nextRevision", next);
        result.put("revisions", savedNumbers);
        result.put("referenceNumber", draft ? "" : savedRevision == null ? referenceNumber(quote) : savedRevision.getReferenceNumber());
        result.put("proposalDate", draft ? (quote == null ? LocalDate.now() : null) : savedRevision == null ? quote.getProposalDate() : savedRevision.getProposalDate());
        result.put("kindAttention", draft ? "" : savedRevision == null ? quote.getKindAttention() : savedRevision.getKindAttention());
        result.put("isStandard", draft ? (quote == null ? operation.getIndianStandard() : "") : savedRevision == null ? quote.getIsStandard() : savedRevision.getIsStandard());
        result.put("product", draft ? "" : savedRevision == null ? quote.getProduct() : savedRevision.getProduct());
        result.put("cmlNumber", draft ? (quote == null ? operation.getCmlNumber() : "") : savedRevision == null ? quote.getCmlNumber() : savedRevision.getCmlNumber());
        result.put("licenceValidityDate", draft ? null : savedRevision == null ? quote.getLicenceValidityDate() : savedRevision.getLicenceValidityDate());
        result.put("actualMarkingFee", draft ? "" : savedRevision == null ? quote.getActualMarkingFee() : savedRevision.getActualMarkingFee());
        result.put("sampleTestingFee", draft ? "" : savedRevision == null ? quote.getSampleTestingFee() : savedRevision.getSampleTestingFee());
        result.put("engineerVisitCharge", draft ? "" : savedRevision == null ? quote.getEngineerVisitCharge() : savedRevision.getEngineerVisitCharge());
        result.put("consultancyServiceFee", draft ? "" : savedRevision == null ? quote.getConsultancyServiceFee() : savedRevision.getConsultancyServiceFee());
        result.put("consultancyOneYear", draft ? "" : savedRevision == null ? quote.getConsultancyOneYear() : savedRevision.getConsultancyOneYear());
        return result;
    }

    @Transactional
    public BisIsiAmcQuotation save(User user, long operationId, BisIsiAmcQuotationForm form) {
        licensedOperation(user, operationId);
        if (form.getProposalDate() == null) throw new IllegalArgumentException("Proposal date is required.");
        BisIsiAmcQuotation quote = quotations.findByOperationId(operationId).orElseGet(() -> {
            BisIsiAmcQuotation created = new BisIsiAmcQuotation();
            created.setOperationId(operationId);
            return created;
        });
        int active = quote.getId() == null ? 0 : revisionOf(quote);
        List<Integer> savedNumbers = revisions.findByOperationIdOrderByRevisionNumberAsc(operationId).stream()
                .map(BisIsiAmcQuotationRevision::getRevisionNumber).toList();
        int next = Math.max(active, savedNumbers.stream().mapToInt(Integer::intValue).max().orElse(0)) + 1;
        int selected = form.getRevisionNumber() == null ? (active == 0 ? 1 : active) : form.getRevisionNumber();
        if (form.isNewRevision() && selected != next)
            throw new IllegalArgumentException("A newer quotation revision has already been saved. Reopen the form before creating another.");
        if (selected != next && selected != active && !savedNumbers.contains(selected))
            throw new IllegalArgumentException("Select an existing revision or create the next revision with New.");
        if (quote.getId() != null && active != selected) ensureSnapshot(quote, active);
        quote.setProposalDate(form.getProposalDate());
        quote.setKindAttention(clean(form.getKindAttention()));
        quote.setIsStandard(clean(form.getIsStandard()));
        quote.setProduct(clean(form.getProduct()));
        quote.setCmlNumber(clean(form.getCmlNumber()));
        quote.setLicenceValidityDate(form.getLicenceValidityDate());
        quote.setActualMarkingFee(clean(form.getActualMarkingFee()));
        quote.setSampleTestingFee(clean(form.getSampleTestingFee()));
        quote.setEngineerVisitCharge(clean(form.getEngineerVisitCharge()));
        quote.setConsultancyServiceFee(clean(form.getConsultancyServiceFee()));
        quote.setConsultancyOneYear(clean(form.getConsultancyOneYear()));
        BisIsiAmcQuotation saved = quotations.saveAndFlush(quote);
        saved.setReferenceNumber(baseReference(saved) + "/R" + selected);
        saved = quotations.saveAndFlush(saved);
        BisIsiAmcQuotationRevision snapshot = revisions.findByOperationIdAndRevisionNumber(operationId, selected)
                .orElseGet(BisIsiAmcQuotationRevision::new);
        snapshot.copyFrom(saved, selected);
        revisions.save(snapshot);
        return saved;
    }

    public BisIsiAmcQuotation forDownload(User user, long operationId, Long quotationId) {
        licensedOperation(user, operationId);
        BisIsiAmcQuotation quotation = quotations.findByOperationId(operationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quotation not found"));
        if (quotationId != null && !quotationId.equals(quotation.getId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quotation not found");
        return quotation;
    }

    public String referenceNumber(BisIsiAmcQuotation quotation) {
        String current = quotation.getReferenceNumber();
        if (current != null && !current.isBlank() && !current.startsWith("AMC-SIT-")) return current;
        return baseReference(quotation) + "/R1";
    }

    private String baseReference(BisIsiAmcQuotation quotation) {
        String current = quotation.getReferenceNumber();
        if (current != null && current.matches("^EVTL/[^/]+/SIT/AMC/\\d+/R\\d+$"))
            return current.substring(0, current.lastIndexOf("/R"));
        return String.format(Locale.ROOT, "EVTL/2026-27/SIT/AMC/%03d", quotation.getId());
    }

    private int revisionOf(BisIsiAmcQuotation quotation) {
        String reference = quotation.getReferenceNumber();
        if (reference != null && reference.matches("^EVTL/[^/]+/SIT/AMC/\\d+/R\\d+$"))
            return Integer.parseInt(reference.substring(reference.lastIndexOf("/R") + 2));
        return 1;
    }

    private void ensureSnapshot(BisIsiAmcQuotation quote, int revision) {
        if (revisions.findByOperationIdAndRevisionNumber(quote.getOperationId(), revision).isPresent()) return;
        BisIsiAmcQuotationRevision snapshot = new BisIsiAmcQuotationRevision();
        snapshot.copyFrom(quote, revision);
        if (quote.getReferenceNumber() == null || quote.getReferenceNumber().startsWith("AMC-SIT-")) {
            quote.setReferenceNumber(baseReference(quote) + "/R" + revision);
            snapshot.copyFrom(quote, revision);
        }
        revisions.save(snapshot);
    }

    private BisIsiOperation licensedOperation(User user, long id) {
        BisIsiOperation operation = operations.get(user, id);
        if ("License Granted".equalsIgnoreCase(operation.getProjectStatus())) return operation;
        String legacyId = operation.getLegacyProjectId();
        if (legacyId != null && (operation.getProjectStatus() == null || operation.getProjectStatus().isBlank())) {
            try {
                if (projectStatuses.findById(Long.parseLong(legacyId))
                        .map(status -> "License Granted".equalsIgnoreCase(status.getProjectStatus())).orElse(false)) return operation;
            } catch (NumberFormatException ignored) { }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "AMC operation not found");
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.length() > 255) throw new IllegalArgumentException("Each quotation field must be 255 characters or fewer.");
        return trimmed;
    }
}
