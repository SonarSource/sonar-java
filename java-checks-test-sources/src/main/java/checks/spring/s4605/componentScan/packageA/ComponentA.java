package checks.spring.s4605.componentScan.packageA;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

@Component
class ComponentA { // Compliant
  ComponentA() {}

  @Service
  public class InnerComponent {} // Compliant

  @Component
  public static class StaticInnerComponent {} // Compliant
}

@Service
class ComponentB {} // Compliant

@Controller
class ComponentC {} // Compliant

@RestController
class ComponentD {} // Compliant

@Repository
class ComponentE { // Compliant
  class InnerClass {}
}
