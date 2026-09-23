package checks.spring.context;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("dev & !test")
@Component
class ProfileExpressionComponent {
}
