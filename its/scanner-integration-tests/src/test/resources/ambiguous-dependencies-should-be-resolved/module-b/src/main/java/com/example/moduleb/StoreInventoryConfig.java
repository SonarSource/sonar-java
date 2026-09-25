package com.example.moduleb;

import com.example.common.InventoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StoreInventoryConfig {

    @Bean
    public InventoryService storeInventoryService() {
        return sku -> 10;
    }
}
