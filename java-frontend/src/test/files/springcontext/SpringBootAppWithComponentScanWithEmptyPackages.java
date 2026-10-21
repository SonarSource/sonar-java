package checks.spring.context;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

// @ComponentScan with explicit empty packages doesn't scan any packages.
@SpringBootApplication
@ComponentScan(basePackages = {})
class SpringBootAppWithComponentScanWithEmptyPackages {}
