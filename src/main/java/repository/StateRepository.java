package repository;
import model.State;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface StateRepository extends JpaRepository<State, Long> { List<State> findByCountryIdOrderByNameAsc(Integer countryId); }
