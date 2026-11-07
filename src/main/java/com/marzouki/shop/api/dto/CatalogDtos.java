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

    public record VariantResponse(Long id, String sku, String label, BigDecimal price, int stock, boolean active) {
        public static VariantResponse of(com.marzouki.shop.catalog.ProductVariant v) {
            return new VariantResponse(v.getId(), v.getSku(), v.getLabel(), v.getEffectivePrice(), v.getStock(),
                    v.isActive());
        }
    }

    public record ImageResponse(String url, String thumbnailUrl, String alt, int width, int height) {
        public static ImageResponse of(com.marzouki.shop.media.ProductImage i) {
            return new ImageResponse(i.getUrl(), i.getThumbUrl(), i.getAltText(), i.getWidth(), i.getHeight());
        }
    }

    public record ProductResponse(Long id, String sku, String name, String description, BigDecimal price, int stock,
                                  boolean active, Long categoryId, String category, Long subcategoryId,
                                  String subcategory, String productType, List<VariantResponse> variants,
                                  List<ImageResponse> images) {
        public static ProductResponse of(Product p) {
            return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getPrice(),
                    p.getStock(), p.isActive(), p.getSubcategory().getCategory().getId(),
                    p.getSubcategory().getCategory().getTitle(), p.getSubcategory().getId(),
                    p.getSubcategory().getTitle(), p.getProductType() == null ? null : p.getProductType().getName(),
                    p.getVariants().stream().map(VariantResponse::of).toList(),
                    p.getImages().stream().map(ImageResponse::of).toList());
        }
    }
    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <S, T> PageResponse<T> of(Page<S> page, java.util.function.Function<S, T> mapper) {
            return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                    page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }
}