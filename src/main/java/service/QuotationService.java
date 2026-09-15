package service;

import dto.QuotationPageResponse;
import dto.QuotationResponse;
import model.TestingEquipment;
import model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.TestingEquipmentRepository;
import repository.UserRepository;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuotationService {
    public static final int PAGE_SIZE = 25;

    private final TestingEquipmentRepository testingEquipmentRepository;
    private final UserRepository userRepository;

    public QuotationService(TestingEquipmentRepository testingEquipmentRepository, UserRepository userRepository) {
        this.testingEquipmentRepository = testingEquipmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public QuotationPageResponse findQuotations(int requestedPage, String query) {
        String search = query == null ? "" : query.trim();
        PageRequest pageable = PageRequest.of(requestedPage - 1, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"));
        Page<TestingEquipment> result = search.isBlank()
                ? testingEquipmentRepository.findAll(pageable)
                : testingEquipmentRepository
                        .findByInvoiceNoContainingIgnoreCaseOrAttentionContainingIgnoreCaseOrClientNameContainingIgnoreCaseOrIsCodeContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(
                                search, search, search, search, search, pageable);

        Set<Long> creatorIds = result.getContent().stream().map(TestingEquipment::getCreatedBy)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> creators = userRepository.findAllById(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return new QuotationPageResponse(result.getContent().stream()
                .map(equipment -> toResponse(equipment, creators)).toList(),
                result.getNumber() + 1, result.getSize(), result.getTotalPages(), result.getTotalElements(),
                result.hasPrevious(), result.hasNext());
    }

    private QuotationResponse toResponse(TestingEquipment equipment, Map<Long, User> creators) {
        User creator = equipment.getCreatedBy() == null ? null : creators.get(equipment.getCreatedBy());
        return new QuotationResponse(equipment.getId() == null ? null : equipment.getId().longValue(),
                creator == null ? null : creator.getName(),
                equipment.getInvoiceNo(), equipment.getDate(), equipment.getAttention(), equipment.getClientName(),
                equipment.getIsCode(), equipment.getCompanyName());
    }
}
