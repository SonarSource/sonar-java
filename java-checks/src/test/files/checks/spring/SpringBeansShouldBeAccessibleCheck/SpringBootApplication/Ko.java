package foo.bar.Ko;

import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.RestController;

// Different from root package hierarchy

@Component
class Ko1 {} // Noncompliant [[sc=7;ec=10]] {{'Ko1' is not reachable by @ComponentsScan or @SpringBootApplication. Either move it to a package configured in @ComponentsScan or update your @ComponentsScan configuration.}}

@Service
class Ko2 {} // Noncompliant

@Controller
class Ko3 {} // Noncompliant

@RestController
class Ko4 {} // Noncompliant
