package service;
import model.User; import org.springframework.http.HttpStatus; import org.springframework.security.core.Authentication; import org.springframework.stereotype.Service; import org.springframework.web.server.ResponseStatusException; import repository.UserRepository; import java.util.*;
@Service public class OperationAccessService {
 public static final Set<String> OPERATIONS_TEAM=Set.of("dev","prashansha","siya","smriti","vaishnavi","vartika");
 public static final Set<String> OPERATIONS_TEAM_HIDDEN_STATUSES=Set.of("Application Under Process","Inspection Completed","License Granted");
 private final UserRepository users; public OperationAccessService(UserRepository users){this.users=users;}
 public User require(Authentication auth){ if(auth==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED); User u=users.findByEmail(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN)); if(!isAdmin(u) && (u.getPermissions()==null || !u.getPermissions().toLowerCase().contains("operation"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Operation permission is required"); return u; }
 public boolean isAdmin(User u){return u.getRoleId()!=null&&u.getRoleId()==1;}
 public boolean isOperationsTeam(User u){return u!=null&&u.getName()!=null&&OPERATIONS_TEAM.contains(u.getName().trim().toLowerCase(Locale.ROOT));}
 public boolean isApplicationProcessor(User u){return u!=null&&u.getName()!=null&&"divyanshu".equals(u.getName().trim().toLowerCase(Locale.ROOT));}
 public boolean isHiddenFromOperationsTeam(model.BisIsiOperation o){return o.getProjectStatus()!=null&&OPERATIONS_TEAM_HIDDEN_STATUSES.contains(o.getProjectStatus());}
 public boolean canView(User u,model.BisIsiOperation o){if(isAdmin(u))return true;if(isApplicationProcessor(u))return "Application Under Process".equals(o.getProjectStatus());if(isOperationsTeam(u))return u.getId().equals(o.getCreatedBy())&&!isHiddenFromOperationsTeam(o);return u.getId().equals(o.getCreatedBy())||u.getId().equals(o.getAssignedEngineerId());}
 public boolean canEdit(User u,model.BisIsiOperation o){return canView(u,o);}
 public User requireAdmin(Authentication authentication) { User user = require(authentication); if(!isAdmin(user)){throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Super Admin access Only!!");}return user;}
}
