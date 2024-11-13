package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public Product getProductById(Long id) {
        if (productRepository.findById(id).isPresent())
            return productRepository.findById(id).get();
        else
            throw new RuntimeException("Product not found");
    }

    public void deleteProductById(Long id) {
        productRepository.deleteById(id);
    }

    public void updateProduct(Product product) {
        Product existingProduct = getProductById(product.getId_product());

        // name, price description
        if (product.getName() != null) {
            existingProduct.setName(product.getName());
        }

        if (product.getPrice() != null) {
            existingProduct.setPrice(product.getPrice());
        }

        if (product.getDescription() != null) {
            existingProduct.setDescription(product.getDescription());
        }
        //subCategory, provider
        if (product.getSubcategory() != null) {
            existingProduct.setSubcategory(product.getSubcategory());
        }

        if (product.getProvider() != null) {
            existingProduct.setProvider(product.getProvider());
        }
        productRepository.saveAndFlush(existingProduct);
    }

    public boolean productExist(Long id) {
        return productRepository.existsById(id);
    }

    public void addProduct(Product product) {
        productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}
