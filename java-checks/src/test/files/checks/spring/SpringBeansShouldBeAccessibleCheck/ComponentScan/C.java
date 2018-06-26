package src.test.files.checks.C;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@Component
class C {} // Noncompliant [[sc=7;ec=8]] {{'C' is not reachable by @ComponentsScan or @SpringBootApplication. Either move it to a package configured in @ComponentsScan or update your @ComponentsScan configuration.}}

@Service
class C2 { // Noncompliant
}

@Controller
class C3 { // Noncompliant
}

@RestController
class C4 { // Noncompliant
}

@Repository
class C5 { // Noncompliant
}

class C6 {
  @Service
  public class C6Inner { // Noncompliant
  }
  @Component
  public static class C6StaticInner { // Noncompliant
  }
}
