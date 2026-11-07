package com.marzouki.shop.export;

import com.marzouki.shop.audit.AuditService;
import com.marzouki.shop.catalog.Product;
import com.marzouki.shop.catalog.ProductService;
import com.marzouki.shop.catalog.ProductVariantRepository;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.ListQuery;
import com.marzouki.shop.customer.CustomerRepository;
import com.marzouki.shop.customer.CustomerRow;
import com.marzouki.shop.order.OrderService;
import com.marzouki.shop.order.StockMovement;
import com.marzouki.shop.order.StockMovementRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CSV exports of the back-office lists. Each one reads the same filtered query the list shows, a page at a
 * time, so the database is never asked for a whole table in one go and nothing is loaded row by row. Very
 * large exports are refused rather than left to run for minutes.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportService {

    /** The most rows one export may contain. */
    public static final int MAX_ROWS = 100_000;
    private static final int PAGE = ListQuery.SIZES.get(ListQuery.SIZES.size() - 1);

    private final OrderService orders;
    private final ProductService products;
    private final CustomerRepository customers;
    private final StockMovementRepository movements;
    private final ProductVariantRepository variants;
    private final AuditService audit;

    @PersistenceContext
    private EntityManager entityManager;

    public void orders(ListQuery query, Writer out) throws IOException {
        CsvWriter csv = new CsvWriter(out);
        csv.row("Order", "Placed at", "Status", "Customer", "Email", "City", "Country", "Coupon", "Subtotal", "Discount", "Total");
        each(page -> orders.search(withPage(query, page)), o -> csv.row(o.getId(), o.getCreatedAt(), o.getStatus(), o.getUser().getFullName(),
                o.getUser().getEmail(), o.getCity(), o.getCountry(), o.getCouponCode(), o.getSubtotal(), o.getDiscount(), o.getTotal()));
    }

    public void products(ListQuery query, Writer out) throws IOException {
        CsvWriter csv = new CsvWriter(out);
        csv.row("SKU", "Name", "Category", "Subcategory", "Supplier", "Type", "Price", "Active", "Variants", "Stock", "Low stock level");
        each(page -> products.search(withPage(query, page)), (Product p) -> csv.row(p.getSku(), p.getName(),
                p.getSubcategory().getCategory().getTitle(), p.getSubcategory().getTitle(),
                p.getSupplier() == null ? null : p.getSupplier().getCompany(), p.getProductType() == null ? null : p.getProductType().getName(),
                p.getPrice(), p.isActive(), p.getVariants().size(), p.getStock(), p.getLowStockLevel()));
    }

    public void customers(ListQuery query, Writer out) throws IOException {
        CsvWriter csv = new CsvWriter(out);
        csv.row("Customer", "Email", "Joined", "Active", "Orders", "Spent", "Last order");
        String like = query.like();
        each(page -> {
            PageRequest paging = PageRequest.of(page, PAGE);
            return switch (query.getSort()) {
                case "newest" -> customers.byNewest(like, paging);
                case "spent" -> customers.bySpent(like, paging);
                case "orders" -> customers.byOrders(like, paging);
                default -> customers.byName(like, paging);
            };
        }, (CustomerRow c) -> csv.row(c.getFullName(), c.email(), c.joined(), c.enabled(), c.orders(), c.spent(), c.lastOrder()));
    }

    public void coupons(ListQuery query, Writer out) throws IOException {
        CsvWriter csv = new CsvWriter(out);
        csv.row("Code", "Percent off", "Enabled", "Expires", "Max uses", "Used", "Created");
        each(page -> orders.couponPage(withPage(query, page)), c -> csv.row(c.getCode(), c.getPercentOff(), c.isActive(),
                c.getExpiresAt(), c.getMaxUses(), c.getUsedCount(), c.getCreatedAt()));
    }

    /** Every stock change in a period (the last 90 days when none is given), with who made it and why. */
    public void stock(LocalDate from, LocalDate to, Writer out) throws IOException {
        Instant start = (from == null ? LocalDate.now(ZoneOffset.UTC).minusDays(90) : from).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = (to == null ? LocalDate.now(ZoneOffset.UTC) : to).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        CsvWriter csv = new CsvWriter(out);
        csv.row("When", "SKU", "Product", "Change", "Reason", "Note", "Order", "By");
        each(page -> movements.inRange(start, end, PageRequest.of(page, PAGE)), null, rows -> {
            Map<Long, com.marzouki.shop.catalog.ProductVariant> found = variants.findAllById(
                    rows.stream().map(StockMovement::getVariantId).filter(java.util.Objects::nonNull).collect(Collectors.toSet()))
                    .stream().collect(Collectors.toMap(com.marzouki.shop.catalog.ProductVariant::getId, Function.identity()));
            for (StockMovement m : rows) {
                var variant = m.getVariantId() == null ? null : found.get(m.getVariantId());
                csv.row(m.getCreatedAt(), variant == null ? null : variant.getSku(), variant == null ? null : variant.getProduct().getName(),
                        m.getDelta(), m.getReason().getLabel(), m.getNote(), m.getOrderId(), m.getActor() == null ? null : m.getActor().getEmail());
            }
        });
    }

    public void audit(String q, String outcome, LocalDate from, LocalDate to, Writer out) throws IOException {
        CsvWriter csv = new CsvWriter(out);
        csv.row("When", "Who", "Action", "Method", "Item", "Item id", "Result", "Details", "IP");
        each(page -> audit.search(q, outcome, from, to, page, PAGE), null, rows -> {
            for (var e : rows) {
                csv.row(e.getOccurredAt(), e.getActorEmail(), e.getAction(), e.getMethod(), e.getEntityType(), e.getEntityId(),
                        e.getOutcome(), e.getSummary(), e.getIp());
            }
        });
    }

    @FunctionalInterface
    private interface RowWriter<T> {
        void write(T item) throws IOException;
    }

    @FunctionalInterface
    private interface PageWriter<T> {
        void write(List<T> items) throws IOException;
    }

    private <T> void each(Function<Integer, Page<T>> fetch, RowWriter<T> row) throws IOException {
        each(fetch, row, null);
    }

    private <T> void each(Function<Integer, Page<T>> fetch, RowWriter<T> row, PageWriter<T> whole) throws IOException {
        int page = 0;
        while (true) {
            Page<T> rows = fetch.apply(page);
            if (page == 0 && rows.getTotalElements() > MAX_ROWS) {
                throw new ConflictException("That is " + rows.getTotalElements() + " rows, more than an export can hold ("
                        + MAX_ROWS + "). Narrow the search or the dates and try again.");
            }
            if (whole != null) {
                whole.write(rows.getContent());
            } else {
                for (T item : rows) {
                    row.write(item);
                }
            }
            if (!rows.hasNext()) {
                return;
            }
            entityManager.clear();
            page++;
        }
    }

    private static ListQuery withPage(ListQuery source, int page) {
        ListQuery copy = new ListQuery();
        copy.setQ(source.getQ());
        copy.setStatus(source.getStatus());
        copy.setSort(source.getSort());
        copy.setSize(PAGE);
        copy.setPage(page);
        return copy;
    }
}