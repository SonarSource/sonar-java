package checks.spring.context;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

// @ComponentScan without explicit packages shadows @SpringBootApplication's ones.
@SpringBootApplication(scanBasePackages = "x")
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.excluded"))
class SpringBootAppWithComponentScanWithoutPackages {}
