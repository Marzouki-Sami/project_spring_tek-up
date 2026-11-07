package com.marzouki.shop.audit;

import com.marzouki.shop.common.CurrentActor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Records every change a manager or admin makes, in the back office and through the API. It works on
 * the request as a whole, so new screens are covered without having to remember to log them.
 */
@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {

    /** Request parameters that are safe and useful to show. Passwords and tokens are never among them. */
    private static final List<String> SHOWN = List.of("action", "scope", "status", "delta", "reason", "price",
            "role", "enabled", "name", "title", "label", "percent", "code", "quantity", "optionTypeId");

    private final AuditService audit;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception failure) {
        String method = request.getMethod();
        if (method.equals("GET") || method.equals("HEAD") || method.equals("OPTIONS") || method.equals("TRACE")) {
            return;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean api = path.startsWith("/api/");
        if (api && (path.startsWith("/api/auth/") || !isStaff())) {
            return;
        }
        if (CurrentActor.id() == null) {
            return;
        }
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, String> vars = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        List<String> words = new ArrayList<>();
        String[] parts = pattern.split("/");
        for (int i = 2; i < parts.length; i++) {
            boolean prefix = i == 2 && parts[i].equals("manage");
            if (!prefix && !parts[i].isEmpty() && !parts[i].startsWith("{")) {
                words.add(parts[i]);
            }
        }
        if (words.isEmpty()) {
            return;
        }
        boolean endsWithVariable = pattern.endsWith("}");
        if (method.equals("DELETE")) {
            words.add("delete");
        } else if (method.equals("PUT") || method.equals("PATCH") || (method.equals("POST") && endsWithVariable)) {
            words.add("update");
        } else if (method.equals("POST") && words.size() == 1) {
            words.add("create");
        }
        String entityId = vars == null || vars.isEmpty() ? null : vars.values().iterator().next();
        audit.record(String.join(".", words), method, words.get(0), entityId, outcome(request, response, failure, api),
                summary(request, method, path));
    }

    private static String outcome(HttpServletRequest request, HttpServletResponse response, Exception failure, boolean api) {
        int status = response.getStatus();
        if (failure != null || status >= 500) {
            return AuditEntry.ERROR;
        }
        if (api) {
            return status < 300 ? AuditEntry.OK : AuditEntry.REJECTED;
        }
        boolean redirect = status >= 300 && status < 400;
        Map<String, ?> flash = RequestContextUtils.getOutputFlashMap(request);
        boolean errorMessage = flash != null && flash.containsKey("error");
        return redirect && !errorMessage ? AuditEntry.OK : AuditEntry.REJECTED;
    }

    private static String summary(HttpServletRequest request, String method, String path) {
        StringBuilder text = new StringBuilder(method).append(' ').append(path);
        List<String> shown = new ArrayList<>();
        for (String name : SHOWN) {
            String value = request.getParameter(name);
            if (value != null && !value.isBlank()) {
                shown.add(name + "=" + (value.length() > 40 ? value.substring(0, 40) + "..." : value));
            }
        }
        String[] ids = request.getParameterValues("ids");
        if (ids != null) {
            shown.add("ids=" + ids.length);
        }
        if (!shown.isEmpty()) {
            text.append(" (").append(String.join(", ", shown)).append(')');
        }
        return text.toString();
    }

    private static boolean isStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_ADMIN"));
    }
}