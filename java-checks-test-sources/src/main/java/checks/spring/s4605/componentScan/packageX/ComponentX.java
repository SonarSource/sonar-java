package checks.spring.s4605.componentScan.packageX;
import org.springframework.stereotype.Component;
// need to be on line 5 to avoid clashes in case of failure
@Component
class ComponentX {} // Compliant
