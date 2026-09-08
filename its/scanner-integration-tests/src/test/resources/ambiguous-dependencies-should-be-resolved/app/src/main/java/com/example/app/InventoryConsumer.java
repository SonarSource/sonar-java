package com.example.app;

import com.example.common.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// CASE: ambiguity between two @Bean-method-declared beans, each in its own @Configuration class, cross-module.
@Component
public class InventoryConsumer {

    @Autowired
    private InventoryService inventoryService;
}
