package service;

import dto.ChallanCreateRequest;
import model.Challan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import repository.ChallanRepository;
import repository.ChallanEditHistoryRepository;
import repository.UserRepository;
import repository.StateRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallanServiceTest {

    @Mock private ChallanRepository repository;
    @Mock private UserRepository userRepository;
    @Mock private ChallanFileStorageService fileStorageService;
    @Mock private StateRepository stateRepository;
    @Mock private ChallanEditHistoryRepository editHistoryRepository;

    @Test
    void returnsAStableTwentyFiveRecordPageMappedToDtos() {
        List<Challan> records = java.util.stream.IntStream.rangeClosed(1, 25)
                .mapToObj(this::challan).toList();
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(records, PageRequest.of(0, 25), 26));

        when(userRepository.findAllById(any())).thenReturn(List.of());
        var response = service().findChallans(1, "  ");

        assertThat(response.records()).hasSize(25);
        assertThat(response.pageSize()).isEqualTo(25);
        assertThat(response.currentPage()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.totalRecords()).isEqualTo(26);
        assertThat(response.hasPrevious()).isFalse();
        assertThat(response.hasNext()).isTrue();
        assertThat(response.records().getFirst().challanNo()).isEqualTo("CH-1");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(25);
        assertThat(pageable.getValue().getSort().getOrderFor("id").isAscending()).isTrue();
    }

    @Test
    void trimsSearchAndUsesAllThreeSearchableColumns() {
        PageRequest expectedPage = PageRequest.of(1, 25, org.springframework.data.domain.Sort.by("id").ascending());
        when(repository.findByChallanNoContainingIgnoreCaseOrClientNameContainingIgnoreCaseOrItemNameContainingIgnoreCase(
                "needle", "needle", "needle", expectedPage))
                .thenReturn(new PageImpl<>(List.of(challan(26)), expectedPage, 26));

        when(userRepository.findAllById(any())).thenReturn(List.of());
        var response = service().findChallans(2, "  needle  ");

        assertThat(response.records()).singleElement().satisfies(record -> {
            assertThat(record.id()).isEqualTo(26L);
            assertThat(record.amount()).isEqualTo("126.00");
        });
        assertThat(response.currentPage()).isEqualTo(2);
    }

    @Test
    void deletesAnExistingChallanById() {
        when(repository.existsById(42L)).thenReturn(true);

        service().deleteChallan(42L);

        verify(repository).deleteById(42L);
    }

    @Test
    void doesNotDeleteWhenChallanDoesNotExist() {
        when(repository.existsById(42L)).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service().deleteChallan(42L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("404");
        verify(repository, never()).deleteById(42L);
    }

    @Test
    void createsAChallanWithServerCalculatedTotalAndGeneratedNumber() {
        ChallanCreateRequest request = new ChallanCreateRequest();
        request.setClientName("  Test Client  ");
        request.setItemName("Test Item");
        request.setAmount("1000");
        request.setGst("18");
        request.setDate(LocalDate.of(2026, 9, 11));
        when(userRepository.findByEmail("creator@evtl.in")).thenReturn(java.util.Optional.empty());
        when(fileStorageService.store(any())).thenReturn(List.of());
        when(repository.saveAndFlush(any(Challan.class))).thenAnswer(invocation -> {
            Challan challan = invocation.getArgument(0);
            challan.setId(322L);
            return challan;
        });
        when(repository.save(any(Challan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Challan saved = service().createChallan(request, List.of(), "creator@evtl.in");

        assertThat(saved.getChallanNo()).isEqualTo("Evtl/DC/2026-27/322");
        assertThat(saved.getClientName()).isEqualTo("Test Client");
        assertThat(saved.getAmount()).isEqualTo("1000.00");
        assertThat(saved.getTotalAmount()).isEqualTo("1180.00");
        verify(repository).saveAndFlush(saved);
        verify(repository).save(saved);
    }

    private ChallanService service() {
        return new ChallanService(repository, userRepository, fileStorageService, stateRepository, editHistoryRepository);
    }

    private Challan challan(int number) {
        Challan challan = new Challan();
        challan.setId((long) number);
        challan.setChallanNo("CH-" + number);
        challan.setClientName("Client " + number);
        challan.setItemName("Item " + number);
        challan.setBrandName("Brand");
        challan.setQty("1");
        challan.setAmount((100L + number) + ".00");
        challan.setDate(LocalDate.of(2026, 9, 11));
        challan.setCreatedBy(null);
        return challan;
    }
}
