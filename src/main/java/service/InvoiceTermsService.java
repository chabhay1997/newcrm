package service;

import model.InvoiceTermsCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.InvoiceTermsConditionRepository;

import java.util.List;

@Service
public class InvoiceTermsService {

    @Autowired
    private InvoiceTermsConditionRepository termsRepository;

    public List<InvoiceTermsCondition> getTermsByType(Integer invType) {
        return termsRepository.findByInvTypeOrderByIdAsc(invType);
    }

    /** Mirrors termsConditions(): update-in-place by index, create extras, delete leftovers. */
    public void saveTerms(Integer invType, List<String> submittedTerms) {
        List<InvoiceTermsCondition> existing = termsRepository.findByInvTypeOrderByIdAsc(invType);

        for (int i = 0; i < submittedTerms.size(); i++) {
            String term = submittedTerms.get(i);
            if (term == null || term.trim().isEmpty()) continue;

            if (i < existing.size()) {
                InvoiceTermsCondition row = existing.get(i);
                row.setTermsCondtions(term);
                termsRepository.save(row);
            } else {
                InvoiceTermsCondition row = new InvoiceTermsCondition();
                row.setInvType(invType);
                row.setTermsCondtions(term);
                termsRepository.save(row);
            }
        }

        if (submittedTerms.size() < existing.size()) {
            for (int i = submittedTerms.size(); i < existing.size(); i++) {
                termsRepository.delete(existing.get(i));
            }
        }
    }
}