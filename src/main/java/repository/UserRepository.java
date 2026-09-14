package repository;

import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("""
            select u from User u
            where (:search = '' or lower(u.name) like lower(concat('%', :search, '%'))
                or lower(coalesce(u.role.name, '')) like lower(concat('%', :search, '%'))
                or lower(coalesce(u.email, '')) like lower(concat('%', :search, '%'))
                or lower(coalesce(u.phone, '')) like lower(concat('%', :search, '%'))
              or lower(coalesce(function('date_format', u.lastLogin, '%b %d, %Y %H:%i'), '')) like lower(concat('%', :search, '%'))
              or lower(coalesce(function('date_format', u.lastLogin, '%Y-%m-%d %H:%i:%s'), '')) like lower(concat('%', :search, '%')))
              and (
                    :status = 'all'
                    or (:status = 'active' and (u.status = 1 or (:highlightId > 0 and u.id = :highlightId)))
                    or (:status = 'inactive' and (u.status is null or u.status <> 1))
                  )
            order by u.createdAt desc, u.id desc
            """)
    Page<User> findFiltered(@Param("search") String search,
                            @Param("status") String status,
                            @Param("highlightId") Long highlightId,
                            Pageable pageable);
}
