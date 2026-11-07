package com.marzouki.shop.catalog;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "You may also like". Manager picks come first, then what other customers bought together with the item,
 * then the same subcategory. Only active products are suggested.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RelatedProducts {

    public static final int LIMIT = 4;
    private static final int MAX_MANUAL = 12;

    private final ProductRepository products;

    @PersistenceContext
    private EntityManager entityManager;

    private final Cache<Long, List<Long>> bought = Caffeine.newBuilder().maximumSize(5_000)
            .expireAfterWrite(Duration.ofMinutes(10)).build();

    /** Forgets what was cached about purchases, so the next look is fresh. */
    public void refreshPurchases() {
        bought.invalidateAll();
    }

    public List<Product> forProduct(Long productId) {
        Product product = products.findById(productId).orElseThrow(() -> new NotFoundException("Product", productId));
        Map<Long, Product> chosen = new LinkedHashMap<>();
        for (Product p : product.getRelated()) {
            if (p.isActive() && chosen.size() < LIMIT) {
                chosen.put(p.getId(), p);
            }
        }
        if (chosen.size() < LIMIT) {
            List<Long> ids = bought.get(productId, this::coPurchased).stream().filter(id -> !chosen.containsKey(id)).toList();
            if (!ids.isEmpty()) {
                Map<Long, Product> found = products.findByIdIn(ids).stream().collect(Collectors.toMap(Product::getId, Function.identity()));
                for (Long id : ids) {
                    Product p = found.get(id);
                    if (p != null && p.isActive() && chosen.size() < LIMIT) {
                        chosen.put(id, p);
                    }
                }
            }
        }
        if (chosen.size() < LIMIT) {
            for (Product p : products.findBySubcategoryIdAndActiveTrueAndIdNotOrderByCreatedAtDescIdDesc(
                    product.getSubcategory().getId(), productId, PageRequest.of(0, LIMIT + chosen.size()))) {
                if (chosen.size() < LIMIT) {
                    chosen.putIfAbsent(p.getId(), p);
                }
            }
        }
        List<Product> result = new ArrayList<>(chosen.values());
        ProductViews.warm(result);
        result.forEach(p -> p.getSubcategory().getTitle());
        return result;
    }

    /** Products in the same orders as this one, most often first. */
    private List<Long> coPurchased(Long productId) {
        return entityManager.createQuery("""
                select other.product.id, count(other) from OrderItem mine join OrderItem other on other.order = mine.order
                where mine.product.id = :id and other.product.id <> :id and other.order.status <> com.marzouki.shop.order.OrderStatus.CANCELLED
                group by other.product.id order by count(other) desc, other.product.id
                """, Object[].class).setParameter("id", productId).setMaxResults(LIMIT * 3)
                .getResultList().stream().map(row -> (Long) row[0]).toList();
    }

    public List<Product> picked(Long productId) {
        Product product = products.findById(productId).orElseThrow(() -> new NotFoundException("Product", productId));
        List<Product> list = new ArrayList<>(product.getRelated());
        list.forEach(p -> p.getSubcategory().getTitle());
        return list;
    }

    @Transactional
    public void add(Long productId, String sku) {
        Product product = products.findById(productId).orElseThrow(() -> new NotFoundException("Product", productId));
        String code = sku == null ? "" : sku.trim();
        Product other = products.findBySkuIgnoreCase(code)
                .orElseThrow(() -> new ConflictException("No product has the SKU '" + code + "'"));
        if (other.getId().equals(productId)) {
            throw new ConflictException("A product cannot be related to itself");
        }
        if (product.getRelated().stream().anyMatch(p -> p.getId().equals(other.getId()))) {
            throw new ConflictException(other.getName() + " is already on the list");
        }
        if (product.getRelated().size() >= MAX_MANUAL) {
            throw new ConflictException("At most " + MAX_MANUAL + " related products can be picked");
        }
        product.getRelated().add(other);
    }

    @Transactional
    public void remove(Long productId, Long relatedId) {
        Product product = products.findById(productId).orElseThrow(() -> new NotFoundException("Product", productId));
        product.getRelated().removeIf(p -> p.getId().equals(relatedId));
    }
}