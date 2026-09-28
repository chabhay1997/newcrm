package service;

import dto.AssistantOperationSummary;
import dto.BisIsiAssistantRequest;
import model.BisIsiOperation;
import model.User;
import org.junit.jupiter.api.Test;
import repository.BisIsiOperationRepository;
import repository.BisPreInspectionRepository;
import repository.IsiChecklistRepository;
import repository.ProjectStatusRepository;
import repository.UserRepository;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BisIsiAssistantSummaryTest {

    @Test
    void calculatesAuthoritativeFactsFromEveryMatchingOperation() {
        BisIsiOperationRepository operations = mock(
                BisIsiOperationRepository.class
        );
        BisPreInspectionRepository preInspections = mock(
                BisPreInspectionRepository.class
        );
        IsiChecklistRepository checklists = mock(
                IsiChecklistRepository.class
        );
        UserRepository users = mock(UserRepository.class);
        ProjectStatusRepository projectStatuses = mock(
                ProjectStatusRepository.class
        );
        OperationAccessService access = new OperationAccessService(users);
        BisIsiService service = new BisIsiService(
                operations,
                preInspections,
                checklists,
                users,
                access,
                projectStatuses
        );

        int year = Year.now().getValue();
        LocalDate today = LocalDate.now();

        BisIsiOperation overdue = operation(
                LocalDate.of(year, 1, 10),
                "Simplified",
                "Drafting",
                10L,
                20L
        );
        overdue.setTargetDate(today.minusDays(46));

        BisIsiOperation dueSoon = operation(
                LocalDate.of(year, 2, 5),
                "Normal",
                "License Granted",
                10L,
                null
        );
        dueSoon.setFinalDate(today.plusDays(7));

        BisIsiOperation missingValues = operation(
                LocalDate.of(year, 2, 20),
                null,
                null,
                null,
                20L
        );

        BisIsiOperation anotherYear = operation(
                LocalDate.of(year - 1, 1, 1),
                "Normal",
                "On Hold",
                10L,
                20L
        );

        User dev = user(10L, "Dev");
        User siya = user(20L, "Siya");

        when(operations.findAll()).thenReturn(List.of(
                overdue,
                dueSoon,
                missingValues,
                anotherYear
        ));
        when(projectStatuses.findAll()).thenReturn(List.of());
        when(users.findAllById(any())).thenReturn(List.of(dev, siya));

        User superAdmin = user(1L, "SuperAdmin");
        superAdmin.setRoleId(1);

        AssistantOperationSummary summary = service.assistantSummary(
                superAdmin,
                new BisIsiAssistantRequest(
                        "summary",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        year,
                        2,
                        "Drafting",
                        List.of()
                )
        );

        assertThat(summary.total()).isEqualTo(3);
        assertThat(summary.monthlyCounts())
                .containsEntry("January", 1L)
                .containsEntry("February", 2L)
                .containsEntry("March", 0L);
        assertThat(summary.procedureCounts())
                .containsEntry("Simplified", 1L)
                .containsEntry("Normal", 1L)
                .containsEntry("Not set", 1L);
        assertThat(summary.statusCounts())
                .containsEntry("Drafting", 1L)
                .containsEntry("License Granted", 1L)
                .containsEntry("Status not set", 1L);
        assertThat(summary.processCounts())
                .isEqualTo(summary.statusCounts());
        assertThat(summary.createdByCounts())
                .containsEntry("Dev", 2L)
                .containsEntry("Not set", 1L);
        assertThat(summary.assignedToCounts())
                .containsEntry("Siya", 2L)
                .containsEntry("Not set", 1L);
        assertThat(summary.overdueCount()).isEqualTo(1);
        assertThat(summary.dueSoonCount()).isEqualTo(1);
        assertThat(summary.overdueRecords()).hasSize(1);
        assertThat(summary.dueSoonRecords()).hasSize(1);
        assertThat(summary.records()).hasSize(3);
        assertThat(summary.operationsTeamMembers()).containsExactly(
                "Dev",
                "Prashansha",
                "Vartika",
                "Siya",
                "Vaishnavi",
                "Smriti",
                "Divyanshu"
        );
        assertThat(summary.operationsTeamMemberCount()).isEqualTo(7);
        assertThat(summary.matchingRecordCount()).isEqualTo(3);
        assertThat(summary.includedRecordCount()).isEqualTo(3);
        assertThat(summary.recordsTruncated()).isFalse();
    }

    @Test
    void limitsSafeRecordsButKeepsTheCompleteMatchingCount() {
        BisIsiOperationRepository operations = mock(
                BisIsiOperationRepository.class
        );
        BisPreInspectionRepository preInspections = mock(
                BisPreInspectionRepository.class
        );
        IsiChecklistRepository checklists = mock(
                IsiChecklistRepository.class
        );
        UserRepository users = mock(UserRepository.class);
        ProjectStatusRepository projectStatuses = mock(
                ProjectStatusRepository.class
        );
        OperationAccessService access = new OperationAccessService(users);
        BisIsiService service = new BisIsiService(
                operations,
                preInspections,
                checklists,
                users,
                access,
                projectStatuses
        );

        int year = Year.now().getValue();
        List<BisIsiOperation> matchingOperations = java.util.stream.IntStream
                .range(0, 55)
                .mapToObj(index -> operation(
                        LocalDate.of(year, 1, 1).plusDays(index),
                        "Simplified",
                        "Drafting",
                        null,
                        null
                ))
                .toList();

        when(operations.findAll()).thenReturn(matchingOperations);
        when(projectStatuses.findAll()).thenReturn(List.of());
        when(users.findAllById(any())).thenReturn(List.of());

        User superAdmin = user(1L, "SuperAdmin");
        superAdmin.setRoleId(1);

        AssistantOperationSummary summary = service.assistantSummary(
                superAdmin,
                new BisIsiAssistantRequest(
                        "list companies",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        year,
                        1,
                        "Drafting",
                        List.of()
                )
        );

        assertThat(summary.total()).isEqualTo(55);
        assertThat(summary.processCounts())
                .containsEntry("Drafting", 55L);
        assertThat(summary.matchingRecordCount()).isEqualTo(55);
        assertThat(summary.includedRecordCount()).isEqualTo(50);
        assertThat(summary.records()).hasSize(50);
        assertThat(summary.recordsTruncated()).isTrue();
        assertThat(summary.recordLimit()).isEqualTo(50);
    }

    @Test
    void safeRecordsContainOnlyOperationsAuthorizedByAnalyticsOperations() {
        BisIsiOperationRepository operations = mock(
                BisIsiOperationRepository.class
        );
        BisPreInspectionRepository preInspections = mock(
                BisPreInspectionRepository.class
        );
        IsiChecklistRepository checklists = mock(
                IsiChecklistRepository.class
        );
        UserRepository users = mock(UserRepository.class);
        ProjectStatusRepository projectStatuses = mock(
                ProjectStatusRepository.class
        );
        OperationAccessService access = new OperationAccessService(users);
        BisIsiService service = new BisIsiService(
                operations,
                preInspections,
                checklists,
                users,
                access,
                projectStatuses
        );

        int year = Year.now().getValue();
        BisIsiOperation authorized = operation(
                LocalDate.of(year, 3, 1),
                "Normal",
                "Drafting",
                10L,
                20L
        );
        authorized.setCompanyName("Authorized Company");

        BisIsiOperation anotherUsersOperation = operation(
                LocalDate.of(year, 3, 2),
                "Normal",
                "Drafting",
                99L,
                20L
        );
        anotherUsersOperation.setCompanyName("Private Company");

        BisIsiOperation hiddenStatus = operation(
                LocalDate.of(year, 3, 3),
                "Normal",
                "License Granted",
                10L,
                20L
        );
        hiddenStatus.setCompanyName("Hidden Status Company");

        User dev = user(10L, "Dev");
        dev.setRoleId(2);

        when(operations.findAll()).thenReturn(List.of(
                authorized,
                anotherUsersOperation,
                hiddenStatus
        ));
        when(projectStatuses.findAll()).thenReturn(List.of());
        when(users.findAllById(any())).thenReturn(List.of(
                dev,
                user(20L, "Siya")
        ));

        AssistantOperationSummary summary = service.assistantSummary(
                dev,
                new BisIsiAssistantRequest(
                        "list companies",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        year,
                        3,
                        "Drafting",
                        List.of()
                )
        );

        assertThat(summary.matchingRecordCount()).isEqualTo(1);
        assertThat(summary.records())
                .extracting(record -> record.companyName())
                .containsExactly("Authorized Company");
    }

    private BisIsiOperation operation(
            LocalDate operationDate,
            String procedure,
            String projectStatus,
            Long createdBy,
            Long assignedTo
    ) {
        BisIsiOperation operation = new BisIsiOperation();
        operation.setOperationDate(operationDate);
        operation.setCompanyName("Example Company");
        operation.setIndianStandard("IS 1234");
        operation.setProcedure(procedure);
        operation.setProjectStatus(projectStatus);
        operation.setCreatedBy(createdBy);
        operation.setAssignedEngineerId(assignedTo);
        return operation;
    }

    private User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }
}
