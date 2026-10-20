package checks.spring.context;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

// The @ComponentScan on the first class must not suppress the own-package fallback of the second one.
@ComponentScan(basePackages = {"com.example.service"})
class ScanConfigDeclaredFirst {}

@SpringBootApplication
class SpringBootAppDeclaredSecond {}
