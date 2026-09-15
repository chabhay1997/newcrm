package service;

import model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import repository.RoleRepository;
import repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Test
    void selectedPermissionsKeepsDistinctActionValues() {
        UserService userService = new UserService(
                mock(UserRepository.class),
                mock(RoleRepository.class),
                mock(PasswordEncoder.class)
        );

        User user = new User();
        user.setPermissions("user.read, client.write, all_lead.delete, user.read, client.write");

        assertEquals(Set.of("user.read", "client.write", "all_lead.delete"), userService.selectedPermissions(user));
    }

    @Test
    void selectedPermissionsReadsNestedJsonPermissionMap() {
        UserService userService = new UserService(
                mock(UserRepository.class),
                mock(RoleRepository.class),
                mock(PasswordEncoder.class)
        );

        User user = new User();
        user.setPermissions("{\"user\":{\"read\":1,\"write\":1,\"edit\":0,\"delete\":0},\"client\":{\"read\":1,\"write\":0,\"edit\":0,\"delete\":0}}\n");

        assertEquals(Set.of("user.read", "user.write", "client.read"), userService.selectedPermissions(user));
    }

    @Test
    void createUserStoresBasicFieldsAndEncodedPassword() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode("StrongPass123")).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserService userService = new UserService(
                userRepository,
                mock(RoleRepository.class),
                passwordEncoder
        );

        User created = userService.createUser(
                "Jane Doe",
                "jane@example.com",
                "9876543210",
                "Mumbai",
                1,
                BigDecimal.valueOf(18.99),
                BigDecimal.valueOf(72.88),
                2,
                LocalDate.of(1995, 5, 10),
                "StrongPass123",
                new String[] {"user.read", "client.write"}
        );

        assertNotNull(created);
        assertEquals("Jane Doe", created.getName());
        assertEquals("jane@example.com", created.getEmail());
        assertEquals("encoded-pass", created.getPassword());
        assertEquals(2, created.getRoleId());
        assertEquals("{\"user\":{\"read\":1},\"client\":{\"write\":1}}", created.getPermissions());
    }

    @Test
    void getUserByEmailReturnsSavedCoordinates() {
        UserRepository userRepository = mock(UserRepository.class);
        User expectedUser = new User();
        expectedUser.setLatitude(BigDecimal.valueOf(28.6139));
        expectedUser.setLongitude(BigDecimal.valueOf(77.2090));
        when(userRepository.findByEmail("demo@example.com")).thenReturn(Optional.of(expectedUser));

        UserService userService = new UserService(
                userRepository,
                mock(RoleRepository.class),
                mock(PasswordEncoder.class)
        );

        User foundUser = userService.getUserByEmail("demo@example.com");

        assertNotNull(foundUser);
        assertEquals(BigDecimal.valueOf(28.6139), foundUser.getLatitude());
        assertEquals(BigDecimal.valueOf(77.2090), foundUser.getLongitude());
    }
}
