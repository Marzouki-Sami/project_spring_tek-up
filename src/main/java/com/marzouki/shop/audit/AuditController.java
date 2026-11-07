package com.marzouki.shop.audit;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** The audit log viewer for admins. Read only. */
@Controller
@RequiredArgsConstructor
public class AuditController {

    private static final java.util.List<Integer> SIZES = java.util.List.of(25, 50, 100);

    private final AuditService audit;

    @GetMapping("/admin/audit")
    public String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String outcome,
                       @RequestParam(defaultValue = "") String from, @RequestParam(defaultValue = "") String to,
                       @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size, Model model) {
        int pageSize = SIZES.contains(size) ? size : 50;
        Page<AuditEntry> entries = audit.search(q, outcome, date(from), date(to), page, pageSize);
        model.addAttribute("entries", entries);
        model.addAttribute("q", q.trim());
        model.addAttribute("outcome", outcome);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("size", pageSize);
        model.addAttribute("sizes", SIZES);
        return "admin/audit";
    }

    private static LocalDate date(String text) {
        try {
            return text == null || text.isBlank() ? null : LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}