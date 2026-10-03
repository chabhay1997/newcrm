package controller;

import model.Lead;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeadsControllerLocationFilterTest {

    @Test
    void matchesStateByStateIdAndCityIdsFromThatState() {
        Lead lead = new Lead();
        lead.setStateId(null);
        lead.setCityId("44");

        Map<String, String> cityStateMap = Map.of("44", "12");

        assertTrue(LeadsController.matchesLeadLocationFilter(lead, "12", null, "Haryana", null, cityStateMap));
    }

    @Test
    void matchesLegacyLocationNamesStoredInIdColumns() {
        Lead lead = new Lead();
        lead.setStateId("Uttar Pradesh");
        lead.setCityId("Noida");

        assertTrue(LeadsController.matchesLeadLocationFilter(lead, "9", null, "Uttar Pradesh", null, Map.of()));
        assertTrue(LeadsController.matchesLeadLocationFilter(lead, null, "101", null, "Noida", Map.of()));
    }

    @Test
    void matchesCityByIdAndFallsBackToAddressTextWhenCityIdIsMissing() {
        Lead idLead = new Lead();
        idLead.setCityId("101");
        assertTrue(LeadsController.matchesLeadLocationFilter(idLead, null, "101", null, "Noida", Map.of()));

        Lead addressLead = new Lead();
        addressLead.setStateId(null);
        addressLead.setCityId(null);
        addressLead.setAddress("Lead from Noida, Uttar Pradesh");

        assertTrue(LeadsController.matchesLeadLocationFilter(addressLead, null, "101", null, "Noida", Map.of()));

        Lead noMatchLead = new Lead();
        noMatchLead.setStateId(null);
        noMatchLead.setCityId(null);
        noMatchLead.setAddress("Lead from Delhi");

        assertFalse(LeadsController.matchesLeadLocationFilter(noMatchLead, null, "101", null, "Noida", Map.of()));
    }

    @Test
    void matchesCityNameToItsStateForLegacyStateSearch() {
        Lead lead = new Lead();
        lead.setStateId(null);
        lead.setCityId("Noida");

        assertTrue(LeadsController.matchesLeadLocationFilter(lead, "9", null, "Uttar Pradesh", null,
                Map.of("name:noida", "9")));
    }
}