package controller;

import com.evtl.crm.EvtlCrmApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(classes = EvtlCrmApplication.class)
@AutoConfigureMockMvc
class EmployeeApplicantControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void indexSuppliesEveryApplicantToTheTemplate() throws Exception {
        mockMvc.perform(get("/emp-app-form/index"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee/applicants/index"))
                .andExpect(model().attribute("totalApplicants", greaterThan(1)))
                .andExpect(model().attribute("applicants", hasSize(greaterThan(1))));
    }
}
