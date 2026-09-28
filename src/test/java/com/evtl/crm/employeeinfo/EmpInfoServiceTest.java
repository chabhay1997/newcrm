package com.evtl.crm.employeeinfo;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmpInfoServiceTest {

    @Test
    void findAllReturnsRowsOrderedByJoinDateDescending() {
        EmpInfoRepository repository = mock(EmpInfoRepository.class);

        EmpInfo first = new EmpInfo();
        first.setId(1L);
        first.setName("Alice");
        first.setJoinDate(LocalDate.of(2024, 4, 10));

        EmpInfo second = new EmpInfo();
        second.setId(2L);
        second.setName("Bob");
        second.setJoinDate(LocalDate.of(2024, 5, 15));

        when(repository.findAllByOrderByJoinDateDesc()).thenReturn(List.of(second, first));

        EmpInfoService service = new EmpInfoService(repository);
        List<EmpInfo> employees = service.findAll();

        assertNotNull(employees);
        assertEquals(2, employees.size());
        assertEquals("Bob", employees.get(0).getName());
        assertEquals("Alice", employees.get(1).getName());
    }
}
