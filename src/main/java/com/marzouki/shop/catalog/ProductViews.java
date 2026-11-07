package com.marzouki.shop.catalog;

import java.util.Collection;

/**
 * Pages are rendered after the service transaction has ended (open-in-view is off), so anything a page
 * reads must be loaded first. Touching the variants of a whole list loads them in batches (see
 * hibernate.default_batch_fetch_size) rather than one query per product.
 */
public final class ProductViews {

    private ProductViews() {
    }

    /** Call inside a transaction. */
    public static void warm(Collection<Product> products) {
        for (Product product : products) {
            product.getVariants().size();
        }
    }
}