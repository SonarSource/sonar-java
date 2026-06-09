package checks.spring.context;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.example.service", "com.example.web"})
class SpringBootAppWithScanBasePackages {}
