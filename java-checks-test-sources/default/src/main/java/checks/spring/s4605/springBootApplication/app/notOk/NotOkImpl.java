package checks.spring.s4605.springBootApplication.app.notOk;

import org.springframework.stereotype.Component;

interface NotOk {
}

@Component
public class NotOkImpl implements NotOk { // Compliant
}
