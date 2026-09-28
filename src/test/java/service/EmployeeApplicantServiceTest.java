package service;

import model.EmployeeApplicant;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import repository.EmployeeApplicantRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class EmployeeApplicantServiceTest {

    @Test
    void findPageAllPreservesRequestedPageAndReturnsFullList() {
        EmployeeApplicantRepository repository = mock(EmployeeApplicantRepository.class);
        EmployeeApplicant applicant1 = new EmployeeApplicant();
        applicant1.setId(1L);
        applicant1.setName("Alice");

        EmployeeApplicant applicant2 = new EmployeeApplicant();
        applicant2.setId(2L);
        applicant2.setName("Bob");

        EmployeeApplicant applicant3 = new EmployeeApplicant();
        applicant3.setId(3L);
        applicant3.setName("Charlie");

        when(repository.findAllForListing()).thenReturn(List.of(applicant1, applicant2, applicant3));

        EmployeeApplicantService service = new EmployeeApplicantService(repository);

        Page<EmployeeApplicant> page = service.findPage("", "all", 2);

        assertNotNull(page);
        assertEquals(3, page.getTotalElements());
        assertEquals(3, page.getContent().size());
        assertEquals(0, page.getNumber());
        assertEquals(3, page.getSize());
    }

    @Test
    void findPageWithBlankOrNullSizeReturnsFullList() {
        EmployeeApplicantRepository repository = mock(EmployeeApplicantRepository.class);
        EmployeeApplicant applicant1 = new EmployeeApplicant();
        applicant1.setId(1L);
        applicant1.setName("Alice");

        EmployeeApplicant applicant2 = new EmployeeApplicant();
        applicant2.setId(2L);
        applicant2.setName("Bob");

        when(repository.findAllForListing()).thenReturn(List.of(applicant1, applicant2));

        EmployeeApplicantService service = new EmployeeApplicantService(repository);

        Page<EmployeeApplicant> page = service.findPage("", null, 0);

        assertNotNull(page);
        assertEquals(2, page.getTotalElements());
        assertEquals(2, page.getContent().size());
        assertEquals(0, page.getNumber());
    }

    @Test
    void findPageWithSearchUsesPagedRepositoryQuery() {
        EmployeeApplicantRepository repository = mock(EmployeeApplicantRepository.class);
        EmployeeApplicant applicant = new EmployeeApplicant();
        applicant.setId(10L);
        applicant.setName("Ravi");

        when(repository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContainingIgnoreCaseOrderByCreatedAtDesc(
                "ravi", "ravi", "ravi", Pageable.ofSize(25))).thenReturn(org.springframework.data.support.PageableExecutionUtils.getPage(
                List.of(applicant), Pageable.ofSize(25), () -> 1L));

        EmployeeApplicantService service = new EmployeeApplicantService(repository);
        Page<EmployeeApplicant> page = service.findPage("ravi", "25", 0);

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals(1, page.getContent().size());
    }

    @Test
    void findPageUsesLastAvailablePageWhenTheRequestedPageNoLongerExists() {
        EmployeeApplicantRepository repository = mock(EmployeeApplicantRepository.class);
        EmployeeApplicant applicant = new EmployeeApplicant();
        applicant.setId(10L);

        when(repository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(), PageRequest.of(99, 25), 26L))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(applicant), PageRequest.of(1, 25), 26L));

        Page<EmployeeApplicant> page = new EmployeeApplicantService(repository).findPage("", "25", 99);

        assertEquals(1, page.getNumber());
        assertEquals(26, page.getTotalElements());
        assertEquals(1, page.getContent().size());
    }
}
