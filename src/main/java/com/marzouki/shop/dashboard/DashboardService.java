package com.marzouki.shop.dashboard;

import com.marzouki.shop.catalog.Product;
import com.marzouki.shop.catalog.ProductRepository;
import com.marzouki.shop.order.OrderRepository;
import com.marzouki.shop.order.OrderStatus;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.UserRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    public static final int LOW_STOCK_LEVEL = com.marzouki.shop.order.OrderService.LOW_STOCK_LEVEL;
    private static final int DAYS = 30;

    private final OrderRepository orders;
    private final ProductRepository products;
    private final UserRepository users;

    public record StatusCount(OrderStatus status, long count) {
    }

    public record TopSeller(String sku, String name, long units) {
    }

    public record Overview(int days, BigDecimal revenue, long orderCount, long customers, long activeProducts,
                           List<StatusCount> byStatus, List<TopSeller> topSellers, List<Product> lowStock) {
    }

    public Overview overview() {
        Instant since = Instant.now().minus(Duration.ofDays(DAYS));
        List<Object[]> rows = orders.countByStatus();
        List<StatusCount> byStatus = new ArrayList<>();
        for (OrderStatus status : OrderStatus.values()) {
            long count = rows.stream().filter(r -> r[0] == status).mapToLong(r -> (Long) r[1]).sum();
            byStatus.add(new StatusCount(status, count));
        }
        List<TopSeller> top = orders.topSellers(PageRequest.of(0, 5)).stream()
                .map(r -> new TopSeller((String) r[0], (String) r[1], ((Number) r[2]).longValue())).toList();
        return new Overview(DAYS, orders.revenueSince(since), orders.countSince(since),
                users.countByRole(Role.CUSTOMER), products.countByActiveTrue(), byStatus, top,
                products.findTop10ByActiveTrueAndStockLessThanEqualOrderByStockAscNameAsc(LOW_STOCK_LEVEL));
    }
}