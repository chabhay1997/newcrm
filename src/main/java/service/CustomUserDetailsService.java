package service;

import model.User;
import repository.UserRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomUserDetailsService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        boolean enabled = user.getStatus() == null || user.getStatus() != 0;

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String roleName = (user.getRoleId() != null && user.getRoleId() == 1)
                ? "ROLE_ADMIN"
                : "ROLE_USER";

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword() != null ? user.getPassword() : "")
                .disabled(!enabled)
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(roleName)))
                .build();
    }

    public void registerUser(
            String name,
            String email,
            String rawPassword,
            Integer roleId,
            Long typeId,
            String phone,
            LocalDate dob,
            String permissions,
            String profile,
            String location,
            BigDecimal latitude,
            BigDecimal longitude,
            String emailVerifiedAt,
            Integer status,
            Integer bisDeadlinePopupEnabled,
            Integer handbookSeen,
            String rememberToken
    ) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already registered!");
        }

        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setDob(dob);
        newUser.setPermissions(permissions);
        newUser.setProfile(profile);
        newUser.setLocation(location);
        newUser.setLatitude(latitude);
        newUser.setLongitude(longitude);
        newUser.setEmailVerifiedAt(emailVerifiedAt);
        newUser.setPassword(passwordEncoder.encode(rawPassword));
        newUser.setRoleId(roleId);
        newUser.setTypeId(typeId);
        newUser.setStatus(status);
        newUser.setBisDeadlinePopupEnabled(bisDeadlinePopupEnabled);
        newUser.setHandbookSeen(handbookSeen);
        newUser.setRememberToken(rememberToken);

        userRepository.save(newUser);
    }
}
