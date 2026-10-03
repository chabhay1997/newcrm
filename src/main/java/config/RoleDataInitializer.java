package config;

import model.Role;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import repository.RoleRepository;

@Component
public class RoleDataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    public RoleDataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (roleRepository.existsByNameIgnoreCase("Testing")) {
            return;
        }

        Integer nextId = roleRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(Role::getId)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .map(id -> id + 1)
                .orElse(1);

        Role testingRole = new Role();
        testingRole.setId(nextId);
        testingRole.setName("Testing");
        roleRepository.save(testingRole);
    }
}
