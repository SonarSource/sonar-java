package checks.spring.context;

import org.springframework.boot.autoconfigure.SpringBootApplication;

// No scanBasePackages / scanBasePackageClasses — the class's own package should be collected.
@SpringBootApplication
class SpringBootAppNoScanAttributes {}
