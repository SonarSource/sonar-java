package com.example.modulea;

import com.example.common.InventoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WarehouseInventoryConfig {

    @Bean
    public InventoryService warehouseInventoryService() {
        return sku -> 100;
    }
}
