package checks.spring.s4605.mixed.app1.visible;

import org.springframework.stereotype.Component;

interface VisibleServiceI {
}

@Component
public class VisibleService implements VisibleServiceI { // Compliant
}
