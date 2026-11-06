package com.marzouki.shop.catalog;

import com.marzouki.shop.catalog.dto.SupplierForm;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierRepository suppliers;
    private final ProductRepository products;
    private final CatalogMapper mapper;

    public List<Supplier> findAll() {
        return suppliers.findAll(Sort.by("company"));
    }

    public Supplier get(Long id) {
        return suppliers.findById(id).orElseThrow(() -> new NotFoundException("Supplier", id));
    }

    public SupplierForm formFor(Long id) {
        return mapper.toForm(get(id));
    }

    @Transactional
    public Supplier create(SupplierForm form) {
        Supplier supplier = new Supplier();
        mapper.apply(form, supplier);
        return suppliers.save(supplier);
    }

    @Transactional
    public Supplier update(Long id, SupplierForm form) {
        Supplier supplier = get(id);
        mapper.apply(form, supplier);
        return supplier;
    }

    @Transactional
    public void delete(Long id) {
        Supplier supplier = get(id);
        if (products.existsBySupplierId(id)) {
            throw new ConflictException("'" + supplier.getCompany()
                    + "' still supplies products. Mark it inactive instead of deleting it");
        }
        suppliers.delete(supplier);
    }
}