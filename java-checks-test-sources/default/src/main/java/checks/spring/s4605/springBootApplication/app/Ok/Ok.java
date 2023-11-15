package checks.spring.s4605.springBootApplication.app.Ok;

import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.RestController;

// Sub-package of the root package.

@Component
class Ok1 {} // Compliant

@Service
class Ok2 {} // Compliant

@Controller
class Ok3 {} // Compliant

@Repository
class Ok4 {} // Compliant

@RestController
class Ok5 {} // Compliant
