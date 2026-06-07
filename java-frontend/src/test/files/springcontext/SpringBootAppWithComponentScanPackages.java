package checks.spring.context;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

// @ComponentScan with explicit packages overrides @SpringBootApplication's default (own package).
@SpringBootApplication
@ComponentScan(basePackages = {"com.example.service", "com.example.web"})
class SpringBootAppWithComponentScanPackages {}
