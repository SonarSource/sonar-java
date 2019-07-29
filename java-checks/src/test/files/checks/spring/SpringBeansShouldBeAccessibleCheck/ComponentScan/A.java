package src.test.files.checks.spring.A;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

@Component
class A1 { // Compliant
  A1() { }

  @Service
  public class A2Inner { // Compliant
  }
  @Component
  public static class A2StaticInner { // Compliant
  }
}

@Service
class A2 { // Compliant
}

@Controller
class A3 { // Compliant
}

@RestController
class A4 { // Compliant
}

@Repository
class A5 { // Compliant
  class A5Inner {
  }
}
