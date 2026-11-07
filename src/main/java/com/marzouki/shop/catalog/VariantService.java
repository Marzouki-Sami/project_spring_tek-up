package com.marzouki.shop.catalog;

import com.marzouki.shop.catalog.dto.VariantForm;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import com.marzouki.shop.order.OrderItemRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds and maintains the variants of a product that has a product type. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VariantService {

    static final int MAX_COMBINATIONS = 200;

    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final ProductTypeRepository types;
    private final OrderItemRepository orderItems;

    public Product productWithVariants(Long productId) {
        Product product = products.findWithDetailsById(productId)
                .orElseThrow(() -> new NotFoundException("Product", productId));
        product.getVariants().forEach(v -> v.getOptionValues().forEach(o -> o.getOptionType().getName()));
        return product;
    }

    /** The option types (with all their values) the product's type allows. */
    public java.util.Collection<OptionType> allowedOptions(Product product) {
        if (product.getProductType() == null) {
            return List.of();
        }
        return types.findWithOptionsById(product.getProductType().getId()).orElseThrow().getOptionTypes();
    }

    public ProductVariant getForStock(Long variantId) {
        return variants.findWithDetailsById(variantId).orElseThrow(() -> new NotFoundException("Variant", variantId));
    }

    public ProductVariant get(Long productId, Long variantId) {
        ProductVariant variant = variants.findWithDetailsById(variantId)
                .orElseThrow(() -> new NotFoundException("Variant", variantId));
        if (!variant.getProduct().getId().equals(productId)) {
            throw new NotFoundException("Variant", variantId);
        }
        return variant;
    }

    /**
     * Creates every combination of the chosen option values that does not exist yet.
     *
     * @param chosen option type id mapped to the ids of the values to offer
     * @return how many variants were created
     */
    @Transactional
    public int generate(Long productId, Map<Long, Set<Long>> chosen) {
        Product product = productWithVariants(productId);
        if (!product.isOptioned()) {
            throw new ConflictException("This product has no type, so it has no options to combine");
        }
        List<List<OptionValue>> lists = new ArrayList<>();
        for (OptionType type : allowedOptions(product)) {
            Set<Long> ids = chosen.getOrDefault(type.getId(), Set.of());
            List<OptionValue> picked = type.getValues().stream().filter(v -> ids.contains(v.getId())).toList();
            if (picked.isEmpty()) {
                throw new ConflictException("Choose at least one " + type.getName().toLowerCase());
            }
            lists.add(picked);
        }
        long total = lists.stream().mapToLong(List::size).reduce(1, (a, b) -> a * b);
        if (total > MAX_COMBINATIONS) {
            throw new ConflictException("That would create " + total + " variants; the limit is " + MAX_COMBINATIONS);
        }

        Set<Set<Long>> existing = product.getVariants().stream()
                .map(v -> v.getOptionValues().stream().map(OptionValue::getId).collect(Collectors.toSet()))
                .collect(Collectors.toSet());
        int created = 0;
        for (List<OptionValue> combo : combinations(lists)) {
            Set<Long> key = combo.stream().map(OptionValue::getId).collect(Collectors.toSet());
            if (existing.contains(key)) {
                continue;
            }
            ProductVariant variant = product.addVariant(freeSku(product.getSku(), combo), 0);
            variant.getOptionValues().addAll(combo);
            created++;
        }
        return created;
    }

    @Transactional
    public ProductVariant update(Long productId, Long variantId, VariantForm form) {
        ProductVariant variant = get(productId, variantId);
        if (variants.existsBySkuIgnoreCaseAndIdNot(form.getSku(), variantId)) {
            throw new ConflictException("SKU '" + form.getSku() + "' is already used");
        }
        variant.setSku(form.getSku());
        variant.setPrice(form.getPrice());
        variant.setActive(form.isActive());
        return variant;
    }

    /** Variants that were ever ordered must stay (deactivate them instead); the rest can go. */
    @Transactional
    public void delete(Long productId, Long variantId) {
        ProductVariant variant = get(productId, variantId);
        Product product = variant.getProduct();
        if (!product.isOptioned()) {
            throw new ConflictException("A product without options always keeps its single variant");
        }
        if (orderItems.existsByVariantId(variantId)) {
            throw new ConflictException("This variant has been ordered, so it can only be deactivated");
        }
        product.getVariants().remove(variant);
    }

    private String freeSku(String base, List<OptionValue> combo) {
        String suffix = combo.stream().map(v -> v.getLabel().toUpperCase().replaceAll("[^A-Z0-9]+", ""))
                .filter(s -> !s.isEmpty()).collect(Collectors.joining("-"));
        String candidate = trim(base + (suffix.isEmpty() ? "" : "-" + suffix), 60);
        int counter = 2;
        while (variants.existsBySkuIgnoreCase(candidate)) {
            String tail = "-" + counter++;
            candidate = trim(base + (suffix.isEmpty() ? "" : "-" + suffix), 60 - tail.length()) + tail;
        }
        return candidate;
    }

    private static String trim(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static List<List<OptionValue>> combinations(List<List<OptionValue>> lists) {
        List<List<OptionValue>> result = new ArrayList<>();
        result.add(new ArrayList<>());
        for (List<OptionValue> options : lists) {
            List<List<OptionValue>> next = new ArrayList<>();
            for (List<OptionValue> prefix : result) {
                for (OptionValue option : options) {
                    List<OptionValue> combo = new ArrayList<>(prefix);
                    combo.add(option);
                    next.add(combo);
                }
            }
            result = next;
        }
        return result;
    }

    /** Used by the page to preselect the values that already have variants. */
    public Map<Long, Set<Long>> usedValues(Product product) {
        Map<Long, Set<Long>> used = new java.util.HashMap<>();
        for (ProductVariant variant : product.getVariants()) {
            for (OptionValue value : variant.getOptionValues()) {
                used.computeIfAbsent(value.getOptionType().getId(), k -> new LinkedHashSet<>()).add(value.getId());
            }
        }
        return used;
    }
}