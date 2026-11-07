package com.marzouki.shop.security.mfa;

import com.marzouki.shop.audit.AuditService;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Authenticator-app sign-in: setup, checking codes, recovery codes and trusted devices. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TwoFactorService {

    static final int RECOVERY_CODES = 10;
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RecoveryCodeRepository recoveryCodes;
    private final TrustedDeviceRepository devices;
    private final SecretVault vault;
    private final MfaProperties properties;
    private final MfaAttempts attempts;
    private final AuditService audit;

    public boolean isRequiredFor(Role role) {
        return properties.staff2fa() && role.isStaff();
    }

    public String newSecret() {
        return Totp.newSecret();
    }

    public String qrFor(User user, String secret) {
        return QrCodes.dataUri(Totp.uri(properties.issuer(), user.getEmail(), secret));
    }

    public long recoveryCodesLeft(Long userId) {
        return recoveryCodes.countByUserIdAndUsedAtIsNull(userId);
    }

    public long trustedDevices(Long userId) {
        return devices.countByUserIdAndExpiresAtAfter(userId, Instant.now());
    }

    public int trustDays() {
        return properties.trustDays();
    }

    /** Turns two-factor on after the user proves their app shows the right code. Returns the recovery codes, once. */
    @Transactional
    public List<String> enable(Long userId, String secret, String code) {
        attempts.check(userId);
        User user = get(userId);
        if (user.isTotpEnabled()) {
            throw new ConflictException("Two-factor authentication is already on");
        }
        long step = Totp.verify(secret, code, System.currentTimeMillis(), null);
        if (step < 0) {
            attempts.fail(userId);
            throw new ConflictException("That code is not right. Check the clock on your phone and try again.");
        }
        user.setTotpSecret(vault.encrypt(secret));
        user.setTotpEnabled(true);
        user.setTotpLastStep(step);
        List<String> fresh = issueRecoveryCodes(userId);
        attempts.clear(userId);
        audit.record("2fa.enabled", "User", userId, "Two-factor authentication turned on");
        return fresh;
    }

    /** Accepts an authenticator code or an unused recovery code. */
    @Transactional
    public boolean verify(Long userId, String input) {
        attempts.check(userId);
        User user = get(userId);
        if (!user.isTotpEnabled() || input == null) {
            return false;
        }
        String text = input.trim();
        boolean ok;
        if (text.replaceAll("[\\s-]", "").matches("[0-9]{6}")) {
            long step = Totp.verify(vault.decrypt(user.getTotpSecret()), text, System.currentTimeMillis(), user.getTotpLastStep());
            ok = step >= 0 && users.claimTotpStep(userId, step) == 1;
        } else {
            ok = useRecoveryCode(userId, text);
        }
        if (ok) {
            attempts.clear(userId);
        } else {
            attempts.fail(userId);
        }
        return ok;
    }

    @Transactional
    public List<String> regenerateRecoveryCodes(Long userId, String code) {
        if (!verify(userId, code)) {
            throw new ConflictException("That code is not right");
        }
        audit.record("2fa.recovery-codes", "User", userId, "Recovery codes replaced");
        return issueRecoveryCodes(userId);
    }

    /** Staff cannot turn it off; everyone else can. */
    @Transactional
    public void disable(Long userId) {
        User user = get(userId);
        if (isRequiredFor(user.getRole())) {
            throw new ConflictException("Managers and admins must keep two-factor authentication on");
        }
        clear(user);
        audit.record("2fa.disabled", "User", userId, "Two-factor authentication turned off");
    }

    /** An admin clears two-factor for someone who lost their phone and recovery codes. They must set it up again. */
    @Transactional
    public void reset(Long userId) {
        clear(get(userId));
        audit.record("2fa.reset", "User", userId, "Two-factor authentication reset by an admin");
    }

    private void clear(User user) {
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        user.setTotpLastStep(null);
        recoveryCodes.deleteAllForUser(user.getId());
        devices.deleteAllForUser(user.getId());
    }

    /** Remembers this browser so the code is skipped on later sign-ins. Returns the cookie value. */
    @Transactional
    public String trustDevice(Long userId, String userAgent) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String label = userAgent == null || userAgent.isBlank() ? "Browser" : userAgent.substring(0, Math.min(120, userAgent.length()));
        devices.save(new TrustedDevice(userId, sha256(token), label, Instant.now().plus(properties.trustDays(), ChronoUnit.DAYS)));
        return token;
    }

    public boolean isTrusted(Long userId, String token) {
        return token != null && !token.isBlank()
                && devices.findByTokenHashAndUserIdAndExpiresAtAfter(sha256(token), userId, Instant.now()).isPresent();
    }

    @Transactional
    public void forgetDevices(Long userId) {
        devices.deleteAllForUser(userId);
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpiredDevices() {
        devices.deleteExpired(Instant.now());
    }

    private boolean useRecoveryCode(Long userId, String text) {
        String normal = text.toUpperCase().replaceAll("[\\s-]", "");
        return recoveryCodes.findByUserIdAndCodeHashAndUsedAtIsNull(userId, vault.hash(normal))
                .map(c -> recoveryCodes.markUsed(c.getId(), Instant.now()) == 1).orElse(false);
    }

    private List<String> issueRecoveryCodes(Long userId) {
        recoveryCodes.deleteAllForUser(userId);
        List<String> shown = new ArrayList<>();
        for (int i = 0; i < RECOVERY_CODES; i++) {
            StringBuilder raw = new StringBuilder();
            for (int j = 0; j < 10; j++) {
                raw.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            recoveryCodes.save(new RecoveryCode(userId, vault.hash(raw.toString())));
            shown.add(raw.substring(0, 5) + "-" + raw.substring(5));
        }
        return shown;
    }

    private User get(Long userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}