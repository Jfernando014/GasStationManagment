package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Product;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.ProductRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.DuplicateProductCodeException;
import com.edu.unicauca.gasstation.backend.inventory.exception.ProductNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product create(Product product) {
        product.setCode(product.getCode().trim());
        product.setName(product.getName().trim());
        if (productRepository.existsByCode(product.getCode())) {
            throw new DuplicateProductCodeException(product.getCode());
        }
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, Product product) {
        Product current = findById(id);
        String code = product.getCode().trim();
        if (productRepository.existsByCodeAndIdNot(code, id)) {
            throw new DuplicateProductCodeException(product.getCode());

        }
        current.setCode(code);
        current.setName(product.getName().trim());
        current.setCategory(product.getCategory());
        current.setPrice(product.getPrice());
        current.setCost(product.getCost());
        current.setActive(product.getActive());
        return productRepository.save(current);
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Product> findAll(ProductCategory category, Boolean active) {
        return productRepository.findAll(category, active);
    }
}
