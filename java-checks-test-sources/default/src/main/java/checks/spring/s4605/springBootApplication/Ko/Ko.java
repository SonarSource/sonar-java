package checks.spring.s4605.springBootApplication.Ko;

import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.RestController;

// Different from root package hierarchy

@Component
class Ko1 {} // Noncompliant {{'Ko1' is not reachable by @ComponentScan or @SpringBootApplication. Either move it to a package configured in @ComponentScan or update your @ComponentScan configuration.}}
//    ^^^

@Service
class Ko2 {} // Noncompliant

@Controller
class Ko3 {} // Noncompliant

@RestController
class Ko4 {} // Noncompliant
