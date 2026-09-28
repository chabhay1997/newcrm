package repository;

import model.State;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StateRepository extends JpaRepository<State, Long> {
    List<State> findByCountryIdOrderByNameAsc(Integer countryId);
    Optional<State> findFirstByNameIgnoreCase(String name);
    Optional<State> findFirstByCountryIdAndNameIgnoreCase(Integer countryId, String name);
}
