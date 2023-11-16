package checks.spring.s4605.componentScan.packageY;

import org.springframework.stereotype.Component;
// need to be on line 6 to avoid clashes in case of failure
@Component
class ComponentY {} // Compliant
