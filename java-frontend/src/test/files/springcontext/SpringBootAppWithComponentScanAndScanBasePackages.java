package checks.spring.context;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

// The directly declared @ComponentScan shadows the one contributed by @SpringBootApplication,
// so scanBasePackages is ignored.
@SpringBootApplication(scanBasePackages = {"com.example.ignored"})
@ComponentScan(basePackages = {"com.example.service", "com.example.web"})
class SpringBootAppWithComponentScanAndScanBasePackages {}
