package service;

import dto.ChallanPageResponse;
import dto.ChallanResponse;
import dto.ChallanCreateRequest;
import dto.ChallanDetailsResponse;
import dto.ChallanReturnAlertResponse;
import model.Challan;
import model.ChallanEditHistory;
import model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import repository.ChallanRepository;
import repository.ChallanEditHistoryRepository;
import repository.UserRepository;
import repository.StateRepository;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ChallanService {
    public static final int PAGE_SIZE = 25;
    private final ChallanRepository challanRepository;
    private final UserRepository userRepository;
    private final ChallanFileStorageService fileStorageService;
    private final StateRepository stateRepository;
    private final ChallanEditHistoryRepository editHistoryRepository;

    public ChallanService(ChallanRepository challanRepository, UserRepository userRepository,
                          ChallanFileStorageService fileStorageService, StateRepository stateRepository,
                          ChallanEditHistoryRepository editHistoryRepository) {
        this.challanRepository = challanRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.stateRepository = stateRepository;
        this.editHistoryRepository = editHistoryRepository;
    }

    @Transactional(readOnly = true)
    public ChallanPageResponse findChallans(int requestedPage, String query) {
        return findChallans(requestedPage, query, null, null);
    }

    @Transactional(readOnly = true)
    public ChallanPageResponse findChallans(int requestedPage, String query, LocalDate startDate, LocalDate endDate) {
        int page = Math.max(requestedPage, 1);
        String search = normalizeSearch(query);
        PageRequest pageable = PageRequest.of(page - 1, PAGE_SIZE, Sort.by("id").ascending());
        Page<Challan> result;
        if (startDate == null && endDate == null) {
            result = search.isBlank()
                    ? challanRepository.findAll(pageable)
                    : challanRepository.findByChallanNoContainingIgnoreCaseOrClientNameContainingIgnoreCaseOrItemNameContainingIgnoreCase(
                            search, search, search, pageable);
        } else {
            result = challanRepository.findFiltered(search, startDate, endDate, pageable);
        }
        Set<Long> creatorIds = result.getContent().stream().map(Challan::getCreatedBy)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> creators = userRepository.findAllById(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return new ChallanPageResponse(result.getContent().stream()
                .map(challan -> toResponse(challan, creators)).toList(),
                result.getNumber() + 1, result.getSize(), result.getTotalPages(), result.getTotalElements(),
                result.hasPrevious(), result.hasNext());
    }

    @Transactional
    public void deleteChallan(long id) {
        if (!challanRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Challan not found");
        }
        editHistoryRepository.deleteByChallanId(id);
        challanRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long countChallans() {
        return challanRepository.count();
    }

    @Transactional(readOnly = true)
    public int pageContaining(long id) {
        long position = challanRepository.countByIdLessThanEqual(id);
        return Math.max(1, (int) Math.ceil(position / (double) PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public String nextChallanNumberPreview() {
        Long autoIncrement = challanRepository.findNextAutoIncrement();
        long nextId = autoIncrement == null ? 1 : autoIncrement;
        return challanNumber(nextId);
    }

    @Transactional(readOnly = true)
    public ChallanCreateRequest findForEdit(long id) {
        Challan challan = findChallan(id);
        ChallanCreateRequest form = new ChallanCreateRequest();
        form.setChallanNo(challan.getChallanNo());
        form.setClientName(challan.getClientName());
        form.setClientNumber(challan.getClientNumber());
        form.setItemName(challan.getItemName());
        form.setBrandName(challan.getBrandName());
        form.setStateId(challan.getStateId());
        form.setQty(challan.getQty());
        form.setAmount(challan.getAmount());
        form.setGst(challan.getGst());
        form.setTotalAmount(challan.getTotalAmount());
        form.setDate(challan.getDate());
        form.setSampleReturnDate(challan.getSampleReturnDate());
        form.setAddress(challan.getAddress());
        form.setPincode(challan.getPincode());
        form.setRemark(challan.getRemark());
        return form;
    }

    @Transactional(readOnly = true)
    public ChallanDetailsResponse findDetails(long id) {
        Challan challan = findChallan(id);
        User creator = challan.getCreatedBy() == null ? null : userRepository.findById(challan.getCreatedBy()).orElse(null);
        var state = challan.getStateId() == null ? null : stateRepository.findById(challan.getStateId()).orElse(null);
        return new ChallanDetailsResponse(challan.getId(), challan.getChallanNo(), challan.getClientName(),
                challan.getClientNumber(), challan.getItemName(), challan.getBrandName(),
                state == null ? null : state.getName(), challan.getQty(), challan.getAmount(), challan.getGst(),
                challan.getTotalAmount(), challan.getDate(), challan.getSampleReturnDate(), challan.getAddress(),
                challan.getPincode(), challan.getRemark(), creator == null ? null : creator.getName(),
                challan.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ChallanReturnAlertResponse> upcomingSampleReturns() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate alertThrough = today.plusDays(7);
        return challanRepository.findBySampleReturnDateBetweenOrderBySampleReturnDateAsc(today, alertThrough).stream()
                .map(challan -> new ChallanReturnAlertResponse(challan.getId(), challan.getChallanNo(),
                        challan.getClientName(), challan.getItemName(), challan.getSampleReturnDate(),
                        ChronoUnit.DAYS.between(today, challan.getSampleReturnDate())))
                .toList();
    }

    @Transactional
    public Challan updateChallan(long id, ChallanCreateRequest request, List<MultipartFile> uploads) {
        Challan challan = findChallan(id);
        validateCreateRequest(request);
        BigDecimal amount = decimal(request.getAmount(), "Amount");
        BigDecimal gst = decimal(request.getGst(), "GST");
        if (gst.compareTo(BigDecimal.valueOf(100)) > 0) throw new IllegalArgumentException("GST must not exceed 100%.");
        BigDecimal total = calculateTotal(amount, gst);
        List<String> storedFiles = fileStorageService.store(uploads);
        try {
            applyEditableFields(challan, request, amount, gst, total);
            if (!storedFiles.isEmpty()) {
                challan.setUpload(joinUploads(challan.getUpload(), storedFiles));
            }
            Challan saved = challanRepository.saveAndFlush(challan);
            recordEdit(saved.getId());
            return saved;
        } catch (RuntimeException exception) {
            fileStorageService.delete(storedFiles);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<ChallanEditHistory> editHistory(long challanId) {
        findChallan(challanId);
        return editHistoryRepository.findByChallanIdOrderByEditedAtDesc(challanId);
    }

    private void recordEdit(Long challanId) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String email = authentication == null ? null : authentication.getName();
        String editor = email == null ? "Unknown user" : userRepository.findByEmail(email)
                .map(User::getName).orElse(email);
        editHistoryRepository.save(new ChallanEditHistory(challanId, editor,
                LocalDateTime.now(ZoneId.of("Asia/Kolkata"))));
    }

    @Transactional
    public Challan createChallan(ChallanCreateRequest request, List<MultipartFile> uploads, String username) {
        validateCreateRequest(request);
        BigDecimal amount = decimal(request.getAmount(), "Amount");
        BigDecimal gst = decimal(request.getGst(), "GST");
        if (gst.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("GST must not exceed 100%.");
        }
        BigDecimal total = calculateTotal(amount, gst);
        Long creatorId = userRepository.findByEmail(username).map(User::getId).orElse(null);
        List<String> storedFiles = fileStorageService.store(uploads);
        try {
            Challan challan = new Challan();
            applyEditableFields(challan, request, amount, gst, total);
            challan.setUpload(storedFiles.isEmpty() ? null : String.join(",", storedFiles));
            challan.setCreatedBy(creatorId);
            challan.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
            challan = challanRepository.saveAndFlush(challan);
            challan.setChallanNo(challanNumber(challan.getId()));
            return challanRepository.save(challan);
        } catch (RuntimeException exception) {
            fileStorageService.delete(storedFiles);
            throw exception;
        }
    }

    private Challan findChallan(long id) {
        if (id < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challan ID must be positive");
        return challanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Challan not found"));
    }

    private BigDecimal calculateTotal(BigDecimal amount, BigDecimal gst) {
        BigDecimal total = amount.multiply(BigDecimal.ONE.add(gst.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
        if (total.toPlainString().length() > 21) throw new IllegalArgumentException("Total Amount is too large.");
        return total;
    }

    private void applyEditableFields(Challan challan, ChallanCreateRequest request, BigDecimal amount,
                                     BigDecimal gst, BigDecimal total) {
        challan.setClientName(clean(request.getClientName()));
        challan.setClientNumber(clean(request.getClientNumber()));
        challan.setItemName(clean(request.getItemName()));
        challan.setBrandName(clean(request.getBrandName()));
        challan.setStateId(request.getStateId());
        challan.setQty(clean(request.getQty()));
        challan.setAmount(amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        challan.setGst(gst.stripTrailingZeros().toPlainString());
        challan.setTotalAmount(total.toPlainString());
        challan.setDate(request.getDate());
        challan.setSampleReturnDate(request.getSampleReturnDate());
        challan.setAddress(clean(request.getAddress()));
        challan.setPincode(clean(request.getPincode()));
        challan.setRemark(clean(request.getRemark()));
    }

    private String joinUploads(String existing, List<String> added) {
        return blank(existing) ? String.join(",", added) : existing + "," + String.join(",", added);
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }

    private void validateCreateRequest(ChallanCreateRequest request) {
        required(request.getClientName(), "Client Name", 255);
        required(request.getItemName(), "Item Name", 255);
        if (request.getDate() == null) throw new IllegalArgumentException("Date is required.");
        maxLength(request.getClientNumber(), "Client Number", 15);
        maxLength(request.getBrandName(), "Brand Name", 255);
        maxLength(request.getQty(), "Quantity", 255);
        maxLength(request.getPincode(), "Pincode", 15);
        maxLength(request.getAddress(), "Address", 10_000);
        maxLength(request.getRemark(), "Remark", 10_000);
        if (request.getStateId() != null && !stateRepository.existsById(request.getStateId()))
            throw new IllegalArgumentException("Selected state is invalid.");
        if (request.getSampleReturnDate() != null && request.getSampleReturnDate().isBefore(request.getDate())) {
            throw new IllegalArgumentException("Sample Return Date cannot be before the Challan Date.");
        }
    }

    private BigDecimal decimal(String value, String field) {
        try {
            BigDecimal number = new BigDecimal(value == null || value.isBlank() ? "0" : value.trim());
            if (number.signum() < 0) throw new NumberFormatException();
            if (number.scale() > 2 || number.compareTo(new BigDecimal("999999999999999999")) > 0) throw new NumberFormatException();
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " must be a valid non-negative number.");
        }
    }

    private void required(String value, String field, int limit) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
        maxLength(value, field, limit);
    }

    private void maxLength(String value, String field, int limit) {
        if (value != null && value.trim().length() > limit) {
            throw new IllegalArgumentException(field + " must not exceed " + limit + " characters.");
        }
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private String challanNumber(long id) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        int startYear = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
        return "Evtl/DC/" + startYear + "-" + String.format("%02d", (startYear + 1) % 100) + "/" + id;
    }

    private String normalizeSearch(String query) {
        return query == null ? "" : query.trim();
    }

    private ChallanResponse toResponse(Challan challan, Map<Long, User> creators) {
        User creator = challan.getCreatedBy() == null ? null : creators.get(challan.getCreatedBy());
        return new ChallanResponse(challan.getId(), challan.getChallanNo(), challan.getClientName(),
                challan.getItemName(), challan.getBrandName(), challan.getQty(), challan.getAmount(),
                challan.getDate(), creator == null ? null : creator.getName(), challan.getCreatedAt());
    }
}
