package service;

import dto.QuotationPageResponse;
import dto.QuotationResponse;
import dto.QuotationSummary;
import dto.QuotationEditRequest;
import dto.QuotationLineItemRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.TestingEquipment;
import model.User;
import model.QuotationEditHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.TestingEquipmentRepository;
import repository.UserRepository;
import repository.QuotationTestingEquipmentRepository;
import repository.QuotationEditHistoryRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuotationService {
    public static final int PAGE_SIZE = 25;

    private final TestingEquipmentRepository testingEquipmentRepository;
    private final UserRepository userRepository;
    private final QuotationTestingEquipmentRepository quotationTestingEquipmentRepository;
    private final QuotationEditHistoryRepository quotationEditHistoryRepository;
    private final ObjectMapper objectMapper;

    public QuotationService(TestingEquipmentRepository testingEquipmentRepository, UserRepository userRepository,
                            QuotationTestingEquipmentRepository quotationTestingEquipmentRepository,
                            QuotationEditHistoryRepository quotationEditHistoryRepository, ObjectMapper objectMapper) {
        this.testingEquipmentRepository = testingEquipmentRepository;
        this.userRepository = userRepository;
        this.quotationTestingEquipmentRepository = quotationTestingEquipmentRepository;
        this.quotationEditHistoryRepository = quotationEditHistoryRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public QuotationPageResponse findQuotations(int requestedPage, String query, YearMonth month) {
        String search = query == null ? "" : query.trim();
        PageRequest pageable = PageRequest.of(requestedPage - 1, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"));
        LocalDate startDate = month == null ? null : month.atDay(1);
        LocalDate endDate = month == null ? null : month.atEndOfMonth();
        Page<TestingEquipment> result;
        if (month != null) {
            result = search.isBlank()
                    ? testingEquipmentRepository.findByDateBetween(startDate, endDate, pageable)
                    : testingEquipmentRepository.searchByDateBetween(search, startDate, endDate, pageable);
        } else {
            result = search.isBlank()
                    ? testingEquipmentRepository.findAll(pageable)
                    : testingEquipmentRepository
                            .findByInvoiceNoContainingIgnoreCaseOrAttentionContainingIgnoreCaseOrClientNameContainingIgnoreCaseOrIsCodeContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(
                                    search, search, search, search, search, pageable);
        }

        Set<Long> creatorIds = result.getContent().stream().map(TestingEquipment::getCreatedBy)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> creators = userRepository.findAllById(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<TestingEquipment> summaryRecords = month == null ? testingEquipmentRepository.findAll()
                : testingEquipmentRepository.findByDateBetween(startDate, endDate);
        QuotationSummary summary = new QuotationSummary(summaryRecords.size(),
                summaryRecords.stream().filter(equipment -> equipment.getQuotationStatus() == null || equipment.getQuotationStatus() == 0).count(),
                summaryRecords.stream().filter(equipment -> Integer.valueOf(1).equals(equipment.getQuotationStatus())).count(),
                summaryRecords.stream().filter(equipment -> Integer.valueOf(2).equals(equipment.getQuotationStatus())).count());

        return new QuotationPageResponse(result.getContent().stream()
                .map(equipment -> toResponse(equipment, creators)).toList(),
                result.getNumber() + 1, result.getSize(), result.getTotalPages(), result.getTotalElements(),
                result.hasPrevious(), result.hasNext(), summary);
    }

    private QuotationResponse toResponse(TestingEquipment equipment, Map<Long, User> creators) {
        User creator = equipment.getCreatedBy() == null ? null : creators.get(equipment.getCreatedBy());
        return new QuotationResponse(equipment.getId() == null ? null : equipment.getId().longValue(),
                creator == null ? null : creator.getName(),
                equipment.getInvoiceNo(), equipment.getDate(), equipment.getAttention(), equipment.getClientName(),
                equipment.getIsCode(), equipment.getCompanyName());
    }

    @Transactional(readOnly = true)
    public QuotationResponse findQuotation(long id) {
        TestingEquipment equipment = equipment(id);
        User creator = equipment.getCreatedBy() == null ? null : userRepository.findById(equipment.getCreatedBy()).orElse(null);
        return toResponse(equipment, creator == null || equipment.getCreatedBy() == null ? Map.of() : Map.of(equipment.getCreatedBy(), creator));
    }

    @Transactional
    public QuotationResponse updateQuotation(long id, QuotationEditRequest request, String username) {
        if (request == null || request.date() == null) throw new IllegalArgumentException("Quotation date is required");
        TestingEquipment equipment = equipment(id);
        equipment.setDate(request.date()); equipment.setAttention(clean(request.attention())); equipment.setClientName(clean(request.clientName())); equipment.setCompanyName(clean(request.companyName())); equipment.setIsCode(clean(request.isCode()));
        if (request.items() != null) updateLineItems(equipment, request);
        else testingEquipmentRepository.save(equipment);
        quotationTestingEquipmentRepository.findByTestingEquipmentId(equipment.getId()).forEach(copy -> { copy.setDate(request.date()); copy.setAttention(clean(request.attention())); copy.setClientName(clean(request.clientName())); copy.setCompanyName(clean(request.companyName())); copy.setIsCode(clean(request.isCode())); });
        String editor = userRepository.findByEmail(username).map(User::getName).orElse(username == null ? "Unknown user" : username);
        quotationEditHistoryRepository.save(new QuotationEditHistory(equipment.getId(), editor, java.time.LocalDateTime.now()));
        return findQuotation(id);
    }

    private void updateLineItems(TestingEquipment anchor, QuotationEditRequest request) {
        if (request.items().isEmpty()) throw new IllegalArgumentException("Add at least one line item");
        if (request.items().size() > 100) throw new IllegalArgumentException("A quotation can contain at most 100 line items");
        List<TestingEquipment> records = anchor.getInvoiceNo() == null ? new java.util.ArrayList<>(List.of(anchor))
                : new java.util.ArrayList<>(testingEquipmentRepository.findByInvoiceNoOrderByIdAsc(anchor.getInvoiceNo()));
        records.remove(anchor); records.add(0, anchor);
        for (int index = 0; index < request.items().size(); index++) {
            QuotationLineItemRequest item = request.items().get(index);
            validateItem(item);
            TestingEquipment record = index < records.size() ? records.get(index) : new TestingEquipment();
            if (record.getId() == null) { record.setCreatedBy(anchor.getCreatedBy()); record.setInvoiceNo(anchor.getInvoiceNo()); record.setQuotationStatus(anchor.getQuotationStatus()); }
            applyHeader(record, request);
            record.setEquipmentName(clean(item.description())); record.setDescription(clean(item.description())); record.setHsnCode(clean(item.hsnCode())); record.setActualPrice(item.actualPrice());
            try { record.setDesQtyPrice(objectMapper.writeValueAsString(item)); } catch (Exception exception) { throw new IllegalArgumentException("Unable to prepare equipment details"); }
            testingEquipmentRepository.save(record);
            List<model.QuotationTestingEquipment> copies = quotationTestingEquipmentRepository.findByTestingEquipmentId(record.getId());
            model.QuotationTestingEquipment copy = copies.isEmpty() ? new model.QuotationTestingEquipment() : copies.get(0);
            copy.setTestingEquipmentId(record.getId()); copy.setInvoiceNo(record.getInvoiceNo()); copy.setDate(request.date()); copy.setAttention(clean(request.attention())); copy.setClientName(clean(request.clientName())); copy.setCompanyName(clean(request.companyName())); copy.setIsCode(clean(request.isCode())); copy.setDesQtyPrice(record.getDesQtyPrice());
            quotationTestingEquipmentRepository.save(copy);
        }
        for (int index = request.items().size(); index < records.size(); index++) {
            TestingEquipment obsolete = records.get(index);
            quotationEditHistoryRepository.deleteByTestingEquipmentId(obsolete.getId());
            quotationTestingEquipmentRepository.deleteByTestingEquipmentId(obsolete.getId());
            testingEquipmentRepository.delete(obsolete);
        }
    }
    private void applyHeader(TestingEquipment record, QuotationEditRequest request) { record.setDate(request.date()); record.setAttention(clean(request.attention())); record.setClientName(clean(request.clientName())); record.setCompanyName(clean(request.companyName())); record.setIsCode(clean(request.isCode())); }
    private void validateItem(QuotationLineItemRequest item) {
        if (item == null || clean(item.description()) == null) throw new IllegalArgumentException("Description is required");
        if (item.quantity() == null || item.quantity() < 1) throw new IllegalArgumentException("Quantity must be at least 1");
        if (item.actualPrice() == null || item.actualPrice().signum() < 0 || item.price() == null || item.price().signum() < 0) throw new IllegalArgumentException("Prices must be non-negative");
    }

    @Transactional
    public void deleteQuotation(long id) { TestingEquipment equipment = equipment(id); quotationEditHistoryRepository.deleteByTestingEquipmentId(equipment.getId()); quotationTestingEquipmentRepository.deleteByTestingEquipmentId(equipment.getId()); testingEquipmentRepository.delete(equipment); }

    @Transactional(readOnly = true)
    public List<QuotationEditHistory> history(long id) { return quotationEditHistoryRepository.findByTestingEquipmentIdOrderByEditedAtDesc(equipment(id).getId()); }
    private TestingEquipment equipment(long id) { return testingEquipmentRepository.findById(Math.toIntExact(id)).orElseThrow(() -> new IllegalArgumentException("Quotation not found")); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}