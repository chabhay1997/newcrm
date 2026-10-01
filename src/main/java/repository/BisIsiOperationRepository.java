package repository;
import model.BisIsiOperation; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.time.LocalDateTime; import java.util.*;
public interface BisIsiOperationRepository extends JpaRepository<BisIsiOperation,Long>, JpaSpecificationExecutor<BisIsiOperation> {
 boolean existsByCompanyNameIgnoreCaseAndIndianStandardIgnoreCase(String company,String standard); long countByAssignedEngineerId(Long id); List<BisIsiOperation> findAllByIdIn(Collection<Long> ids);
 @Modifying(clearAutomatically=true,flushAutomatically=true)
 @Query("update BisIsiOperation o set o.assignedEngineerId=:engineerId, o.updatedAt=:updatedAt where o.id in :ids")
 int assignSelected(@Param("ids") Collection<Long> ids,@Param("engineerId") Long engineerId,@Param("updatedAt") LocalDateTime updatedAt);
 @Modifying(clearAutomatically=true,flushAutomatically=true)
 @Query("update BisIsiOperation o set o.projectStatus=:status, o.updatedAt=:updatedAt where o.id=:id")
 int updateProjectStatus(@Param("id") Long id,@Param("status") String status,@Param("updatedAt") LocalDateTime updatedAt);
 @Modifying(clearAutomatically=true,flushAutomatically=true)
 @Query("update BisIsiOperation o set o.advancePaymentStatus=:status, o.updatedAt=:updatedAt where o.id=:id")
 int updateAdvancePaymentStatus(@Param("id") Long id,@Param("status") String status,@Param("updatedAt") LocalDateTime updatedAt);
 @Modifying(clearAutomatically=true,flushAutomatically=true)
 @Query("update BisIsiOperation o set o.paymentStatus=:status, o.paymentRemark=:remark, o.paymentData=:paymentData, o.updatedAt=:updatedAt where o.id=:id")
 int updatePaymentDetails(@Param("id") Long id,@Param("status") String status,@Param("remark") String remark,@Param("paymentData") String paymentData,@Param("updatedAt") LocalDateTime updatedAt);
}
