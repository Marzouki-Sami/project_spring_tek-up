package com.marzouki.shop.api.dto;

import com.marzouki.shop.catalog.Category;
import com.marzouki.shop.catalog.Product;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;

/** Response bodies of the catalog endpoints. */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record SubcategoryResponse(Long id, String title) {
    }

    public record CategoryResponse(Long id, String title, String description, List<SubcategoryResponse> subcategories) {
        public static CategoryResponse of(Category category) {
            return new CategoryResponse(category.getId(), category.getTitle(), category.getDescription(),
                    category.getSubcategories().stream()
                            .map(s -> new SubcategoryResponse(s.getId(), s.getTitle())).toList());
        }
    }

    public record ProductResponse(Long id, String sku, String name, String description, BigDecimal price, int stock,
                                  boolean active, Long categoryId, String category, Long subcategoryId,
                                  String subcategory) {
        public static ProductResponse of(Product p) {
            return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getPrice(),
                    p.getStock(), p.isActive(), p.getSubcategory().getCategory().getId(),
                    p.getSubcategory().getCategory().getTitle(), p.getSubcategory().getId(),
                    p.getSubcategory().getTitle());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <S, T> PageResponse<T> of(Page<S> page, java.util.function.Function<S, T> mapper) {
            return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                    page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }
}