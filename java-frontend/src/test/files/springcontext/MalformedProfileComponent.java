package checks.spring.context;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("a & b | c")
@Component
class MalformedProfileComponent {
}
