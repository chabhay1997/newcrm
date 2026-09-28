package repository;

import model.BisCrsRenewal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BisCrsRenewalRepository extends JpaRepository<BisCrsRenewal, Long> {
    @Query("""
            select count(renewal) > 0 from BisCrsRenewal renewal
            where lower(trim(coalesce(renewal.licenceNumber, ''))) = lower(:licenceNumber)
              and lower(trim(coalesce(renewal.manufacturerName, ''))) = lower(:manufacturerName)
              and lower(trim(coalesce(renewal.productName, ''))) = lower(:productName)
              and lower(trim(coalesce(renewal.airName, ''))) = lower(:airName)
              and lower(trim(coalesce(renewal.brandName, ''))) = lower(:brandName)
              and lower(trim(coalesce(renewal.currentStatus, ''))) = lower(:currentStatus)
              and ((:licenceDate is null and renewal.licenceDate is null) or renewal.licenceDate = :licenceDate)
            """)
    boolean existsExactRecord(@Param("licenceNumber") String licenceNumber,
                              @Param("manufacturerName") String manufacturerName,
                              @Param("productName") String productName,
                              @Param("airName") String airName,
                              @Param("brandName") String brandName,
                              @Param("licenceDate") java.time.LocalDate licenceDate,
                              @Param("currentStatus") String currentStatus);

    @Query("""
            select count(renewal) > 0 from BisCrsRenewal renewal
            where renewal.id <> :id
              and lower(trim(coalesce(renewal.licenceNumber, ''))) = lower(:licenceNumber)
              and lower(trim(coalesce(renewal.manufacturerName, ''))) = lower(:manufacturerName)
              and lower(trim(coalesce(renewal.productName, ''))) = lower(:productName)
              and lower(trim(coalesce(renewal.airName, ''))) = lower(:airName)
              and lower(trim(coalesce(renewal.brandName, ''))) = lower(:brandName)
              and lower(trim(coalesce(renewal.currentStatus, ''))) = lower(:currentStatus)
              and ((:licenceDate is null and renewal.licenceDate is null) or renewal.licenceDate = :licenceDate)
            """)
    boolean existsExactRecordExcludingId(@Param("id") Long id,
                                         @Param("licenceNumber") String licenceNumber,
                                         @Param("manufacturerName") String manufacturerName,
                                         @Param("productName") String productName,
                                         @Param("airName") String airName,
                                         @Param("brandName") String brandName,
                                         @Param("licenceDate") java.time.LocalDate licenceDate,
                                         @Param("currentStatus") String currentStatus);

    @Query("""
            select renewal from BisCrsRenewal renewal
            where :search = ''
               or lower(coalesce(renewal.licenceNumber, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(renewal.manufacturerName, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(renewal.productName, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(renewal.currentStatus, '')) like lower(concat('%', :search, '%'))
            order by renewal.createdAt desc, renewal.id desc
            """)
    Page<BisCrsRenewal> search(@Param("search") String search, Pageable pageable);
}
