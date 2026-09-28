package service;
import model.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.domain.Specification; import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.server.ResponseStatusException; import repository.*; import jakarta.persistence.criteria.Predicate; import java.time.*; import java.util.*; import dto.AssistantOperationFact; import dto.AssistantOperationSummary; import dto.BisIsiAssistantRequest;
@Service public class BisIsiService {
 public record Dashboard(long total,Map<String,Long> statusCounts,long licenceGranted,long inspectionCompleted){}
 public record MonthlyAnalytics(int year,List<MonthlyAnalyticsPoint> months,long maximum){}
 public record MonthlyAnalyticsPoint(String label,long total,long licenceGranted,List<ProcessBreakdown> breakdown){}
 public record ProcessBreakdown(String label,long count){}
 public record StatusCard(String label,String status,long count){}
 private record StatusCardType(String label,String status){}
 private static final List<StatusCardType> STATUS_CARD_TYPES=List.of(
  new StatusCardType("Registration","Registration"),new StatusCardType("PreTesting","Testing Under Process"),new StatusCardType("Testing Done","Testing Completed"),
  new StatusCardType("Drafting","Drafting"),new StatusCardType("Application under process","Application Under Process"),new StatusCardType("Query Raised","Query Raised"),
  new StatusCardType("Closure Notice Issued","Closure Notice Issued"),new StatusCardType("Reply Done","Reply Submitted"),new StatusCardType("Inspection date allotted","Inspection Date Alloted"),
  new StatusCardType("Inspection Done","Inspection Completed"),new StatusCardType("License Granted","License Granted"),new StatusCardType("On Hold","On Hold"),
  new StatusCardType("Qci Inspection","Qci Inspection"),new StatusCardType("TRF Generated","TRF Generated"),new StatusCardType("Other Service","Other Service"));
 public static final List<String> STATUSES=Arrays.asList("Application Under Process","Closure Notice Issued","Drafting","Inspection Completed","Inspection Date Alloted","License Granted","On Hold","Other Service","Qci Inspection","Query Raised","Registration","Reply Submitted","Testing Completed","Testing Under Process","TRF Generated");
 public static final List<String> PAYMENT_STATUSES=List.of("1st Installment","2nd Installment","3rd Installment","4th Installment");
 private static final int ASSISTANT_RECORD_LIMIT=50;
 private static final int ASSISTANT_DUE_SOON_DAYS=7;
 private static final List<String> OPERATIONS_TEAM_MEMBERS=List.of("Dev","Prashansha","Vartika","Siya","Vaishnavi","Smriti","Divyanshu");
 public static final Set<String> ASSIGNEE_NAMES=Set.of("SuperAdmin","Naveen","Arti Rani","Anjali Pandey","Afiya Khan","Varun Sir","Prerna Pandey","Sretee Pandey","Dev","Tejal Maurya","Tanya","Divyanshu","Vartika","Prashansha","Vaishnavi","Prince","FMCS","Swati","Kajal Kumari","Smriti","Gaurav Sharma","Sunil","Siya","Operation");
 private final BisIsiOperationRepository operations; private final BisPreInspectionRepository pre; private final IsiChecklistRepository checklists; private final UserRepository users; private final OperationAccessService access; private final ProjectStatusRepository projectStatuses;
 public BisIsiService(BisIsiOperationRepository operations,BisPreInspectionRepository pre,IsiChecklistRepository checklists,UserRepository users,OperationAccessService access,ProjectStatusRepository projectStatuses){this.operations=operations;this.pre=pre;this.checklists=checklists;this.users=users;this.access=access;this.projectStatuses=projectStatuses;}
 public Page<BisIsiOperation> list(User user,LocalDate startDate,LocalDate endDate,String procedure,Boolean extras,Long engineer,Long creator,String status,String search,int page){return list(user,startDate,endDate,procedure,extras,engineer,creator,status,search,null,page);}
 public Page<BisIsiOperation> list(User user,LocalDate startDate,LocalDate endDate,String procedure,Boolean extras,Long engineer,Long creator,String status,String search,List<String> processes,int page){Set<String> legacyGrantedIds=legacyGrantedIds();Specification<BisIsiOperation> spec=(root,q,cb)->{List<Predicate> p=new ArrayList<>();if(access.isApplicationProcessor(user))p.add(cb.equal(root.get("projectStatus"),"Application Under Process"));else if(access.isOperationsTeam(user)){p.add(cb.equal(root.get("createdBy"),user.getId()));p.add(cb.or(cb.isNull(root.get("projectStatus")),cb.not(root.get("projectStatus").in(OperationAccessService.OPERATIONS_TEAM_HIDDEN_STATUSES))));}else if(!access.isAdmin(user))p.add(cb.or(cb.equal(root.get("createdBy"),user.getId()),cb.equal(root.get("assignedEngineerId"),user.getId())));if(processes==null||processes.isEmpty())p.add(unlicensedPredicate(root,cb,legacyGrantedIds));if(startDate!=null)p.add(cb.greaterThanOrEqualTo(root.get("operationDate"),startDate));if(endDate!=null)p.add(cb.lessThanOrEqualTo(root.get("operationDate"),endDate));if(startDate!=null&&endDate!=null&&startDate.isAfter(endDate))p.add(cb.disjunction());if(procedure!=null&&!procedure.isBlank())p.add(cb.equal(root.get("procedure"),procedure));if(engineer!=null)p.add(cb.equal(root.get("assignedEngineerId"),engineer));if(creator!=null)p.add(cb.equal(root.get("createdBy"),creator));if(status!=null&&!status.isBlank())p.add(cb.equal(cb.lower(root.get("extraService")),status.toLowerCase(Locale.ROOT)));if(processes!=null&&!processes.isEmpty())p.add(root.get("projectStatus").in(processes));if(search!=null&&!search.isBlank()){String x="%"+search.toLowerCase()+"%";p.add(cb.or(cb.like(cb.lower(root.get("companyName")),x),cb.like(cb.lower(root.get("clientName")),x),cb.like(cb.lower(root.get("indianStandard")),x)));}return cb.and(p.toArray(Predicate[]::new));};return operations.findAll(spec,PageRequest.of(Math.max(0,page),25,Sort.by(Sort.Direction.DESC,"id")));}
 public Page<BisIsiOperation> listAmc(User user,int page){return listAmc(user,null,25,page);}
 public Page<BisIsiOperation> listAmc(User user,String search,int size,int page){return operations.findAll(amcSpec(user,search),PageRequest.of(Math.max(0,page),Math.max(10,Math.min(size,100)),Sort.by(Sort.Direction.DESC,"id")));}
 public List<BisIsiOperation> allAmc(User user,String search){return operations.findAll(amcSpec(user,search),Sort.by(Sort.Direction.DESC,"id"));}
 private Specification<BisIsiOperation> amcSpec(User user,String search){Set<String> legacyGrantedIds=legacyGrantedIds();return (root,q,cb)->{List<Predicate> p=new ArrayList<>();p.add(licensedPredicate(root,cb,legacyGrantedIds));if(!access.isAdmin(user)){if(access.isApplicationProcessor(user)||access.isOperationsTeam(user))p.add(cb.disjunction());else p.add(cb.or(cb.equal(root.get("createdBy"),user.getId()),cb.equal(root.get("assignedEngineerId"),user.getId())));}if(search!=null&&!search.isBlank()){String term="%"+search.trim().toLowerCase(Locale.ROOT)+"%";p.add(cb.or(cb.like(cb.lower(root.get("companyName")),term),cb.like(cb.lower(root.get("clientName")),term),cb.like(cb.lower(root.get("indianStandard")),term),cb.like(cb.lower(root.get("cmlNumber")),term)));}return cb.and(p.toArray(Predicate[]::new));};}
 private Set<String> legacyGrantedIds(){Set<String> ids=new HashSet<>();projectStatuses.findAll().stream().filter(status->"License Granted".equalsIgnoreCase(status.getProjectStatus())).forEach(status->ids.add(String.valueOf(status.getId())));return ids;}
 private Predicate licensedPredicate(jakarta.persistence.criteria.Root<BisIsiOperation> root,jakarta.persistence.criteria.CriteriaBuilder cb,Set<String> legacyIds){Predicate current=cb.equal(root.get("projectStatus"),"License Granted");if(legacyIds.isEmpty())return current;return cb.or(current,cb.and(cb.or(cb.isNull(root.get("projectStatus")),cb.equal(root.get("projectStatus"),"")),root.get("legacyProjectId").in(legacyIds)));}
 private Predicate unlicensedPredicate(jakarta.persistence.criteria.Root<BisIsiOperation> root,jakarta.persistence.criteria.CriteriaBuilder cb,Set<String> legacyIds){Predicate noText=cb.or(cb.isNull(root.get("projectStatus")),cb.equal(root.get("projectStatus"),""));Predicate otherText=cb.and(cb.isNotNull(root.get("projectStatus")),cb.notEqual(root.get("projectStatus"),""),cb.notEqual(root.get("projectStatus"),"License Granted"));if(legacyIds.isEmpty())return cb.or(otherText,noText);return cb.or(otherText,cb.and(noText,cb.or(cb.isNull(root.get("legacyProjectId")),cb.not(root.get("legacyProjectId").in(legacyIds)))));}
 public Dashboard dashboard(User user){return dashboard(user,null,null,null,null);}
 /**
  * Analytics deliberately use the same ownership fields exposed by the BIS-ISI
  * filters.  This lets an administrator see a date-bounded breakdown for either
  * the person an operation is assigned to or the person who created it.
  */
 public Dashboard dashboard(User user,LocalDate startDate,LocalDate endDate,Long engineer,Long creator){
  return dashboard(user,startDate,endDate,engineer,creator,null);
 }
 public Dashboard dashboard(User user,LocalDate startDate,LocalDate endDate,Long engineer,Long creator,List<String> processes){
  List<BisIsiOperation> visible=analyticsOperations(user,startDate,endDate,engineer,creator,processes);
  Map<String,Long> counts=statusCounts(visible);
  return new Dashboard(visible.size(),counts,counts.getOrDefault("License Granted",0L),counts.getOrDefault("Inspection Completed",0L));
 }
 public MonthlyAnalytics monthlyAnalytics(User user,int year,LocalDate startDate,LocalDate endDate,Long engineer,Long creator){
  return monthlyAnalytics(user,year,startDate,endDate,engineer,creator,null);
 }
 public MonthlyAnalytics monthlyAnalytics(User user,int year,LocalDate startDate,LocalDate endDate,Long engineer,Long creator,List<String> processes){
  return monthlyAnalytics(user,year,startDate,endDate,engineer,creator,processes,"License Granted");
 }
 public MonthlyAnalytics monthlyAnalytics(User user,int year,LocalDate startDate,LocalDate endDate,Long engineer,Long creator,List<String> processes,String selectedProcess){
  List<BisIsiOperation> visible=analyticsOperations(user,startDate,endDate,engineer,creator,processes);
  Map<String,String> legacyStatuses=legacyStatusMap();
  List<MonthlyAnalyticsPoint> months=new ArrayList<>();
  for(Month month:Month.values()){
   List<BisIsiOperation> inMonth=visible.stream().filter(operation->operation.getOperationDate()!=null&&operation.getOperationDate().getYear()==year&&operation.getOperationDate().getMonth()==month).toList();
   Map<String,Long> processCounts=new LinkedHashMap<>();
   for(BisIsiOperation operation:inMonth){String status=resolvedStatus(operation,legacyStatuses);String label=STATUS_CARD_TYPES.stream().filter(type->type.status().equalsIgnoreCase(status)).map(StatusCardType::label).findFirst().orElse(status);processCounts.merge(label,1L,Long::sum);}
   List<ProcessBreakdown> breakdown=processCounts.entrySet().stream().map(entry->new ProcessBreakdown(entry.getKey(),entry.getValue())).toList();
   long selectedProcessCount=inMonth.stream().filter(operation->selectedProcess.equalsIgnoreCase(resolvedStatus(operation,legacyStatuses))).count();
   months.add(new MonthlyAnalyticsPoint(month.getDisplayName(java.time.format.TextStyle.FULL,Locale.ENGLISH),inMonth.size(),selectedProcessCount,breakdown));
  }
  long maximum=months.stream().mapToLong(point->Math.max(point.total(),point.licenceGranted())).max().orElse(0L);
  return new MonthlyAnalytics(year,months,maximum);
 }
 public List<Integer> analyticsYears(User user,Long engineer,Long creator){
  TreeSet<Integer> years=analyticsOperations(user,null,null,engineer,creator).stream().map(BisIsiOperation::getOperationDate).filter(Objects::nonNull).map(LocalDate::getYear).collect(java.util.stream.Collectors.toCollection(TreeSet::new));
  years.add(Year.now().getValue());
  return years.descendingSet().stream().toList();
 }
 public Map<Long,String> resolvedStatuses(Collection<BisIsiOperation> source){
  Map<String,String> legacyStatuses=legacyStatusMap();Map<Long,String> result=new HashMap<>();
  for(BisIsiOperation operation:source)result.put(operation.getId(),resolvedStatus(operation,legacyStatuses));
  return result;
 }
 public List<StatusCard> statusCards(User user){return statusCards(user,null,null,null,null);}
 public List<StatusCard> statusCards(User user,LocalDate startDate,LocalDate endDate,Long engineer,Long creator){
  List<BisIsiOperation> visible=analyticsOperations(user,startDate,endDate,engineer,creator);
  Map<String,String> legacyStatuses=new HashMap<>();projectStatuses.findAll().forEach(project->legacyStatuses.put(String.valueOf(project.getId()),project.getProjectStatus()));
  Map<String,Long> counts=new HashMap<>();
  for(BisIsiOperation operation:visible){
   String status=operation.getProjectStatus();if(status==null||status.isBlank())status=legacyStatuses.get(operation.getLegacyProjectId());
   if(status==null||status.isBlank()||!canViewCardStatus(user,operation,status))continue;
   counts.merge(status.trim().toLowerCase(Locale.ROOT),1L,Long::sum);
  }
  return STATUS_CARD_TYPES.stream().map(type->new StatusCard(type.label(),type.status(),counts.getOrDefault(type.status().toLowerCase(Locale.ROOT),0L))).toList();
 }
 private List<BisIsiOperation> analyticsOperations(User user,LocalDate startDate,LocalDate endDate,Long engineer,Long creator){
  return analyticsOperations(user,startDate,endDate,engineer,creator,null);
 }
 private List<BisIsiOperation> analyticsOperations(User user,LocalDate startDate,LocalDate endDate,Long engineer,Long creator,List<String> processes){
  if(startDate!=null&&endDate!=null&&startDate.isAfter(endDate))return List.of();
  Map<String,String> legacyStatuses=legacyStatusMap();
  return operations.findAll().stream()
   .filter(operation->canViewCardStatus(user,operation,resolvedStatus(operation,legacyStatuses)))
   .filter(operation->startDate==null||operation.getOperationDate()!=null&&!operation.getOperationDate().isBefore(startDate))
   .filter(operation->endDate==null||operation.getOperationDate()!=null&&!operation.getOperationDate().isAfter(endDate))
   .filter(operation->engineer==null||engineer.equals(operation.getAssignedEngineerId()))
   .filter(operation->creator==null||creator.equals(operation.getCreatedBy()))
   .filter(operation->processes==null||processes.isEmpty()||processes.stream().anyMatch(process->process.equalsIgnoreCase(resolvedStatus(operation,legacyStatuses))))
   .toList();
 }
 private Map<String,String> legacyStatusMap(){Map<String,String> statuses=new HashMap<>();projectStatuses.findAll().forEach(project->statuses.put(String.valueOf(project.getId()),project.getProjectStatus()));return statuses;}
 private String resolvedStatus(BisIsiOperation operation,Map<String,String> legacyStatuses){String status=operation.getProjectStatus();if(status==null||status.isBlank())status=legacyStatuses.get(operation.getLegacyProjectId());return status==null||status.isBlank()?"Status not set":status.trim();}
 private Map<String,Long> statusCounts(List<BisIsiOperation> operationsForAnalytics){
  Map<String,String> legacyStatuses=new HashMap<>();projectStatuses.findAll().forEach(project->legacyStatuses.put(String.valueOf(project.getId()),project.getProjectStatus()));
  Map<String,Long> counts=new LinkedHashMap<>();
  for(String status:STATUSES)counts.put(status,0L);
  for(BisIsiOperation operation:operationsForAnalytics){
   String status=operation.getProjectStatus();if(status==null||status.isBlank())status=legacyStatuses.get(operation.getLegacyProjectId());
   if(status!=null&&counts.containsKey(status))counts.compute(status,(key,value)->value+1);
  }
  return counts;
 }
 private boolean canViewCardStatus(User user,BisIsiOperation operation,String status){
  if(access.isAdmin(user))return true;
  if(access.isApplicationProcessor(user))return "Application Under Process".equalsIgnoreCase(status);
  if(access.isOperationsTeam(user))return user.getId().equals(operation.getCreatedBy())&&OperationAccessService.OPERATIONS_TEAM_HIDDEN_STATUSES.stream().noneMatch(hidden->hidden.equalsIgnoreCase(status));
  return user.getId().equals(operation.getCreatedBy())||user.getId().equals(operation.getAssignedEngineerId());
 }
 public BisIsiOperation get(User u,long id){BisIsiOperation o=operations.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));if(!access.canView(u,o))throw new ResponseStatusException(HttpStatus.FORBIDDEN);return o;}
 @Transactional public BisIsiOperation save(User u,BisIsiOperation form,Long id){required(form); BisIsiOperation o=id==null?new BisIsiOperation():get(u,id); if(id!=null&&!access.canEdit(u,o))throw new ResponseStatusException(HttpStatus.FORBIDDEN); if((id==null||!o.getCompanyName().equalsIgnoreCase(form.getCompanyName())||!o.getIndianStandard().equalsIgnoreCase(form.getIndianStandard()))&&operations.existsByCompanyNameIgnoreCaseAndIndianStandardIgnoreCase(form.getCompanyName().trim(),form.getIndianStandard().trim()))throw new IllegalArgumentException("This company already has an operation for the same IS number"); Long existingEngineer=o.getAssignedEngineerId();copy(form,o);if(!access.isAdmin(u))o.setAssignedEngineerId(existingEngineer);if(id==null)o.setCreatedBy(u.getId());calculateDates(o);return operations.save(o); }
 @Transactional public void updateStatus(User u,long id,String status){BisIsiOperation o=get(u,id);if(!access.canEdit(u,o))throw new ResponseStatusException(HttpStatus.FORBIDDEN);if(!STATUSES.contains(status))throw new IllegalArgumentException("Invalid project status");if(operations.updateProjectStatus(id,status,LocalDateTime.now())!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Operation not found");}
 @Transactional public void updatePayments(User u,long id,String payment,String advance){BisIsiOperation o=get(u,id);if(!access.canEdit(u,o))throw new ResponseStatusException(HttpStatus.FORBIDDEN);if(!PAYMENT_STATUSES.contains(payment))throw new IllegalArgumentException("Invalid payment status");o.setPaymentStatus(payment);o.setAdvancePaymentStatus(advance);operations.save(o);}
 @Transactional public void updateAdvancePayment(User u,long id,String advance){BisIsiOperation o=get(u,id);if(!access.canEdit(u,o))throw new ResponseStatusException(HttpStatus.FORBIDDEN);if(!List.of("Pending","Paid","Not Applicable").contains(advance))throw new IllegalArgumentException("Invalid advance payment status");if(operations.updateAdvancePaymentStatus(id,advance,LocalDateTime.now())!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Operation not found");}
 @Transactional public void updatePaymentDetails(User u,long id,String payment,String remark){BisIsiOperation o=get(u,id);if(!access.canEdit(u,o))throw new ResponseStatusException(HttpStatus.FORBIDDEN);if(!PAYMENT_STATUSES.contains(payment))throw new IllegalArgumentException("Invalid payment option");String cleanRemark=remark==null||remark.isBlank()?null:remark.trim();if(operations.updatePaymentDetails(id,payment,cleanRemark,LocalDateTime.now())!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Operation not found");}
 @Transactional public void bulkAssign(User u,List<Long> ids,Long engineer){if(!access.isAdmin(u))throw new ResponseStatusException(HttpStatus.FORBIDDEN);if(ids==null||ids.isEmpty())throw new IllegalArgumentException("Select at least one operation");users.findById(engineer).orElseThrow(()->new IllegalArgumentException("Engineer not found"));int assigned=operations.assignSelected(new LinkedHashSet<>(ids),engineer,LocalDateTime.now());if(assigned==0)throw new IllegalArgumentException("No matching operations were found");}
 @Transactional public void delete(User u,long id){if(!access.isAdmin(u))throw new ResponseStatusException(HttpStatus.FORBIDDEN);pre.deleteByOperationId(id);checklists.deleteByOperationId(id);operations.deleteById(id);}
 @Transactional public int bulkDelete(User u,List<Long> ids){
  if(!access.isAdmin(u))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  if(ids==null||ids.isEmpty()||ids.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("Select at least one operation to delete.");
  Set<Long> selected=new LinkedHashSet<>(ids);
  if(operations.findAllById(selected).size()!=selected.size())throw new IllegalArgumentException("One or more selected operations no longer exist. Nothing was deleted.");
  for(Long id:selected){pre.deleteByOperationId(id);checklists.deleteByOperationId(id);operations.deleteById(id);}
  return selected.size();
 }
 public List<User> engineers(){return users.findAll().stream().filter(x->x.getStatus()==null||x.getStatus()!=0).filter(x->x.getRoleId()==null||x.getRoleId()!=1).filter(x->{String p=x.getPermissions()==null?"":x.getPermissions().toLowerCase();return p.contains("operation")||p.contains("checklist")||p.contains("engineer");}).toList();} public List<User> creators(){return users.findAll();}
 public List<User> assignmentUsers(){return users.findAll().stream().filter(x->x.getStatus()==null||x.getStatus()!=0).filter(x->x.getName()!=null&&ASSIGNEE_NAMES.stream().anyMatch(name->name.equalsIgnoreCase(x.getName().trim()))).sorted(Comparator.comparing(User::getName,String.CASE_INSENSITIVE_ORDER)).toList();}
 public boolean operationExists(String company,String standard){return operations.existsByCompanyNameIgnoreCaseAndIndianStandardIgnoreCase(company.trim(),standard.trim());}
 private void calculateDates(BisIsiOperation o){if(o.getTargetDate()==null)o.setTargetDate(LocalDate.now().plusDays(10));if(o.getFinalDate()==null)o.setFinalDate(o.getTargetDate().plusDays("Simplified".equals(o.getProcedure())?45:70));}
 private void required(BisIsiOperation f){if(f.getOperationDate()==null||blank(f.getCompanyName())||blank(f.getClientName())||blank(f.getIndianStandard())||blank(f.getContactNumber())||blank(f.getEmail())||blank(f.getProjectStatus())||blank(f.getPaymentStatus())||blank(f.getTestingStatus())||blank(f.getTestingPerson())||f.getStateId()==null)throw new IllegalArgumentException("Date, company, client, Indian Standard, contact number, client email, project status, payment status, testing status, testing person and state are required");if(!STATUSES.contains(f.getProjectStatus()))throw new IllegalArgumentException("Invalid project status");if(!PAYMENT_STATUSES.contains(f.getPaymentStatus()))throw new IllegalArgumentException("Invalid payment status");if(!List.of("Paid","Pending").contains(f.getTestingStatus()))throw new IllegalArgumentException("Invalid testing status");if(!List.of("Simplified","Normal").contains(f.getProcedure()))throw new IllegalArgumentException("Procedure must be Simplified or Normal");}
 private boolean blank(String x){return x==null||x.isBlank();} private void copy(BisIsiOperation f,BisIsiOperation o){o.setOperationDate(f.getOperationDate());o.setCompanyName(f.getCompanyName().trim());o.setClientName(f.getClientName().trim());o.setIndianStandard(f.getIndianStandard().trim());o.setContactNumber(f.getContactNumber().trim());o.setAlternateContactNumber(f.getAlternateContactNumber());o.setEmail(f.getEmail());o.setAddress(f.getAddress());o.setStateId(f.getStateId());o.setPortalUsername(f.getPortalUsername());o.setPortalPassword(f.getPortalPassword());o.setEvtlEmail(f.getEvtlEmail());o.setEvtlEmailPasscode(f.getEvtlEmailPasscode());o.setProjectStatus(f.getProjectStatus());o.setProcedure(f.getProcedure());o.setPaymentStatus(f.getPaymentStatus());o.setAdvancePaymentStatus(f.getAdvancePaymentStatus());o.setPaymentInstallments(f.getPaymentInstallments());o.setTotalAmount(f.getTotalAmount());o.setTestingDeadline(f.getTestingDeadline());o.setTestingStatus(f.getTestingStatus());o.setTestingPerson(f.getTestingPerson());o.setOperatingPerson(f.getOperatingPerson());o.setApplicationDeadline(f.getApplicationDeadline());o.setLicenceDeadline(f.getLicenceDeadline());o.setTargetDate(f.getTargetDate());o.setTargetFinalExceptionDate(f.getTargetFinalExceptionDate());o.setFinalDate(f.getFinalDate());o.setCmlNumber(f.getCmlNumber());o.setLicenceNumber(f.getLicenceNumber());o.setLicenceDate(f.getLicenceDate());o.setAssignedEngineerId(f.getAssignedEngineerId());o.setAssignedEngineerName(f.getAssignedEngineerName());o.setRemarks(f.getRemarks());o.setExtraService(f.getExtraService());o.setInclusionEnabled(f.isInclusionEnabled());o.setRenewalEnabled(f.isRenewalEnabled());o.setBrandInclusionEnabled(f.isBrandInclusionEnabled());o.setThirdPartyVisitEnabled(f.isThirdPartyVisitEnabled());o.setSitEnabled(f.isSitEnabled());o.setSitStatus(f.getSitStatus());o.setSitQuarter(f.getSitQuarter());o.setExtraServiceNotes(f.getExtraServiceNotes());}
public AssistantOperationSummary assistantSummary(
        User currentUser,
        BisIsiAssistantRequest request
) {
    int analyticsYear = request.analyticsYear() != null
            ? request.analyticsYear()
            : Year.now().getValue();

    Integer selectedMonth = request.selectedMonth();

    if (selectedMonth == null
            || selectedMonth < 1
            || selectedMonth > 12) {
        selectedMonth = LocalDate.now().getMonthValue();
    }

    String analyticsProcess = request.analyticsProcess() != null
            && STATUSES.contains(request.analyticsProcess())
            ? request.analyticsProcess()
            : "License Granted";

    List<String> selectedProcesses = request.processes() == null
            ? List.of()
            : request.processes()
                    .stream()
                    .filter(STATUSES::contains)
                    .distinct()
                    .toList();

    /*
     * analyticsOperations already applies:
     * - logged-in user access restrictions
     * - start and end date
     * - Assigned By
     * - Filter By User
     * - selected process tiles
     */
    List<BisIsiOperation> authorizedOperations = analyticsOperations(
            currentUser,
            request.startDate(),
            request.endDate(),
            request.engineer(),
            request.creator(),
            selectedProcesses
    );

    // Apply the remaining page filters.
    authorizedOperations = authorizedOperations.stream()
            .filter(operation ->
                    request.procedure() == null
                    || request.procedure().isBlank()
                    || request.procedure()
                            .equalsIgnoreCase(operation.getProcedure())
            )
            .filter(operation ->
                    request.status() == null
                    || request.status().isBlank()
                    || (
                        operation.getExtraService() != null
                        && request.status().equalsIgnoreCase(
                                operation.getExtraService()
                        )
                    )
            )
            .toList();

    Map<String, String> legacyStatuses = legacyStatusMap();

    /*
     * The monthly chart is year-specific, so restrict the chatbot
     * summary to the selected analytics year as well.
     */
    List<BisIsiOperation> yearOperations = authorizedOperations.stream()
            .filter(operation ->
                    operation.getOperationDate() != null
                    && operation.getOperationDate().getYear()
                            == analyticsYear
            )
            .toList();

    Map<String, Long> statusCounts = new LinkedHashMap<>();

    for (String status : STATUSES) {
        statusCounts.put(status, 0L);
    }
    statusCounts.put("Status not set", 0L);

    for (BisIsiOperation operation : yearOperations) {
        String resolvedStatus = resolvedStatus(
                operation,
                legacyStatuses
        );

        statusCounts.merge(
                resolvedStatus,
                1L,
                Long::sum
        );
    }

    Map<String, Long> monthlyCounts = new LinkedHashMap<>();

    for (Month month : Month.values()) {
        long count = yearOperations.stream()
                .filter(operation ->
                        operation.getOperationDate().getMonth() == month
                )
                .count();

        monthlyCounts.put(
                month.getDisplayName(
                        java.time.format.TextStyle.FULL,
                        Locale.ENGLISH
                ),
                count
        );
    }

    Map<Long, String> userNames = assistantUserNames(yearOperations);
    Map<String, Long> procedureCounts = initializedCounts(
            List.of("Simplified", "Normal", "Not set")
    );
    Map<String, Long> createdByCounts = initializedCounts(
            OPERATIONS_TEAM_MEMBERS
    );
    Map<String, Long> assignedToCounts = initializedCounts(
            OPERATIONS_TEAM_MEMBERS
    );

    for (BisIsiOperation operation : yearOperations) {
        incrementCount(
                procedureCounts,
                assistantText(operation.getProcedure())
        );
        incrementCount(
                createdByCounts,
                assistantUserName(userNames, operation.getCreatedBy())
        );
        incrementCount(
                assignedToCounts,
                assistantUserName(
                        userNames,
                        operation.getAssignedEngineerId()
                )
        );
    }

    Map<String, Long> processCounts = new LinkedHashMap<>(statusCounts);
    LocalDate asOfDate = LocalDate.now();
    LocalDate dueSoonThrough = asOfDate.plusDays(
            ASSISTANT_DUE_SOON_DAYS
    );

    long overdueCount = yearOperations.stream()
            .map(this::assistantFinalDate)
            .filter(Objects::nonNull)
            .filter(date -> date.isBefore(asOfDate))
            .count();

    long dueSoonCount = yearOperations.stream()
            .map(this::assistantFinalDate)
            .filter(Objects::nonNull)
            .filter(date -> !date.isBefore(asOfDate))
            .filter(date -> !date.isAfter(dueSoonThrough))
            .count();

    List<BisIsiOperation> newestFirst = yearOperations.stream()
            .sorted(assistantOperationOrder())
            .toList();

    List<AssistantOperationFact> safeRecords = newestFirst.stream()
            .limit(ASSISTANT_RECORD_LIMIT)
            .map(operation -> assistantOperationFact(
                    operation,
                    legacyStatuses,
                    userNames
            ))
            .toList();

    List<AssistantOperationFact> overdueRecords = newestFirst.stream()
            .filter(operation -> {
                LocalDate finalDate = assistantFinalDate(operation);
                return finalDate != null && finalDate.isBefore(asOfDate);
            })
            .limit(ASSISTANT_RECORD_LIMIT)
            .map(operation -> assistantOperationFact(
                    operation,
                    legacyStatuses,
                    userNames
            ))
            .toList();

    List<AssistantOperationFact> dueSoonRecords = newestFirst.stream()
            .filter(operation -> {
                LocalDate finalDate = assistantFinalDate(operation);
                return finalDate != null
                        && !finalDate.isBefore(asOfDate)
                        && !finalDate.isAfter(dueSoonThrough);
            })
            .limit(ASSISTANT_RECORD_LIMIT)
            .map(operation -> assistantOperationFact(
                    operation,
                    legacyStatuses,
                    userNames
            ))
            .toList();

    String selectedUser = resolveAssistantUserName(
            request.creator(),
            request.engineer()
    );

    return new AssistantOperationSummary(
            yearOperations.size(),
            statusCounts,
            monthlyCounts,
            procedureCounts,
            createdByCounts,
            assignedToCounts,
            processCounts,
            overdueCount,
            dueSoonCount,
            asOfDate,
            OPERATIONS_TEAM_MEMBERS,
            OPERATIONS_TEAM_MEMBERS.size(),
            safeRecords,
            overdueRecords,
            dueSoonRecords,
            yearOperations.size(),
            safeRecords.size(),
            yearOperations.size() > ASSISTANT_RECORD_LIMIT,
            ASSISTANT_RECORD_LIMIT,
            selectedUser,
            request.startDate(),
            request.endDate(),
            analyticsYear,
            selectedMonth,
            analyticsProcess,
            normalizedPageFilter(request.procedure()),
            resolveAssistantUserName(request.engineer()),
            resolveAssistantUserName(request.creator()),
            normalizedPageFilter(request.status()),
            selectedProcesses
    );
}
private Comparator<BisIsiOperation> assistantOperationOrder() {
    return Comparator.comparing(
            BisIsiOperation::getOperationDate,
            Comparator.nullsLast(Comparator.reverseOrder())
    ).thenComparing(
            BisIsiOperation::getId,
            Comparator.nullsLast(Comparator.reverseOrder())
    );
}
private AssistantOperationFact assistantOperationFact(
        BisIsiOperation operation,
        Map<String, String> legacyStatuses,
        Map<Long, String> userNames
) {
    return new AssistantOperationFact(
            operation.getId(),
            assistantText(operation.getCompanyName()),
            assistantText(operation.getIndianStandard()),
            operation.getOperationDate(),
            assistantText(operation.getProcedure()),
            resolvedStatus(operation, legacyStatuses),
            assistantUserName(userNames, operation.getCreatedBy()),
            assistantUserName(
                    userNames,
                    operation.getAssignedEngineerId()
            ),
            operation.getTargetDate(),
            assistantFinalDate(operation)
    );
}
private Map<Long, String> assistantUserNames(
        List<BisIsiOperation> source
) {
    Set<Long> userIds = new LinkedHashSet<>();

    for (BisIsiOperation operation : source) {
        if (operation.getCreatedBy() != null) {
            userIds.add(operation.getCreatedBy());
        }
        if (operation.getAssignedEngineerId() != null) {
            userIds.add(operation.getAssignedEngineerId());
        }
    }

    Map<Long, String> result = new HashMap<>();
    users.findAllById(userIds).forEach(user ->
            result.put(user.getId(), assistantText(user.getName()))
    );
    return result;
}
private Map<String, Long> initializedCounts(List<String> labels) {
    Map<String, Long> result = new LinkedHashMap<>();
    labels.forEach(label -> result.put(label, 0L));
    return result;
}
private void incrementCount(
        Map<String, Long> counts,
        String label
) {
    counts.merge(label, 1L, Long::sum);
}
private String assistantUserName(
        Map<Long, String> userNames,
        Long userId
) {
    return userId == null
            ? "Not set"
            : userNames.getOrDefault(userId, "Not set");
}
private String assistantText(String value) {
    return value == null || value.isBlank()
            ? "Not set"
            : value.trim();
}
private LocalDate assistantFinalDate(BisIsiOperation operation) {
    if (operation.getFinalDate() != null) {
        return operation.getFinalDate();
    }

    if (operation.getTargetDate() == null) {
        return null;
    }

    long additionalDays = "Simplified".equalsIgnoreCase(
            operation.getProcedure()
    ) ? 45L : 70L;

    return operation.getTargetDate().plusDays(additionalDays);
}
private String normalizedPageFilter(String value) {
    return value == null || value.isBlank() ? "All" : value.trim();
}
private String resolveAssistantUserName(Long userId) {
    if (userId == null) {
        return "All Users";
    }

    return users.findById(userId)
            .map(User::getName)
            .orElse("Unknown User");
}
private String resolveAssistantUserName(
        Long creatorId,
        Long engineerId
) {
    Long selectedUserId = creatorId != null
            ? creatorId
            : engineerId;

    return resolveAssistantUserName(selectedUserId);
}
}
