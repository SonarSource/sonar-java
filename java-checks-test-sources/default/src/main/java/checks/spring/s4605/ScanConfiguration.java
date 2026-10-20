package checks.spring.s4605;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

// Declares the only scanned package of this test package: checks.spring.s4605.scanned. Paired with
// ScannedComponent (covered) and UnscannedComponent (not covered).
@Configuration
@ComponentScan("checks.spring.s4605.scanned")
public class ScanConfiguration {
}
