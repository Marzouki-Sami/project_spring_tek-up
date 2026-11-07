package com.marzouki.shop.catalog;

import com.marzouki.shop.catalog.dto.CategoryForm;
import com.marzouki.shop.catalog.dto.ProductForm;
import com.marzouki.shop.catalog.dto.SubcategoryForm;
import com.marzouki.shop.catalog.dto.SupplierForm;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/** Copies between entities and the forms the pages edit. Relations are set by the services. */
@Mapper
public interface CatalogMapper {

    CategoryForm toForm(Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subcategories", ignore = true)
    void apply(CategoryForm form, @MappingTarget Category category);

    @Mapping(target = "categoryId", source = "category.id")
    SubcategoryForm toForm(Subcategory subcategory);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    void apply(SubcategoryForm form, @MappingTarget Subcategory subcategory);

    SupplierForm toForm(Supplier supplier);

    @Mapping(target = "id", ignore = true)
    void apply(SupplierForm form, @MappingTarget Supplier supplier);

    @Mapping(target = "subcategoryId", source = "subcategory.id")
    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "productTypeId", source = "productType.id")
    @Mapping(target = "stock", ignore = true)
    ProductForm toForm(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "subcategory", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "productType", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "related", ignore = true)
    void apply(ProductForm form, @MappingTarget Product product);
}