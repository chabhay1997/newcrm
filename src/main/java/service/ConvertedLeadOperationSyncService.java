package service;

import model.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import repository.*;
import java.time.LocalDate;

@Service
public class ConvertedLeadOperationSyncService {
    private final LeadRepository leads; private final UserRepository users; private final BisIsiService operations;
    public ConvertedLeadOperationSyncService(LeadRepository leads,UserRepository users,BisIsiService operations){this.leads=leads;this.users=users;this.operations=operations;}

    @Scheduled(fixedDelayString="${operation.lead-sync.delay-ms:30000}")
    public synchronized void sync(){
        User fallback=users.findAll().stream().filter(u->u.getRoleId()!=null&&u.getRoleId()==1).findFirst().orElse(null);
        if(fallback==null)return;
        for(Lead lead:leads.findConvertedActive()){
            String company=value(lead.getCompanyName(),"Lead #"+lead.getId()),standard=value(lead.getIsNumber(),"LEAD-"+lead.getId());
            if(operations.operationExists(company,standard))continue;
            User creator=lead.getCreatedBy()==null?fallback:users.findById(lead.getCreatedBy()).orElse(fallback);
            BisIsiOperation operation=new BisIsiOperation();operation.setOperationDate(lead.getConvertedDate()!=null?lead.getConvertedDate():(lead.getLeadDate()!=null?lead.getLeadDate():LocalDate.now()));operation.setCompanyName(company);operation.setClientName(value(lead.getIsName(),"N/A"));operation.setIndianStandard(standard);operation.setContactNumber(value(lead.getPhone(),value(lead.getCompanyMobile(),"N/A")));operation.setEmail(value(lead.getOfficialMailId(),value(lead.getEmail(),"N/A")));operation.setAddress(value(lead.getAddress(),"N/A"));operation.setStateId(stateId(lead.getStateId()));operation.setProjectStatus("Registration");operation.setProcedure("Normal");operation.setPaymentStatus("1st Installment");operation.setAdvancePaymentStatus("Pending");operation.setTestingStatus("Pending");operation.setTestingPerson("N/A");operation.setRemarks(marker(lead)+(lead.getRemarks()==null?"":" | "+lead.getRemarks()));
            try{operations.save(creator,operation,null);}catch(IllegalArgumentException ignored){}
        }
    }
    private String marker(Lead lead){return "Converted from Lead #"+lead.getId();}
    private String value(String value,String fallback){return value==null||value.isBlank()?fallback:value.trim();}
    private Long stateId(String value){try{return value==null||value.isBlank()?0L:Long.parseLong(value.trim());}catch(NumberFormatException ignored){return 0L;}}
}
