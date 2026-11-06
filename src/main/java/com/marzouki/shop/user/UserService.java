package com.marzouki.shop.user;

import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import com.marzouki.shop.user.dto.PasswordChangeForm;
import com.marzouki.shop.user.dto.ProfileForm;
import com.marzouki.shop.user.dto.RegistrationForm;
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

    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }

    public User findByEmail(String email) {
        return users.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new NotFoundException("User", email));
    }

    public List<User> findAll() {
        return users.findAllByOrderByLastNameAscFirstNameAsc();
    }

    /** Creates a customer account. Public sign-up can never create a more privileged role. */
    @Transactional
    public User register(RegistrationForm form) {
        return create(form.getFirstName(), form.getLastName(), form.getEmail(), form.getPassword(), Role.CUSTOMER);
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
}