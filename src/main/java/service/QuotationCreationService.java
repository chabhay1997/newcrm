package service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dto.QuotationCreateRequest;
import dto.QuotationCreateResponse;
import dto.QuotationLineItemRequest;
import model.QuotationTestingEquipment;
import model.TestingEquipment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import repository.QuotationTestingEquipmentRepository;
import repository.TestingEquipmentRepository;
import repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class QuotationCreationService {
    private static final int MAX_ITEMS = 100;
    private final TestingEquipmentRepository testingEquipmentRepository;
    private final QuotationTestingEquipmentRepository quotationTestingEquipmentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public QuotationCreationService(TestingEquipmentRepository testingEquipmentRepository,
                                    QuotationTestingEquipmentRepository quotationTestingEquipmentRepository,
                                    UserRepository userRepository, ObjectMapper objectMapper) {
        this.testingEquipmentRepository = testingEquipmentRepository;
        this.quotationTestingEquipmentRepository = quotationTestingEquipmentRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public QuotationCreateResponse create(QuotationCreateRequest request, String username) {
        validate(request);
        String invoiceNo = invoiceNumber(request.invoiceNo());
        if (quotationTestingEquipmentRepository.existsByInvoiceNo(invoiceNo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice number already exists");
        }
        Long createdBy = userRepository.findByEmail(username).map(user -> user.getId()).orElse(null);

        try {
            for (QuotationLineItemRequest item : request.items()) {
                TestingEquipment equipment = new TestingEquipment();
                equipment.setCreatedBy(createdBy);
                equipment.setInvoiceNo(invoiceNo);
                equipment.setDate(request.date());
                equipment.setAttention(clean(request.attention()));
                equipment.setClientName(clean(request.clientName()));
                equipment.setCompanyName(clean(request.companyName()));
                equipment.setIsCode(clean(request.isCode()));
                equipment.setEquipmentName(clean(item.description()));
                equipment.setHsnCode(clean(item.hsnCode()));
                equipment.setActualPrice(item.actualPrice().setScale(2, RoundingMode.HALF_UP));
                equipment.setDescription(clean(item.description()));
                String lineData = objectMapper.writeValueAsString(item);
                equipment.setDesQtyPrice(lineData);
                equipment = testingEquipmentRepository.saveAndFlush(equipment);

                QuotationTestingEquipment quotation = new QuotationTestingEquipment();
                quotation.setTestingEquipmentId(equipment.getId());
                quotation.setInvoiceNo(invoiceNo);
                quotation.setDate(request.date());
                quotation.setAttention(clean(request.attention()));
                quotation.setClientName(clean(request.clientName()));
                quotation.setCompanyName(clean(request.companyName()));
                quotation.setIsCode(clean(request.isCode()));
                quotation.setDesQtyPrice(lineData);
                quotationTestingEquipmentRepository.save(quotation);
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to prepare quotation data", exception);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Quotation could not be saved");
        }
        return new QuotationCreateResponse(invoiceNo, request.items().size(), "Equipment quotation created successfully");
    }

    private void validate(QuotationCreateRequest request) {
        if (request == null) throw new IllegalArgumentException("Quotation data is required");
        if (request.date() == null) throw new IllegalArgumentException("Date is required");
        required(request.attention(), "Attention", 255);
        required(request.clientName(), "Client Name", 255);
        required(request.companyName(), "Company Name", 225);
        required(request.isCode(), "IS Code", 50);
        if (request.items() == null || request.items().isEmpty()) throw new IllegalArgumentException("Add at least one line item");
        if (request.items().size() > MAX_ITEMS) throw new IllegalArgumentException("A quotation can contain at most 100 line items");
        request.items().forEach(this::validateItem);
    }

    private void validateItem(QuotationLineItemRequest item) {
        if (item == null) throw new IllegalArgumentException("Invalid line item");
        required(item.description(), "Description", 225);
        maxLength(item.hsnCode(), "HSN Code", 50);
        if (item.quantity() == null || item.quantity() < 1 || item.quantity() > 1_000_000)
            throw new IllegalArgumentException("Quantity must be between 1 and 1000000");
        money(item.actualPrice(), "Actual Price");
        money(item.price(), "Price");
    }

    private void money(BigDecimal value, String name) {
        if (value == null || value.signum() < 0 || value.scale() > 2 || value.compareTo(new BigDecimal("99999999.99")) > 0)
            throw new IllegalArgumentException(name + " must be a valid non-negative amount");
    }

    private String invoiceNumber(String value) {
        String invoiceNo = clean(value);
        if (invoiceNo == null || "EVTL/TEST-EQ".equals(invoiceNo)) {
            return "EVTL/TEST-EQ/" + UUID.randomUUID();
        }
        maxLength(invoiceNo, "Invoice No", 50);
        return invoiceNo;
    }

    private void required(String value, String name, int maxLength) {
        if (clean(value) == null) throw new IllegalArgumentException(name + " is required");
        maxLength(value, name, maxLength);
    }

    private void maxLength(String value, String name, int maxLength) {
        if (value != null && value.trim().length() > maxLength)
            throw new IllegalArgumentException(name + " must not exceed " + maxLength + " characters");
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
