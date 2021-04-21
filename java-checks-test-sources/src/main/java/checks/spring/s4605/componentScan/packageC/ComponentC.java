package checks.spring.s4605.componentScan.packageC;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@Component
class ComponentC {} // Noncompliant [[sc=7;ec=17]] {{'ComponentC' is not reachable by @ComponentsScan or @SpringBootApplication. Either move it to a package configured in @ComponentsScan or update your @ComponentsScan configuration.}}

@Service
class ComponentD {} // Noncompliant

@Controller
class ComponentE {} // Noncompliant

@RestController
class ComponentF {} // Noncompliant

@Repository
class ComponentG {} // Noncompliant

class ComponentH {
  @Service
  public class InnerComponent {} // Noncompliant

  @Component
  public static class StaticInnerComponent {} // Noncompliant
}
