package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.model.Provider;
import com.example.demo.service.ProductService;
import com.example.demo.service.ProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/product")
public class ProductController {

    @Autowired
    ProductService productService;

    @Autowired
    ProviderService providerService;

    @GetMapping("/all")
    public String getAllProducts(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        return "product/all_products";
    }

    @GetMapping("/add")
    public String addProduct(Model model) {
        Product product = new Product();
        List<Provider> providerList = providerService.getAllProviders();
        model.addAttribute("ProductForm", product);
        model.addAttribute("providers", providerList);
        return "/product/add_product";
    }

    @PostMapping("/save")
    public String saveProduct(@ModelAttribute("ProductForm") Product product) {
        productService.addProduct(product);
        return "redirect:/product/all";
    }

    @GetMapping("/edit/{id}")
    public String editProduct(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        model.addAttribute("ProductForm", product);
        return "product/edit_product";
    }

    @PostMapping("/update")
    public String updateProduct(@ModelAttribute("ProductForm") Product product) {
        productService.updateProduct(product);
        return "redirect:/product/all";
    }

    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteProductById(id);
        return "redirect:/product/all";
    }
}
