package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.Product;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.ProductEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductPersistenceMapper {
    public Product toDomain(ProductEntity entity) {
        Product product = new Product();
        product.setId(entity.getId());
        product.setCode(entity.getCode());
        product.setName(entity.getName());
        product.setCategory(entity.getCategory());
        product.setPrice(entity.getPrice());
        product.setCost(entity.getCost());
        product.setActive(entity.isActive());
        return product;
    }
    public ProductEntity toEntity(Product product) {
        ProductEntity entity = new ProductEntity();
        entity.setId(product.getId());
        entity.setCode(product.getCode());
        entity.setName(product.getName());
        entity.setCategory(product.getCategory());
        entity.setPrice(product.getPrice());
        entity.setCost(product.getCost());
        entity.setActive(product.getActive());
        return entity;
    }
}
