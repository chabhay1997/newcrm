package model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Integer roleId = 1;

    @Column(name = "type_id", nullable = false)
    private Long typeId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    private String phone;

    private LocalDate dob;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String permissions;

    private String profile;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    private String location;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "email_verified_at", length = 10)
    private String emailVerifiedAt;

    private String password;

    private Integer status;

    @Column(name = "bis_deadline_popup_enabled")
    private Integer bisDeadlinePopupEnabled;

    @Column(name = "handbook_seen")
    private Integer handbookSeen;

    @Column(name = "remember_token", length = 100)
    private String rememberToken;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public User() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone(){ return phone; }
    public void setPhone(String phone){ this.phone = phone; }

    public LocalDate getDob(){ return dob; }
    public void setDob(LocalDate dob){ this.dob = dob; }

    public String getPermissions(){ return permissions; }
    public void setPermissions(String permissions){ this.permissions = permissions; }

    public String getProfile(){ return profile; }
    public void setProfile(String profile){ this.profile = profile; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public String getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(String emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getBisDeadlinePopupEnabled() { return bisDeadlinePopupEnabled; }
    public void setBisDeadlinePopupEnabled(Integer bisDeadlinePopupEnabled) { this.bisDeadlinePopupEnabled = bisDeadlinePopupEnabled; }

    public Integer getHandbookSeen() { return handbookSeen; }
    public void setHandbookSeen(Integer handbookSeen) { this.handbookSeen = handbookSeen; }

    public String getRememberToken() { return rememberToken; }
    public void setRememberToken(String rememberToken) { this.rememberToken = rememberToken; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
