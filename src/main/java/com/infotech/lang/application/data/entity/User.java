package com.infotech.lang.application.data.entity;


import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

@Entity
@Table(name = "\"user\"")
public class User extends AbstractEntity {

    @NotEmpty
    private String firstName = "";

    @NotEmpty
    private String lastName = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.defaultRole();

    @Column(nullable = false)
    private Boolean enabled = true;
    
    @ManyToOne
    private Status status;

    @Email
    @NotEmpty
    private String email = "";

    // Unique username/user ID for login (created by user during registration)
    @Column(name = "user_name", unique = true)
    @NotEmpty
    private String userName = "";

    // Password field for custom authentication (hashed, not plain text)
    // Null for OAuth users
    private String password;

    // Authentication provider: "LOCAL" for email/password, "GOOGLE" for OAuth
    private String authProvider = "LOCAL";

    // For future email verification feature
    private Boolean emailVerified = false;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "sex", length = 20)
    private String sex;

    @Column(name = "street", length = 100)
    private String street;

    @Column(name = "city", length = 60)
    private String city;

    @Column(name = "state", length = 60)
    private String state;

    @Column(name = "country", length = 60)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "native_language", length = 18)
    private String nativeLanguage;

    // Curriculum level (e.g. CBSE level 1, 2, ...). Nullable and left unset for new
    // users — there's no single sensible default across every curriculum a student
    // might belong to; it only becomes meaningful once assigned to a
    // level-differentiating curriculum. Set manually by a teacher/admin via the
    // student level promotion UI (see
    // docs/cbse-level1-hindi-vocabulary-features/06-student-level-promotion.md).
    // No content currently reads this value — Level 1 is the only level that exists.
    @Column(name = "level")
    private Integer level;

    @Override
    public String toString() {
        return firstName + " " + lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserRole getRoleOrDefault() {
        return role != null ? role : UserRole.defaultRole();
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAccountEnabled() {
        return !Boolean.FALSE.equals(enabled);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEffectiveDisplayName() {
        if (displayName != null && !displayName.isBlank()) return displayName.trim();
        if (firstName != null && !firstName.isBlank()) return firstName.trim();
        return email;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getNativeLanguage() {
        return nativeLanguage;
    }

    public void setNativeLanguage(String nativeLanguage) {
        this.nativeLanguage = nativeLanguage;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }
}
