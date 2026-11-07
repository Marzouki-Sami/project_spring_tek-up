package com.marzouki.shop.user;

import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import com.marzouki.shop.user.dto.PasswordChangeForm;
import com.marzouki.shop.user.dto.ProfileForm;
import com.marzouki.shop.user.dto.RegistrationForm;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final com.marzouki.shop.security.mfa.TwoFactorService twoFactor;
    private final EmailTokenService emailTokens;
    private final org.springframework.context.ApplicationEventPublisher events;

    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }

    public User findByEmail(String email) {
        return users.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new NotFoundException("User", email));
    }

    public org.springframework.data.domain.Page<User> search(com.marzouki.shop.common.ListQuery query) {
        Instant now = Instant.now();
        org.springframework.data.jpa.domain.Specification<User> status = switch (query.getStatus()) {
            case "CUSTOMER", "MANAGER", "ADMIN" -> (r, q, cb) -> cb.equal(r.get("role"), Role.valueOf(query.getStatus()));
            case "DISABLED" -> (r, q, cb) -> cb.isFalse(r.get("enabled"));
            case "LOCKED" -> (r, q, cb) -> cb.greaterThan(r.<Instant>get("lockedUntil"), now);
            case "NO2FA" -> (r, q, cb) -> cb.and(cb.isFalse(r.get("totpEnabled")), cb.notEqual(r.get("role"), Role.CUSTOMER));
            default -> com.marzouki.shop.common.Specs.none();
        };
        var sort = query.getSort().equals("newest")
                ? org.springframework.data.domain.Sort.by("createdAt").descending().and(org.springframework.data.domain.Sort.by("id").descending())
                : org.springframework.data.domain.Sort.by("lastName").ascending().and(org.springframework.data.domain.Sort.by("firstName")).and(org.springframework.data.domain.Sort.by("id"));
        return users.findAll(com.marzouki.shop.common.Specs.<User>text(query.like(), "firstName", "lastName", "email").and(status),
                query.pageRequest(sort));
    }

    public List<User> findAll() {
        return users.findAllByOrderByLastNameAscFirstNameAsc();
    }

    /** Creates a customer account. Public sign-up can never create a more privileged role. */
    @Transactional
    public User register(RegistrationForm form) {
        User user = create(form.getFirstName(), form.getLastName(), form.getEmail(), form.getPassword(), Role.CUSTOMER);
        user.setEmailVerified(false);
        events.publishEvent(new UserEvents.VerificationRequested(user.getId(),
                emailTokens.issue(user, EmailToken.Type.VERIFY)));
        return user;
    }

    /** Creates an account with any role. Only for trusted callers: setup code and tests. */
    @Transactional
    public User create(String firstName, String lastName, String email, String password, Role role) {
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(normalized)) {
            throw new ConflictException("An account with this email address already exists");
        }
        User user = new User();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setEmail(normalized);
        user.setPasswordHash(encoder.encode(password));
        user.setRole(role);
        user.setEmailVerified(true);
        return users.save(user);
    }

    @Transactional
    public User updateProfile(Long id, ProfileForm form) {
        User user = get(id);
        user.setFirstName(form.getFirstName().trim());
        user.setLastName(form.getLastName().trim());
        user.setPhone(form.getPhone() == null || form.getPhone().isBlank() ? null : form.getPhone().trim());
        return user;
    }

    public ProfileForm profileFor(Long id) {
        User user = get(id);
        ProfileForm form = new ProfileForm();
        form.setFirstName(user.getFirstName());
        form.setLastName(user.getLastName());
        form.setPhone(user.getPhone());
        return form;
    }

    /** Changing the password signs the account out of every API session. */
    @Transactional
    public void changePassword(Long id, PasswordChangeForm form) {
        User user = get(id);
        if (!encoder.matches(form.getCurrentPassword(), user.getPasswordHash())) {
            throw new ConflictException("The current password is not correct");
        }
        user.setPasswordHash(encoder.encode(form.getNewPassword()));
        refreshTokens.revokeAllForUser(id);
        twoFactor.forgetDevices(id);
    }

    @Transactional
    public User changeRole(Long actorId, Long targetId, Role role) {
        if (actorId.equals(targetId)) {
            throw new ConflictException("You cannot change your own role");
        }
        User target = get(targetId);
        target.setRole(role);
        return target;
    }

    @Transactional
    public User setEnabled(Long actorId, Long targetId, boolean enabled) {
        if (actorId.equals(targetId)) {
            throw new ConflictException("You cannot disable your own account");
        }
        User target = get(targetId);
        target.setEnabled(enabled);
        if (!enabled) {
            refreshTokens.revokeAllForUser(targetId);
        }
        return target;
    }

    @Transactional
    public User unlock(Long id) {
        User user = get(id);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        return user;
    }

    @Transactional
    public void resendVerification(Long id) {
        User user = get(id);
        if (!user.isEmailVerified()) {
            events.publishEvent(new UserEvents.VerificationRequested(user.getId(),
                    emailTokens.issue(user, EmailToken.Type.VERIFY)));
        }
    }

    @Transactional
    public User verifyEmail(String token) {
        User user = emailTokens.consume(token, EmailToken.Type.VERIFY);
        user.setEmailVerified(true);
        return user;
    }

    /** Silent when the address is unknown, so the form cannot be used to discover who has an account. */
    @Transactional
    public void requestPasswordReset(String email) {
        users.findByEmailIgnoreCase(email.trim()).filter(User::isEnabled).ifPresent(user ->
                events.publishEvent(new UserEvents.PasswordResetRequested(user.getId(),
                        emailTokens.issue(user, EmailToken.Type.RESET))));
    }

    /** Sets a new password from a reset link. Receiving the email also proves the address, and every API session ends. */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = emailTokens.consume(token, EmailToken.Type.RESET);
        user.setPasswordHash(encoder.encode(newPassword));
        user.setEmailVerified(true);
        user.setFailedAttempts(0);
        twoFactor.forgetDevices(user.getId());
        user.setLockedUntil(null);
        refreshTokens.revokeAllForUser(user.getId());
    }
}