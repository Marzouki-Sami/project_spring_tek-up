package com.marzouki.shop.audit;

import com.marzouki.shop.common.CurrentActor;
import com.marzouki.shop.common.Like;
import com.marzouki.shop.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Writes and reads the audit log. Recording never fails the action being recorded. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Instant FOREVER = Instant.parse("3000-01-01T00:00:00Z");

    private final AuditRepository entries;
    private final UserRepository users;

    /** Records an action by whoever is signed in on the current request. */
    public void record(String action, String method, String entityType, String entityId, String outcome, String summary) {
        try {
            Long actorId = CurrentActor.id();
            String email = actorId == null ? null : users.findById(actorId).map(u -> u.getEmail()).orElse(null);
            save(new AuditEntry(actorId, email, action, method, entityType, entityId, outcome, summary, currentIp()));
        } catch (RuntimeException e) {
            log.warn("Could not write the audit log entry for {}", action, e);
        }
    }

    /** Shorthand for a successful action that is not a plain web request, such as a security change. */
    public void record(String action, String entityType, Object entityId, String summary) {
        record(action, null, entityType, entityId == null ? null : String.valueOf(entityId), AuditEntry.OK, summary);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void save(AuditEntry entry) {
        entries.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditEntry> search(String q, String outcome, LocalDate from, LocalDate to, int page, int size) {
        String text = q == null ? "" : q.trim();
        String exact = text.matches("[0-9]{1,18}") ? text : "-";
        Instant start = from == null ? Instant.EPOCH : from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = to == null ? FOREVER : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        String wanted = outcome == null ? "" : outcome.trim().toUpperCase();
        if (!wanted.isEmpty() && !java.util.List.of(AuditEntry.OK, AuditEntry.REJECTED, AuditEntry.ERROR).contains(wanted)) {
            wanted = "";
        }
        return entries.search(Like.contains(text), exact, wanted, start, end, PageRequest.of(Math.max(page, 0), size));
    }

    private static String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRemoteAddr();
        }
        return null;
    }
}