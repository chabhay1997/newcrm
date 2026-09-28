package repository;
import model.State;
import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< HEAD
import java.util.List; import java.util.Optional;
public interface StateRepository extends JpaRepository<State, Long> { List<State> findByCountryIdOrderByNameAsc(Integer countryId); Optional<State> findFirstByNameIgnoreCase(String name); Optional<State> findFirstByCountryIdAndNameIgnoreCase(Integer countryId,String name); }
=======
import java.util.List;
public interface StateRepository extends JpaRepository<State, Long> { List<State> findByCountryIdOrderByNameAsc(Integer countryId); }
>>>>>>> 91cef887e4e2d034cf24dd0094d8272c8b034ea4
