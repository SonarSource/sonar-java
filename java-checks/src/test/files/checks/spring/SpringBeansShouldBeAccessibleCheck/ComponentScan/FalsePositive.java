package falsey.positive;

import org.springframework.stereotype.Component;

// must be on line 7 to avoid clash with C.java
@Component
class FP {} // Noncompliant (False Positive because class constants are not supported)
