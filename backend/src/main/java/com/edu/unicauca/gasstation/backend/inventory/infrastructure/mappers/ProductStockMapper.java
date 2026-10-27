package com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.LocationStockResponse;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.ProductStockResponse;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.SalesSyncResponse;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.StockMovementResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.LocationStock;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.ProductStock;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.SalesSyncResult;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovement;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductStockMapper {

    StockMovementResponse toMovementResponse(StockMovement movement);

    List<StockMovementResponse> toMovementResponses(List<StockMovement> movements);

    LocationStockResponse toLocationResponse(LocationStock stock);

    ProductStockResponse toProductStockResponse(ProductStock stock);

    List<ProductStockResponse> toProductStockResponses(List<ProductStock> stocks);

    SalesSyncResponse toSyncResponse(SalesSyncResult result);
}