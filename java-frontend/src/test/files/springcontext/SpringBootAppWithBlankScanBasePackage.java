package checks.spring.context;

import org.springframework.boot.autoconfigure.SpringBootApplication;

// Empty string in scanBasePackages should be silently ignored.
@SpringBootApplication(scanBasePackages = {"com.example.service", ""})
class SpringBootAppWithBlankScanBasePackage {}
