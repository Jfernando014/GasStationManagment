package com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.ProductRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.ProductResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    Product toDomain(ProductRequest request);

    @Mapping(target = "marginPercent",
            expression = "java(product.calculateMarginPercent().orElse(null))")
    ProductResponse toResponse(Product product);

    List<ProductResponse> toResponseList(List<Product> products);
}
