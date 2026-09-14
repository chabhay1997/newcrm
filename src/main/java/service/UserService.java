package service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import repository.UserRepository;
import repository.RoleRepository;
import config.PermissionConfig;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = new ObjectMapper();
    }

    public Page<User> findUsers(String search, String status, String size, int page) {
        return findUsers(search, status, size, page, 0L);
    }

    public Page<User> findUsers(String search, String status, String size, int page, Long highlightId) {
        String normalizedSearch = normalizeSearch(search);
        String normalizedStatus = normalizeStatus(status);
        Long normalizedHighlightId = highlightId == null ? 0L : highlightId;
        if ("all".equals(size)) {
            Page<User> allUsers = userRepository.findFiltered(normalizedSearch, normalizedStatus, normalizedHighlightId, Pageable.unpaged());
            int allRowsSize = Math.max(allUsers.getNumberOfElements(), 1);
            return new PageImpl<>(allUsers.getContent(), PageRequest.of(0, allRowsSize), allUsers.getTotalElements());
        }
        return userRepository.findFiltered(normalizedSearch, normalizedStatus, normalizedHighlightId,
                PageRequest.of(Math.max(page, 0), pageSize(size)));
    }

    public List<User> findUsersForExport(String search, String status) {
        return userRepository.findFiltered(normalizeSearch(search), normalizeStatus(status), 0L, Pageable.unpaged())
                .getContent();
    }

    public void updateName(Long id, String name) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        Long userId = Objects.requireNonNull(id, "User id is required");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setName(normalizedName);
        userRepository.save(user);
    }

    public User getUser(Long id) {
        return userRepository.findById(Objects.requireNonNull(id, "User id is required"))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User getUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return userRepository.findByEmail(email.trim()).orElse(null);
    }

    public Set<String> selectedPermissions(User user) {
        if (user.getPermissions() == null || user.getPermissions().isBlank()) {
            return Set.of();
        }

        String rawPermissions = user.getPermissions().trim();
        if (rawPermissions.startsWith("{") && rawPermissions.endsWith("}")) {
            try {
                Map<String, Object> permissionMap = objectMapper.readValue(rawPermissions, new TypeReference<>() {});
                Set<String> selected = new LinkedHashSet<>();
                for (Map.Entry<String, Object> moduleEntry : permissionMap.entrySet()) {
                    Object actions = moduleEntry.getValue();
                    if (!(actions instanceof Map<?, ?> actionMap)) {
                        continue;
                    }
                    for (Map.Entry<?, ?> actionEntry : actionMap.entrySet()) {
                        Object value = actionEntry.getValue();
                        if (value instanceof Number numericValue && numericValue.intValue() == 1) {
                            selected.add(moduleEntry.getKey() + "." + actionEntry.getKey());
                        } else if (value instanceof String stringValue && "1".equals(stringValue)) {
                            selected.add(moduleEntry.getKey() + "." + actionEntry.getKey());
                        }
                    }
                }
                return selected;
            } catch (Exception ignored) {
                // Fallback to legacy CSV storage if the value is not valid JSON.
            }
        }

        return Arrays.stream(rawPermissions
                        .replace("[", "")
                        .replace("]", "")
                        .replace("\"", "")
                        .split(","))
                .map(value -> value.trim())
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> permissionActions(User user, String permissionKey) {
        Set<String> selected = selectedPermissions(user);
        Set<String> actions = new LinkedHashSet<>();
        for (String action : Arrays.asList("read", "write", "edit", "delete", "excel")) {
            if (selected.contains(permissionKey + "." + action) || selected.contains(permissionKey)) {
                actions.add(action);
            }
        }
        return actions;
    }

    public java.util.Map<String, java.util.Map<String, String>> permissionGroups() {
        return PermissionConfig.groups();
    }

    public List<model.Role> findRoles() {
        return roleRepository.findAll();
    }

    public boolean isAdmin(String email) {
        return email != null && userRepository.findByEmail(email)
                .map(user -> user.getRoleId() != null && user.getRoleId() == 1)
                .orElse(false);
    }

    public User createUser(String name, String email, String phone, String location,
                          Integer status, BigDecimal latitude, BigDecimal longitude, Integer roleId,
                          LocalDate dob, String password, String[] permissions) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (userRepository.findByEmail(email.trim()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setName(normalizedName);
        user.setEmail(email.trim());
        user.setPhone(phone == null ? null : phone.trim());
        user.setLocation(location == null ? null : location.trim());
        user.setStatus(status != null ? status : 1);
        user.setLatitude(latitude);
        user.setLongitude(longitude);
        user.setRoleId(roleId == null ? 1 : roleId);
        user.setTypeId(2L);
        user.setDob(dob);
        user.setPassword(passwordEncoder.encode(password == null || password.isBlank() ? "Welcome@123" : password));
        user.setPermissions(normalizePermissions(permissions));
        return userRepository.save(user);
    }

    public void updateUser(Long id, String name, String email, String phone, String location,
                           Integer status, BigDecimal latitude, BigDecimal longitude, Integer roleId,
                           LocalDate dob, String password, String[] permissions) {
        User user = getUser(id);
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        user.setName(normalizedName);
        user.setEmail(email == null ? null : email.trim());
        user.setPhone(phone == null ? null : phone.trim());
        user.setLocation(location == null ? null : location.trim());
        user.setStatus(status);
        user.setLatitude(latitude);
        user.setLongitude(longitude);
        user.setRoleId(roleId);
        user.setDob(dob);
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        user.setPermissions(normalizePermissions(permissions));
        userRepository.save(user);
    }

    public void updateStatus(Long id, Integer status) {
        User user = getUser(id);
        user.setStatus(status != null && status == 1 ? 1 : 0);
        userRepository.save(user);
    }

    public void updateLocation(String email, String latitude, String longitude) {
        if (email == null || latitude == null || longitude == null
                || latitude.isBlank() || longitude.isBlank()) {
            return;
        }

        try {
            BigDecimal parsedLatitude = new BigDecimal(latitude);
            BigDecimal parsedLongitude = new BigDecimal(longitude);
            if (parsedLatitude.compareTo(BigDecimal.valueOf(-90)) < 0
                    || parsedLatitude.compareTo(BigDecimal.valueOf(90)) > 0
                    || parsedLongitude.compareTo(BigDecimal.valueOf(-180)) < 0
                    || parsedLongitude.compareTo(BigDecimal.valueOf(180)) > 0) {
                return;
            }

            userRepository.findByEmail(email).ifPresent(user -> {
                user.setLatitude(parsedLatitude);
                user.setLongitude(parsedLongitude);
                userRepository.save(user);
            });
        } catch (NumberFormatException ignored) {
            // Malformed browser coordinates should not block login.
        }
    }

    public void delete(Long id) {
        Long userId = Objects.requireNonNull(id, "User id is required");
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found");
        }
        userRepository.deleteById(userId);
    }

    public int pageSize(String size) {
        return switch (size == null ? "25" : size) {
            case "50" -> 50;
            case "100" -> 100;
            default -> 25;
        };
    }

    public String buildPermissionJson(String[] permissions) {
        if (permissions == null || permissions.length == 0) {
            return "";
        }

        Map<String, Map<String, Integer>> permissionMap = new LinkedHashMap<>();
        for (String permission : permissions) {
            if (permission == null) {
                continue;
            }
            String value = permission.trim();
            if (value.isBlank()) {
                continue;
            }
            int separatorIndex = value.lastIndexOf('.');
            if (separatorIndex <= 0 || separatorIndex == value.length() - 1) {
                continue;
            }

            String module = value.substring(0, separatorIndex);
            String action = value.substring(separatorIndex + 1);
            if (!Set.of("read", "write", "edit", "delete", "excel").contains(action)) {
                continue;
            }

            permissionMap.computeIfAbsent(module, key -> new LinkedHashMap<>())
                    .put(action, 1);
        }

        if (permissionMap.isEmpty()) {
            return "";
        }

        try {
            return objectMapper.writeValueAsString(permissionMap);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize permissions", e);
        }
    }

    private String normalizePermissions(String[] permissions) {
        return buildPermissionJson(permissions);
    }

    private String normalizeSearch(String search) {
        return search == null ? "" : search.trim();
    }

    private String normalizeStatus(String status) {
        return "active".equals(status) || "inactive".equals(status) ? status : "all";
    }
}