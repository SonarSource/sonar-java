package com.example.app;

import com.example.common.CacheProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// CASE: ambiguity between three @Bean-method-declared beans (not @Component), same module.
@Component
public class CacheConsumer {

    @Autowired
    private CacheProvider cacheProvider;
}
